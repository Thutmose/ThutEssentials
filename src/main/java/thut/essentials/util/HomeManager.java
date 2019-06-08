package thut.essentials.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.ClickEvent.Action;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;

public class HomeManager
{
    public static String[] HOMEPERMS = null;

    public static void registerPerms()
    {
        if (HOMEPERMS != null) return;
        HOMEPERMS = new String[ConfigManager.INSTANCE.maxHomes];
        for (int i = 0; i < ConfigManager.INSTANCE.maxHomes; i++)
        {
            HOMEPERMS[i] = "thutessentials.homes.max." + (i + 1);
            PermissionAPI.registerNode(HOMEPERMS[i], DefaultPermissionLevel.ALL,
                    "Can the player have this many homes (checked when adding a home).");
        }
    }

    public static int[] getHome(PlayerEntity player, String home)
    {
        if (home == null) home = "Home";
        CompoundNBT tag = PlayerDataHandler.getCustomDataTag(player);
        CompoundNBT homes = tag.getCompound("homes");
        if (homes.hasKey(home)) return homes.getIntArray(home);
        return null;
    }

    public static void setHome(PlayerEntity player, String home)
    {
        BlockPos pos = player.getPosition();
        if (home == null) home = "Home";
        CompoundNBT tag = PlayerDataHandler.getCustomDataTag(player);
        CompoundNBT homes = tag.getCompound("homes");
        int num = homes.getKeySet().size();
        if (num >= ConfigManager.INSTANCE.maxHomes)
        {
            ITextComponent message = new StringTextComponent(
                    "You may not have more than " + ConfigManager.INSTANCE.maxHomes + " homes!");
            message.getStyle().setColor(TextFormatting.DARK_RED);
            player.sendMessage(message);
            return;
        }
        String node = HOMEPERMS[num];
        if (!PermissionAPI.hasPermission(player, node))
        {
            ITextComponent message = new StringTextComponent("You may not have another home!");
            message.getStyle().setColor(TextFormatting.DARK_RED);
            player.sendMessage(message);
            return;
        }
        if (homes.hasKey(home))
        {
            ITextComponent message = new StringTextComponent(
                    "Already have " + home + " use /delhome to remove it first!");
            message.getStyle().setColor(TextFormatting.DARK_RED);
            player.sendMessage(message);
            return;
        }
        int[] loc = new int[] { pos.getX(), pos.getY(), pos.getZ(), player.dimension };
        homes.putIntArray(home, loc);
        tag.setTag("homes", homes);
        player.sendMessage(new StringTextComponent("set " + home));
        PlayerDataHandler.saveCustomData(player);
    }

    public static void removeHome(PlayerEntity player, String home)
    {
        if (home == null) home = "Home";
        CompoundNBT tag = PlayerDataHandler.getCustomDataTag(player);
        CompoundNBT homes = tag.getCompound("homes");
        homes.remove(home);
        tag.setTag("homes", homes);
        player.sendMessage(new StringTextComponent("Removed " + home));
        PlayerDataHandler.saveCustomData(player);
    }

    public static void sendHomeList(PlayerEntity player)
    {
        CompoundNBT tag = PlayerDataHandler.getCustomDataTag(player);
        CompoundNBT homes = tag.getCompound("homes");
        player.sendMessage(new StringTextComponent("================"));
        player.sendMessage(new StringTextComponent("      Homes     "));
        player.sendMessage(new StringTextComponent("================"));
        for (String s : homes.getKeySet())
        {
            Style style = new Style();
            style.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/home " + s));
            player.sendMessage(new StringTextComponent(s).setStyle(style));
        }
        player.sendMessage(new StringTextComponent("================"));
    }
}
