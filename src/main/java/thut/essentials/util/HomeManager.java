package thut.essentials.util;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.ClickEvent.Action;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.KGobalPos;

public class HomeManager
{
    public static final PermissionNode<Integer> HOMES_PERM = new PermissionNode<>("thutessentials", "homes.max",
            PermissionTypes.INTEGER, (p, i, x) ->
            {
                return Essentials.config.maxHomes;
            });

    public static void registerPerms()
    {
        PermNodes.registerNode(HOMES_PERM);
    }

    public static boolean canAddHome(final ServerPlayer player, final int index)
    {
        int homes = PermissionAPI.getPermission(player, HOMES_PERM);
        return index < homes;
    }

    public static KGobalPos getHome(final ServerPlayer player, String home)
    {
        if (home == null) home = "Home";
        final CompoundTag tag = PlayerDataHandler.getCustomDataTag(player);
        final CompoundTag homes = tag.getCompound("homes");
        // Legacy home
        if (homes.contains(home, 11))
        {
            final int[] pos = homes.getIntArray(home);
            if (pos.length == 4)
            {
                final BlockPos b = new BlockPos(pos[0], pos[1], pos[2]);
                final ResourceKey<Level> dim = LandManager.Coordinate.fromOld(pos[3]);
                return KGobalPos.getPosition(dim, b);
            }
            return null;
        }
        if (homes.contains(home)) return CoordinateUtls.fromNBT(homes.getCompound(home));
        return null;
    }

    public static int setHome(final ServerPlayer player, String home)
    {
        final BlockPos pos = player.blockPosition();
        if (home == null) home = "Home";
        final CompoundTag tag = PlayerDataHandler.getCustomDataTag(player);
        final CompoundTag homes = tag.getCompound("homes");
        final int num = homes.getAllKeys().size();
        // Too many
        if (num >= Essentials.config.maxHomes) return 1;
        // No perms
        if (!HomeManager.canAddHome(player, num)) return 2;
        // Already exists
        if (homes.contains(home)) return 3;
        final KGobalPos loc = KGobalPos.getPosition(player.getCommandSenderWorld().dimension(), pos);
        homes.put(home, CoordinateUtls.toNBT(loc, home));
        tag.put("homes", homes);
        ChatHelper.sendSystemMessage(player, new TextComponent("set " + home));
        PlayerDataHandler.saveCustomData(player);
        return 0;
    }

    public static int removeHome(final ServerPlayer player, String home)
    {
        if (home == null) home = "Home";
        final CompoundTag tag = PlayerDataHandler.getCustomDataTag(player);
        final CompoundTag homes = tag.getCompound("homes");
        // No home!
        if (!homes.contains(home)) return 1;
        homes.remove(home);
        tag.put("homes", homes);
        PlayerDataHandler.saveCustomData(player);
        return 0;
    }

    public static void sendHomeList(final ServerPlayer player)
    {
        final CompoundTag tag = PlayerDataHandler.getCustomDataTag(player);
        final CompoundTag homes = tag.getCompound("homes");
        ChatHelper.sendSystemMessage(player, CommandManager.makeFormattedComponent("thutessentials.homes.header"));
        for (String s : homes.getAllKeys())
        {
            final MutableComponent message = CommandManager.makeFormattedComponent("thutessentials.homes.entry", null,
                    false, s);
            if (s.contains(" ")) s = "\"" + s + "\"";
            final Style style = message.getStyle().withClickEvent(new ClickEvent(Action.RUN_COMMAND, "/home " + s));
            ChatHelper.sendSystemMessage(player, message.setStyle(style));
        }
        ChatHelper.sendSystemMessage(player, CommandManager.makeFormattedComponent("thutessentials.homes.footer"));
    }
}