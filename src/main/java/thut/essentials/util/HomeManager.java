package thut.essentials.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.ClickEvent.Action;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.IPermissionHandler;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.context.PlayerContext;

public class HomeManager
{
    public static String[] HOMEPERMS = null;

    public static void registerPerms()
    {
        if (HOMEPERMS != null) return;
        IPermissionHandler manager = PermissionAPI.getPermissionHandler();
        HOMEPERMS = new String[ConfigManager.INSTANCE.maxHomes];
        for (int i = 0; i < ConfigManager.INSTANCE.maxHomes; i++)
        {
            HOMEPERMS[i] = "thutessentials.homes.max." + (i + 1);
            manager.registerNode(HOMEPERMS[i], DefaultPermissionLevel.ALL,
                    "Can the player have this many homes (checked when adding a home).");
        }
    }

    public static int[] getHome(EntityPlayer player, String home)
    {
        if (home == null) home = "Home";
        NBTTagCompound tag = PlayerDataHandler.getCustomDataTag(player);
        NBTTagCompound homes = tag.getCompoundTag("homes");
        if (homes.hasKey(home)) return homes.getIntArray(home);
        return null;
    }

    public static void setHome(EntityPlayer player, String home)
    {
        BlockPos pos = player.getPosition();
        if (home == null) home = "Home";
        NBTTagCompound tag = PlayerDataHandler.getCustomDataTag(player);
        NBTTagCompound homes = tag.getCompoundTag("homes");
        int num = homes.getKeySet().size();
        if (num >= ConfigManager.INSTANCE.maxHomes)
        {
            ITextComponent message = new TextComponentString(
                    "You may not have more than " + ConfigManager.INSTANCE.maxHomes + " homes!");
            message.getStyle().setColor(TextFormatting.DARK_RED);
            player.sendMessage(message);
            return;
        }
        String node = HOMEPERMS[num];
        IPermissionHandler manager = PermissionAPI.getPermissionHandler();
        if (!manager.hasPermission(player.getGameProfile(), node, new PlayerContext(player)))
        {
            ITextComponent message = new TextComponentString("You may not have another home!");
            message.getStyle().setColor(TextFormatting.DARK_RED);
            player.sendMessage(message);
            return;
        }
        if (homes.hasKey(home))
        {
            ITextComponent message = new TextComponentString(
                    "Already have " + home + " use /delhome to remove it first!");
            message.getStyle().setColor(TextFormatting.DARK_RED);
            player.sendMessage(message);
            return;
        }
        int[] loc = new int[] { pos.getX(), pos.getY(), pos.getZ(), player.dimension };
        homes.setIntArray(home, loc);
        tag.setTag("homes", homes);
        player.sendMessage(new TextComponentString("set " + home));
        PlayerDataHandler.saveCustomData(player);
    }

    public static void removeHome(EntityPlayer player, String home)
    {
        if (home == null) home = "Home";
        NBTTagCompound tag = PlayerDataHandler.getCustomDataTag(player);
        NBTTagCompound homes = tag.getCompoundTag("homes");
        homes.removeTag(home);
        tag.setTag("homes", homes);
        player.sendMessage(new TextComponentString("Removed " + home));
        PlayerDataHandler.saveCustomData(player);
    }

    public static void sendHomeList(EntityPlayer player)
    {
        NBTTagCompound tag = PlayerDataHandler.getCustomDataTag(player);
        NBTTagCompound homes = tag.getCompoundTag("homes");
        player.sendMessage(new TextComponentString("================"));
        player.sendMessage(new TextComponentString("      Homes     "));
        player.sendMessage(new TextComponentString("================"));
        for (String s : homes.getKeySet())
        {
            Style style = new Style();
            style.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/home " + s));
            player.sendMessage(new TextComponentString(s).setStyle(style));
        }
        player.sendMessage(new TextComponentString("================"));
    }
}
