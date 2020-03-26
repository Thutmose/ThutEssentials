package thut.essentials.util;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.ClickEvent.Action;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;

public class HomeManager
{
    public static String[] HOMEPERMS = null;

    public static void registerPerms()
    {
        if (HomeManager.HOMEPERMS != null) return;
        HomeManager.HOMEPERMS = new String[Essentials.config.maxHomes];
        for (int i = 0; i < Essentials.config.maxHomes; i++)
        {
            HomeManager.HOMEPERMS[i] = "thutessentials.homes.max." + (i + 1);
            PermissionAPI.registerNode(HomeManager.HOMEPERMS[i], DefaultPermissionLevel.ALL,
                    "Can the player have this many homes (checked when adding a home).");
        }
    }

    public static int[] getHome(final ServerPlayerEntity player, String home)
    {
        if (home == null) home = "Home";
        final CompoundNBT tag = PlayerDataHandler.getCustomDataTag(player);
        final CompoundNBT homes = tag.getCompound("homes");
        if (homes.contains(home)) return homes.getIntArray(home);
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
        final String node = HomeManager.HOMEPERMS[num];
        // No perms
        if (!PermissionAPI.hasPermission(player, node)) return 2;
        // Already exists
        if (homes.contains(home)) return 3;
        final int[] loc = new int[] { pos.getX(), pos.getY(), pos.getZ(), player.dimension.getId() };
        homes.putIntArray(home, loc);
        tag.put("homes", homes);
        player.sendMessage(new StringTextComponent("set " + home));
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
        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.homes.header"));
        for (String s : homes.keySet())
        {
            final Style style = new Style();
            if (s.contains(" ")) s = "\"" + s + "\"";
            style.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/home " + s));
            final ITextComponent message = CommandManager.makeFormattedComponent("thutessentials.homes.entry", null,
                    false, s);
            player.sendMessage(message.setStyle(style));
        }
        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.homes.footer"));
    }
}