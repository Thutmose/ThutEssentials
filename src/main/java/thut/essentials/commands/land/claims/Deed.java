package thut.essentials.commands.land.claims;

import java.util.Set;

import com.google.common.collect.Sets;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.KGobalPos;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.land.LandSaveHandler;
import thut.essentials.util.CoordinateUtls;

public class Deed
{
    @SubscribeEvent(receiveCanceled = true)
    public static void interact(final PlayerInteractEvent.RightClickItem evt)
    {
        if (!(evt.getPlayer() instanceof ServerPlayer)) return;
        final ItemStack stack = evt.getItemStack();
        if (!stack.hasTag() || !stack.getTag().getBoolean("isDeed")) return;

        final ServerPlayer player = (ServerPlayer) evt.getPlayer();
        if (!PermissionAPI.hasPermission(player, Deed.CANREDEEMDEEDS))
        {
            player.sendMessage(Essentials.config.getMessage("thutessentials.claim.notallowed.teamperms"),
                    Util.NIL_UUID);
            return;
        }
        final LandTeam team = LandManager.getTeam(player);
        if (!team.hasRankPerm(player.getUUID(), LandTeam.CLAIMPERM))
        {
            player.sendMessage(Essentials.config.getMessage("thutessentials.claim.notallowed.teamperms"),
                    Util.NIL_UUID);
            return;
        }

        final int num = stack.getTag().getInt("num");
        int n = 0;
        int x = 0, z = 0;
        final Level world = player.getCommandSenderWorld();
        for (int i = 0; i < num; i++)
        {
            final CompoundTag tag = stack.getTag().getCompound("" + i);
            final KGobalPos c = CoordinateUtls.fromNBT(tag);
            if (c == null) continue;
            if (c.getDimension() != world.dimension())
            {
                player.sendMessage(Essentials.config.getMessage("thutessentials.deed.notallowed.wrongdim"),
                        Util.NIL_UUID);
                return;
            }
            x = c.getPos().getX();
            z = c.getPos().getZ();
            // Unclaim from deed team first.
            LandManager.getInstance().unclaimLand(Deed.DEEDTEAM, world, c.getPos(), true);
            // Then claim for the new owner.
            final int re = Claim.claim(world, c.getPos(), player, team, false, PermissionAPI.hasPermission(player,
                    Deed.BYPASSLIMIT));
            if (re == 0)
            {
                n++;
                stack.getTag().remove("" + i);
            }
        }
        stack.getTag().putInt("num", num - n);
        player.sendMessage(Essentials.config.getMessage("thutessentials.deed.claimed", n, team.teamName),
                Util.NIL_UUID);
        if (n == num) stack.grow(-1);
        else stack.setHoverName(Essentials.config.getMessage("thutessentials.deed.for", num - n, x << 4, z << 4));
    }

    private static final String BYPASSLIMIT    = "thutessentials.land.deed.nolimit";
    private static final String CANREDEEMDEEDS = "thutessentials.land.deed";

    private static final String DEEDTEAM = "__deeds__";

    private static boolean registered = false;

    public static void register(final CommandDispatcher<CommandSourceStack> commandDispatcher)
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
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs,
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

    private static int execute(final CommandSourceStack source, final boolean up, final boolean down, final boolean here)
            throws CommandSyntaxException
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

        player.getServer().execute(() ->
        {

            final int x = player.blockPosition().getX() >> 4;
            final int y = player.blockPosition().getY() >> 4;
            final int z = player.blockPosition().getZ() >> 4;

            final Set<KGobalPos> deeds = Sets.newHashSet();

            final ResourceKey<Level> dim = player.getCommandSenderWorld().dimension();
            boolean done = false;
            if (here)
            {
                final int ret = Deed.unclaim(x, y, z, player, team, true, canUnclaimAnything);
                if (ret == 0)
                {
                    final KGobalPos chunk = KGobalPos.getPosition(dim, new BlockPos(x, y, z));
                    done = true;
                    deeds.add(chunk);
                }
                else return;
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
                        final KGobalPos chunk = KGobalPos.getPosition(dim, new BlockPos(x, y, z));
                        deeds.add(chunk);
                        done = true;
                        claimnum++;
                    }
                    else if (check == 3) owned_other++;
                }
                if (owned_other > 0) player.sendMessage(Essentials.config.getMessage(
                        "thutessentials.unclaim.notallowed.notowner", owned_other), Util.NIL_UUID);
                if (done) player.sendMessage(Essentials.config.getMessage("thutessentials.unclaim.done.num", claimnum,
                        team.teamName), Util.NIL_UUID);
                else player.sendMessage(Essentials.config.getMessage("thutessentials.unclaim.done.failed", claimnum,
                        team.teamName), Util.NIL_UUID);
            }

            if (!deeds.isEmpty())
            {
                final ItemStack deed = new ItemStack(Items.PAPER);
                deed.setTag(new CompoundTag());
                deed.getTag().putInt("num", deeds.size());
                deed.getTag().putBoolean("isDeed", true);
                int i = 0;
                for (final KGobalPos c : deeds)
                    deed.getTag().put("" + i++, CoordinateUtls.toNBT(c, "deed"));
                deed.setHoverName(Essentials.config.getMessage("thutessentials.deed.for", deeds.size(), x << 4,
                        z << 4));
                if (!player.addItem(deed)) player.drop(deed, false);
            }
            LandSaveHandler.saveTeam(team.teamName);
            LandSaveHandler.saveTeam(Deed.DEEDTEAM);
        });
        return 0;
    }

    private static int unclaim(final KGobalPos chunk, final Player player, final LandTeam team,
            final boolean messages, final boolean canUnclaimAnything)
    {
        final LandTeam owner = LandManager.getInstance().getLandOwner(chunk);
        if (LandManager.isWild(owner))
        {
            if (messages) player.sendMessage(Essentials.config.getMessage("thutessentials.unclaim.notallowed.noowner"),
                    Util.NIL_UUID);
            return 2;
        }
        else if (owner != team && !canUnclaimAnything)
        {
            if (messages) player.sendMessage(Essentials.config.getMessage("thutessentials.unclaim.notallowed.notowner",
                    owner.teamName), Util.NIL_UUID);
            return 3;
        }

        final Level world = player.getCommandSenderWorld();
        LandManager.getInstance().unclaimLand(team.teamName, world, chunk.getPos(), true);
        // ensure the deed team exist, and that it is set to reserved.
        Deed.initDeedTeam();
        // Transfers the claim over to the "deed team"
        LandManager.getInstance().claimLand(Deed.DEEDTEAM, world, chunk.getPos(), true);
        if (messages) player.sendMessage(Essentials.config.getMessage("thutessentials.unclaim.done", team.teamName),
                Util.NIL_UUID);

        return 0;
    }

    private static void initDeedTeam()
    {
        final LandTeam team = LandManager.getInstance().getTeam(Deed.DEEDTEAM, true);
        team.reserved = true;
    }

    private static int unclaim(final int x, final int y, final int z, final Player player, final LandTeam team,
            final boolean messages, final boolean canUnclaimAnything)
    {
        final ResourceKey<Level> dim = player.getCommandSenderWorld().dimension();
        final KGobalPos chunk = KGobalPos.getPosition(dim, new BlockPos(x, y, z));
        return Deed.unclaim(chunk, player, team, messages, canUnclaimAnything);
    }
}
