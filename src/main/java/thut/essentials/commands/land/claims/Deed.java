package thut.essentials.commands.land.claims;

import java.util.Set;

import com.google.common.collect.Sets;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.land.LandSaveHandler;
import thut.essentials.util.Coordinate;

public class Deed
{
    @SubscribeEvent(receiveCanceled = true)
    public static void interact(final PlayerInteractEvent.RightClickItem evt)
    {
        if (!(evt.getPlayer() instanceof ServerPlayerEntity)) return;
        final ItemStack stack = evt.getItemStack();
        if (!stack.hasTag() || !stack.getTag().getBoolean("isDeed")) return;

        final ServerPlayerEntity player = (ServerPlayerEntity) evt.getPlayer();
        if (!PermissionAPI.hasPermission(player, Deed.CANREDEEMDEEDS))
        {
            player.sendMessage(Essentials.config.getMessage("thutessentials.claim.notallowed.teamperms"));
            return;
        }
        final LandTeam team = LandManager.getTeam(player);
        if (!team.hasRankPerm(player.getUniqueID(), LandTeam.CLAIMPERM))
        {
            player.sendMessage(Essentials.config.getMessage("thutessentials.claim.notallowed.teamperms"));
            return;
        }

        final int num = stack.getTag().getInt("num");
        int n = 0;
        int x = 0, z = 0;
        for (int i = 0; i < num; i++)
        {
            final CompoundNBT tag = stack.getTag().getCompound("" + i);
            final Coordinate c = new Coordinate(tag);
            x = c.x;
            z = c.z;
            // Unclaim from deed team first.
            LandManager.getInstance().removeTeamLand(Deed.DEEDTEAM, c);
            // Then claim for the new owner.
            final int re = Claim.claim(c, player, team, false, PermissionAPI.hasPermission(player, Deed.BYPASSLIMIT));
            if (re == 0)
            {
                n++;
                stack.getTag().remove("" + i);
            }
        }
        stack.getTag().putInt("num", num - n);
        player.sendMessage(Essentials.config.getMessage("thutessentials.deed.claimed", n, team.teamName));
        if (n == num) stack.grow(-1);
        else stack.setDisplayName(Essentials.config.getMessage("thutessentials.deed.for", num - n, x << 4, z << 4));
    }

    private static final String BYPASSLIMIT    = "thutessentials.land.deed.nolimit";
    private static final String CANREDEEMDEEDS = "thutessentials.land.deed";

    private static final String DEEDTEAM = "__deeds__";

    private static boolean registered = false;

    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        final String name = "reclaim_deed";
        if (Essentials.config.commandBlacklist.contains(name)) return;
        String perm;
        PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /" + name);
        PermissionAPI.registerNode(Deed.BYPASSLIMIT, DefaultPermissionLevel.OP,
                "Permission to bypass the land per player limit for a team using deeds.");
        PermissionAPI.registerNode(Deed.CANREDEEMDEEDS, DefaultPermissionLevel.ALL,
                "Permission to use deeds to claim land.");

        // Register to bus
        if (!Deed.registered) MinecraftForge.EVENT_BUS.register(Deed.class);
        Deed.registered = true;

        // Setup with name and permission
        LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs,
                perm));

        // Entire chunk
        command = command.executes(ctx -> Deed.execute(ctx.getSource(), true, true, false));
        commandDispatcher.register(command);

        command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs, perm));
        command = command.then(Commands.literal("up").executes(ctx -> Deed.execute(ctx.getSource(), true, false,
                false)));
        commandDispatcher.register(command);

        command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs, perm));
        command = command.then(Commands.literal("down").executes(ctx -> Deed.execute(ctx.getSource(), false, true,
                false)));
        commandDispatcher.register(command);

        command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs, perm));
        command = command.then(Commands.literal("here").executes(ctx -> Deed.execute(ctx.getSource(), false, false,
                true)));
        commandDispatcher.register(command);
    }

    private static int execute(final CommandSource source, final boolean up, final boolean down, final boolean here)
            throws CommandSyntaxException
    {
        final PlayerEntity player = source.asPlayer();
        final LandTeam team = LandManager.getTeam(player);
        final boolean canUnclaimAnything = PermissionAPI.hasPermission(player, Unclaim.GLOBALPERM);

        if (!canUnclaimAnything && !team.hasRankPerm(player.getUniqueID(), LandTeam.UNCLAIMPERM))
        {
            player.sendMessage(Essentials.config.getMessage("thutessentials.unclaim.notallowed.teamperms"));
            return 1;
        }

        final int x = player.getPosition().getX() >> 4;
        final int y = player.getPosition().getY() >> 4;
        final int z = player.getPosition().getZ() >> 4;

        final Set<Coordinate> deeds = Sets.newHashSet();

        final int dim = player.dimension.getId();
        boolean done = false;
        if (here)
        {
            final int ret = Deed.unclaim(x, y, z, player, team, true, canUnclaimAnything);
            if (ret == 0)
            {
                final Coordinate chunk = new Coordinate(x, y, z, dim);
                done = true;
                deeds.add(chunk);
            }
            else return ret;
        }
        else
        {
            final int min = down ? 0 : y;
            final int max = up ? 16 : y;

            int claimnum = 0;
            int owned_other = 0;
            for (int i = min; i < max; i++)
            {
                final int check = Deed.unclaim(x, i, z, player, team, false, canUnclaimAnything);
                if (check == 0)
                {
                    final Coordinate chunk = new Coordinate(x, i, z, dim);
                    deeds.add(chunk);
                    done = true;
                    claimnum++;
                }
                else if (check == 3) owned_other++;
            }
            if (owned_other > 0) player.sendMessage(Essentials.config.getMessage(
                    "thutessentials.unclaim.notallowed.notowner", owned_other));
            if (done) player.sendMessage(Essentials.config.getMessage("thutessentials.unclaim.done.num", claimnum,
                    team.teamName));
            else player.sendMessage(Essentials.config.getMessage("thutessentials.unclaim.done.failed", claimnum,
                    team.teamName));
        }

        if (!deeds.isEmpty())
        {
            final ItemStack deed = new ItemStack(Items.PAPER);
            deed.setTag(new CompoundNBT());
            deed.getTag().putInt("num", deeds.size());
            deed.getTag().putBoolean("isDeed", true);
            int i = 0;
            for (final Coordinate c : deeds)
                deed.getTag().put("" + i++, c.serializeNBT());
            deed.setDisplayName(Essentials.config.getMessage("thutessentials.deed.for", deeds.size(), x << 4, z << 4));
            if (!player.addItemStackToInventory(deed)) player.dropItem(deed, false);
        }
        LandSaveHandler.saveTeam(team.teamName);
        LandSaveHandler.saveTeam(Deed.DEEDTEAM);
        return done ? 0 : 1;
    }

    private static int unclaim(final Coordinate chunk, final PlayerEntity player, final LandTeam team,
            final boolean messages, final boolean canUnclaimAnything)
    {
        final LandTeam owner = LandManager.getInstance().getLandOwner(chunk);
        if (owner == null)
        {
            if (messages) player.sendMessage(Essentials.config.getMessage("thutessentials.unclaim.notallowed.noowner"));
            return 2;
        }
        else if (owner != team && !canUnclaimAnything)
        {
            if (messages) player.sendMessage(Essentials.config.getMessage("thutessentials.unclaim.notallowed.notowner",
                    owner.teamName));
            return 3;
        }
        LandManager.getInstance().removeTeamLand(team.teamName, chunk);
        // ensure the deed team exist, and that it is set to reserved.
        Deed.initDeedTeam();
        // Transfers the claim over to the "deed team"
        LandManager.getInstance().addTeamLand(Deed.DEEDTEAM, chunk, true);
        if (messages) player.sendMessage(Essentials.config.getMessage("thutessentials.unclaim.done", team.teamName));
        return 0;
    }

    private static void initDeedTeam()
    {
        final LandTeam team = LandManager.getInstance().getTeam(Deed.DEEDTEAM, true);
        team.reserved = true;
    }

    private static int unclaim(final int x, final int y, final int z, final PlayerEntity player, final LandTeam team,
            final boolean messages, final boolean canUnclaimAnything)
    {
        // TODO better bounds check to support say cubic chunks.
        if (y < 0 || y > 15) return 1;
        final int dim = player.dimension.getId();
        final Coordinate chunk = new Coordinate(x, y, z, dim);
        return Deed.unclaim(chunk, player, team, messages, canUnclaimAnything);
    }
}
