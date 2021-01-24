package thut.essentials.util;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.ClickEvent.Action;
import net.minecraft.world.World;
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

    public static boolean canAddHome(final ServerPlayerEntity player, final int index)
    {
        for (int i = HomeManager.HOMEPERMS.length - 1; i >= index; i--)
        {
            final String perm = HomeManager.HOMEPERMS[i];
            if (PermissionAPI.hasPermission(player, perm)) return true;
        }
        return false;
    }

    public static KGobalPos getHome(final ServerPlayerEntity player, String home)
    {
        if (home == null) home = "Home";
        final CompoundNBT tag = PlayerDataHandler.getCustomDataTag(player);
        final CompoundNBT homes = tag.getCompound("homes");
        // Legacy home
        if (homes.contains(home, 11))
        {
            final int[] pos = homes.getIntArray(home);
            if (pos.length == 4)
            {
                final BlockPos b = new BlockPos(pos[0], pos[1], pos[2]);
                final RegistryKey<World> dim = LandManager.Coordinate.fromOld(pos[3]);
                return KGobalPos.getPosition(dim, b);
            }
            return null;
        }
        if (homes.contains(home)) return CoordinateUtls.fromNBT(homes.getCompound(home));
        return null;
    }

    public static int setHome(final ServerPlayerEntity player, String home)
    {
        final BlockPos pos = player.getPosition();
        if (home == null) home = "Home";
        final CompoundNBT tag = PlayerDataHandler.getCustomDataTag(player);
        final CompoundNBT homes = tag.getCompound("homes");
        final int num = homes.keySet().size();
        // Too many
        if (num >= Essentials.config.maxHomes) return 1;
        // No perms
        if (!HomeManager.canAddHome(player, num)) return 2;
        // Already exists
        if (homes.contains(home)) return 3;
        final KGobalPos loc = KGobalPos.getPosition(player.getEntityWorld().getDimensionKey(), pos);
        homes.put(home, CoordinateUtls.toNBT(loc, home));
        tag.put("homes", homes);
        player.sendMessage(new StringTextComponent("set " + home), Util.DUMMY_UUID);
        PlayerDataHandler.saveCustomData(player);
        return 0;
    }

    public static int removeHome(final ServerPlayerEntity player, String home)
    {
        if (home == null) home = "Home";
        final CompoundNBT tag = PlayerDataHandler.getCustomDataTag(player);
        final CompoundNBT homes = tag.getCompound("homes");
        // No home!
        if (!homes.contains(home)) return 1;
        homes.remove(home);
        tag.put("homes", homes);
        PlayerDataHandler.saveCustomData(player);
        return 0;
    }

    public static void sendHomeList(final ServerPlayerEntity player)
    {
        final CompoundNBT tag = PlayerDataHandler.getCustomDataTag(player);
        final CompoundNBT homes = tag.getCompound("homes");
        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.homes.header"), Util.DUMMY_UUID);
        for (String s : homes.keySet())
        {
            final IFormattableTextComponent message = CommandManager.makeFormattedComponent(
                    "thutessentials.homes.entry", null, false, s);
            if (s.contains(" ")) s = "\"" + s + "\"";
            final Style style = message.getStyle().setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/home " + s));
            player.sendMessage(message.setStyle(style), Util.DUMMY_UUID);
        }
        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.homes.footer"), Util.DUMMY_UUID);
    }
}