package thut.essentials.commands.land.claims;

import java.util.List;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.events.UnclaimLandEvent;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.land.LandSaveHandler;
import thut.essentials.util.Coordinate;

public class Unclaim
{
    private static final String GLOBALPERM = "thutessentials.land.unclaim.any";

    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        // TODO configurable this.
        final String name = "unclaim";
        if (Essentials.config.commandBlacklist.contains(name)) return;
        String perm;
        PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /" + name);
        PermissionAPI.registerNode(Unclaim.GLOBALPERM, DefaultPermissionLevel.OP,
                "Permission to unclaim land regardless of owner.");

        // Setup with name and permission
        LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs,
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

        command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs, perm));
        command = command.then(Commands.literal("everything").executes(ctx -> Unclaim.execute(ctx.getSource(), false,
                false, false, true)));
        commandDispatcher.register(command);
    }

    private static int execute(final CommandSource source, final boolean up, final boolean down, final boolean here,
            final boolean all) throws CommandSyntaxException
    {
        final PlayerEntity player = source.asPlayer();
        final LandTeam team = LandManager.getTeam(player);
        if (!team.hasRankPerm(player.getUniqueID(), LandTeam.UNCLAIMPERM))
        {
            player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.unclaim.notallowed.teamperms",
                    TextFormatting.RED));
            return 1;
        }

        if (all)
        {
            final List<Coordinate> allLand = Lists.newArrayList(team.land.land);
            for (final Coordinate c : allLand)
                Unclaim.unclaim(c, player, team, false);
            player.sendMessage(new TranslationTextComponent("thutessentials.unclaim.done.num", allLand.size(),
                    team.teamName));
            return 0;
        }

        final int x = MathHelper.floor(player.getPosition().getX() >> 4);
        final int y = MathHelper.floor(player.getPosition().getY() >> 4);
        final int z = MathHelper.floor(player.getPosition().getZ() >> 4);

        if (here) return Unclaim.unclaim(x, y, z, player, team, true);

        final int min = down ? 0 : y;
        final int max = up ? 16 : y;

        boolean claimed = false;
        int claimnum = 0;
        int owned_other = 0;
        for (int i = min; i < max; i++)
        {
            final int check = Unclaim.unclaim(x, i, z, player, team, false);
            if (check == 0)
            {
                claimed = true;
                claimnum++;
            }
            else if (check == 3) owned_other++;
        }
        if (owned_other > 0) player.sendMessage(new TranslationTextComponent(
                "thutessentials.unclaim.notallowed.notowner", owned_other));
        if (claimed) player.sendMessage(new TranslationTextComponent("thutessentials.unclaim.done.num", claimnum,
                team.teamName));
        else player.sendMessage(new TranslationTextComponent("thutessentials.unclaim.done.failed", claimnum,
                team.teamName));

        LandSaveHandler.saveTeam(team.teamName);
        return claimed ? 0 : 1;
    }

    private static int unclaim(final Coordinate chunk, final PlayerEntity player, final LandTeam team,
            final boolean messages)
    {
        final LandTeam owner = LandManager.getInstance().getLandOwner(chunk);
        if (owner == null)
        {
            if (messages) player.sendMessage(new TranslationTextComponent("thutessentials.unclaim.notallowed.noowner"));
            return 2;
        }
        else if (owner != team && !PermissionAPI.hasPermission(player, Unclaim.GLOBALPERM))
        {
            if (messages) player.sendMessage(new TranslationTextComponent("thutessentials.unclaim.notallowed.notowner",
                    owner.teamName));
            return 3;
        }
        final UnclaimLandEvent event = new UnclaimLandEvent(new BlockPos(chunk.x, chunk.y, chunk.z), chunk.dim, player,
                team.teamName);
        MinecraftForge.EVENT_BUS.post(event);
        LandManager.getInstance().removeTeamLand(team.teamName, chunk);
        if (messages) player.sendMessage(new TranslationTextComponent("thutessentials.unclaim.done", team.teamName));
        return 0;
    }

    private static int unclaim(final int x, final int y, final int z, final PlayerEntity player, final LandTeam team,
            final boolean messages)
    {
        // TODO better bounds check to support say cubic chunks.
        if (y < 0 || y > 15) return 1;
        final int dim = player.dimension.getId();
        final Coordinate chunk = new Coordinate(x, y, z, dim);
        return Unclaim.unclaim(chunk, player, team, messages);
    }
}
