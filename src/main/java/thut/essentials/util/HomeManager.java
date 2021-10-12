package thut.essentials.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.ClickEvent.Action;
import net.minecraft.world.level.Level;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.KGobalPos;

public class HomeManager
{
    public static String[] HOMEPERMS = null;

    public static void registerPerms()
    {
        if (HomeManager.HOMEPERMS != null && HomeManager.HOMEPERMS.length >= Essentials.config.maxHomes) return;
        HomeManager.HOMEPERMS = new String[Essentials.config.maxHomes];
        for (int i = 0; i < Essentials.config.maxHomes; i++)
        {
            HomeManager.HOMEPERMS[i] = "thutessentials.homes.max." + (i + 1);
            PermissionAPI.registerNode(HomeManager.HOMEPERMS[i], DefaultPermissionLevel.ALL,
                    "Can the player have this many homes (checked when adding a home).");
        }
    }

    public static boolean canAddHome(final ServerPlayer player, final int index)
    {
        for (int i = HomeManager.HOMEPERMS.length - 1; i >= index; i--)
        {
            final String perm = HomeManager.HOMEPERMS[i];
            if (PermissionAPI.hasPermission(player, perm)) return true;
        }
        return false;
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
        player.sendMessage(new TextComponent("set " + home), Util.NIL_UUID);
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
        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.homes.header"), Util.NIL_UUID);
        for (String s : homes.getAllKeys())
        {
            final MutableComponent message = CommandManager.makeFormattedComponent(
                    "thutessentials.homes.entry", null, false, s);
            if (s.contains(" ")) s = "\"" + s + "\"";
            final Style style = message.getStyle().withClickEvent(new ClickEvent(Action.RUN_COMMAND, "/home " + s));
            player.sendMessage(message.setStyle(style), Util.NIL_UUID);
        }
        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.homes.footer"), Util.NIL_UUID);
    }
}