package thut.essentials.commands.land.claims;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceKey;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.events.UnclaimLandEvent;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.KGobalPos;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.land.LandManager.TeamLand;
import thut.essentials.land.LandSaveHandler;

public class Unclaim
{
    public static final String GLOBALPERM = "thutessentials.land.unclaim.any";

    public static void register(final CommandDispatcher<CommandSourceStack> commandDispatcher)
    {
        final String name = "unclaim";
        if (Essentials.config.commandBlacklist.contains(name)) return;
        String perm;
        PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /" + name);
        PermissionAPI.registerNode(Unclaim.GLOBALPERM, DefaultPermissionLevel.OP,
                "Permission to unclaim land regardless of owner.");

        // Setup with name and permission
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs,
                perm));

        // Entire chunk
        command = command.executes(ctx -> Unclaim.execute(ctx.getSource(), true, true, false, false));
        commandDispatcher.register(command);

        command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs, perm));
        command = command.then(Commands.literal("up").executes(ctx -> Unclaim.execute(ctx.getSource(), true, false,
                false, false)));
        commandDispatcher.register(command);

        command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs, perm));
        command = command.then(Commands.literal("down").executes(ctx -> Unclaim.execute(ctx.getSource(), false, true,
                false, false)));
        commandDispatcher.register(command);

        command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs, perm));
        command = command.then(Commands.literal("here").executes(ctx -> Unclaim.execute(ctx.getSource(), false, false,
                true, false)));
        commandDispatcher.register(command);

        command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs, perm));
        command = command.then(Commands.literal("everything").executes(ctx -> Unclaim.execute(ctx.getSource(), false,
                false, false, true)));
        commandDispatcher.register(command);
    }

    private static int execute(final CommandSourceStack source, final boolean up, final boolean down, final boolean here,
            final boolean all) throws CommandSyntaxException
    {
        final Player player = source.getPlayerOrException();
        final LandTeam team = LandManager.getTeam(player);
        final boolean canUnclaimAnything = PermissionAPI.hasPermission(player, Unclaim.GLOBALPERM);

        if (!canUnclaimAnything && !team.hasRankPerm(player.getUUID(), LandTeam.UNCLAIMPERM))
        {
            player.sendMessage(Essentials.config.getMessage("thutessentials.unclaim.notallowed.teamperms"),
                    Util.NIL_UUID);
            return 1;
        }

        if (all)
        {
            final int num = team.land.countLand();
            LandManager.getInstance()._team_land.remove(team.land.uuid);
            team.land = new TeamLand();
            LandManager.getInstance()._team_land.put(team.land.uuid, team);
            LandSaveHandler.saveTeam(team.teamName);
            player.sendMessage(Essentials.config.getMessage("thutessentials.unclaim.done.num", num, team.teamName),
                    Util.NIL_UUID);
            return 0;
        }
        player.getServer().execute(() ->
        {
            final int x = Mth.floor(player.blockPosition().getX() >> 4);
            final int y = Mth.floor(player.blockPosition().getY() >> 4);
            final int z = Mth.floor(player.blockPosition().getZ() >> 4);

            final AtomicInteger worked = new AtomicInteger();
            final AtomicInteger other = new AtomicInteger();
            final AtomicBoolean ready = new AtomicBoolean();

            if (here)
            {
                Unclaim.unclaim(x, y, z, player, team, true, canUnclaimAnything, worked, other, ready);
                LandSaveHandler.saveTeam(team.teamName);
                return;
            }
            final int min = down ? 0 : y;
            final int max = up ? 16 : y;
            boolean done = false;
            int claimnum = 0;
            int owned_other = 0;
            for (int i = min; i < max; i++)
                Unclaim.unclaim(x, i, z, player, team, false, canUnclaimAnything, worked, other, ready);

            claimnum = worked.get();
            owned_other = other.get();
            done = claimnum != 0;
            if (owned_other > 0) player.sendMessage(Essentials.config.getMessage(
                    "thutessentials.unclaim.notallowed.notowner", owned_other), Util.NIL_UUID);
            if (done) player.sendMessage(Essentials.config.getMessage("thutessentials.unclaim.done.num", claimnum,
                    team.teamName), Util.NIL_UUID);
            else player.sendMessage(Essentials.config.getMessage("thutessentials.unclaim.done.failed", claimnum,
                    team.teamName), Util.NIL_UUID);
            LandSaveHandler.saveTeam(team.teamName);
        });
        return 0;
    }

    private static int unclaim(final KGobalPos chunk, final Player player, final LandTeam team,
            final boolean messages, final boolean canUnclaimAnything, final AtomicInteger worked,
            final AtomicInteger other, final AtomicBoolean ready)
    {

        final LandTeam owner = LandManager.getInstance().getLandOwner(player.getCommandSenderWorld(), chunk.getPos(), true);
        if (LandManager.isWild(owner))
        {
            if (messages) player.sendMessage(Essentials.config.getMessage("thutessentials.unclaim.notallowed.noowner"),
                    Util.NIL_UUID);
            ready.getAndSet(true);
            return 2;
        }
        else if (owner != team && !canUnclaimAnything)
        {
            if (messages) player.sendMessage(Essentials.config.getMessage("thutessentials.unclaim.notallowed.notowner",
                    owner.teamName), Util.NIL_UUID);
            other.getAndIncrement();
            ready.getAndSet(true);
            return 3;
        }
        final UnclaimLandEvent event = new UnclaimLandEvent(chunk, player, team.teamName);
        MinecraftForge.EVENT_BUS.post(event);
        LandManager.getInstance().unclaimLand(team.teamName, player.getCommandSenderWorld(), chunk.getPos(), true);
        worked.getAndIncrement();
        ready.getAndSet(true);
        if (messages) player.sendMessage(Essentials.config.getMessage("thutessentials.unclaim.done", team.teamName),
                Util.NIL_UUID);

        return 0;
    }

    private static int unclaim(final int x, final int y, final int z, final Player player, final LandTeam team,
            final boolean messages, final boolean canUnclaimAnything, final AtomicInteger worked,
            final AtomicInteger other, final AtomicBoolean ready)
    {
        final ResourceKey<Level> dim = player.getCommandSenderWorld().dimension();
        final BlockPos b = new BlockPos(x, y, z);
        final KGobalPos chunk = KGobalPos.getPosition(dim, b);
        return Unclaim.unclaim(chunk, player, team, messages, canUnclaimAnything, worked, other, ready);
    }
}
