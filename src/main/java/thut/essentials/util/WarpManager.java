package thut.essentials.util;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import net.minecraft.command.CommandException;
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
import net.minecraftforge.server.permission.IPermissionHandler;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.context.PlayerContext;
import thut.essentials.ThutEssentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.commands.misc.Spawn;
import thut.essentials.commands.misc.Spawn.PlayerMover;

public class WarpManager
{
    public static Map<String, int[]> warpLocs;
    final static Field               warpsField;

    static
    {
        Field temp = null;
        try
        {
            temp = ConfigManager.class.getDeclaredField("warps");
        }
        catch (SecurityException | NoSuchFieldException e)
        {
            e.printStackTrace();
        }
        warpsField = temp;
    }

    static void init()
    {
        warpLocs = Maps.newHashMap();
        for (String s : ConfigManager.INSTANCE.warps)
        {
            String[] args = s.split(":");
            warpLocs.put(args[0], getInt(args[1]));
        }

        IPermissionHandler manager = PermissionAPI.getPermissionHandler();
        for (String s : warpLocs.keySet())
        {
            String node = "thutessentials.warp." + s;
            if (!manager.getRegisteredNodes().contains(node))
            {
                manager.registerNode(node, DefaultPermissionLevel.ALL, "Warp to " + s);
            }
        }
    }

    static int[] getInt(String val)
    {
        String[] args = val.split(" ");
        int dim = args.length == 4 ? Integer.parseInt(args[3]) : 0;
        return new int[] { Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]), dim };
    }

    public static void setWarp(BlockPos center, int dimension, String name) throws Exception
    {
        List<String> warps = Lists.newArrayList(ConfigManager.INSTANCE.warps);
        for (String s : warps)
        {
            String[] args = s.split(":");
            if (args[0].equals(s)) { throw new CommandException(
                    "Requested warp already exits, try /delwarp " + name + " to remove it"); }
        }
        String warp = name + ":" + center.getX() + " " + center.getY() + " " + center.getZ() + " " + dimension;
        warps.add(warp);
        warpLocs.put(name, new int[] { center.getX(), center.getY(), center.getZ(), dimension });
        IPermissionHandler manager = PermissionAPI.getPermissionHandler();
        String node = "thutessentials.warp." + name;
        if (!manager.getRegisteredNodes().contains(node))
        {
            manager.registerNode(node, DefaultPermissionLevel.ALL, "Warp to " + name);
        }
        ConfigManager.INSTANCE.updateField(warpsField, warps.toArray(new String[0]));
    }

    public static void delWarp(String name) throws Exception
    {
        List<String> warps = Lists.newArrayList(ConfigManager.INSTANCE.warps);
        for (String s : warps)
        {
            String[] args = s.split(":");
            if (args[0].equals(name))
            {
                warps.remove(s);
                warpLocs.remove(name);
                ConfigManager.INSTANCE.updateField(warpsField, warps.toArray(new String[0]));
                return;
            }
        }
        throw new CommandException("Warp " + name + " does not exist");
    }

    public static int[] getWarp(String name)
    {
        return warpLocs.get(name);
    }

    public static void sendWarpsList(PlayerEntity player)
    {
        IPermissionHandler manager = PermissionAPI.getPermissionHandler();
        PlayerContext context = new PlayerContext(player);
        player.sendMessage(new StringTextComponent("================"));
        player.sendMessage(new StringTextComponent("      Warps     "));
        player.sendMessage(new StringTextComponent("================"));
        for (String s : ConfigManager.INSTANCE.warps)
        {
            String[] args = s.split(":");
            s = args[0];
            if (!manager.hasPermission(player.getGameProfile(), "thutessentials.warp." + s, context)) continue;
            Style style = new Style();
            style.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/warp " + s));
            player.sendMessage(new StringTextComponent(s).setStyle(style));
        }
        player.sendMessage(new StringTextComponent("================"));
    }

    public static void attemptWarp(PlayerEntity player, String warpName) throws CommandException
    {
        int[] warp = WarpManager.getWarp(warpName);
        CompoundNBT tag = PlayerDataHandler.getCustomDataTag(player);
        CompoundNBT tptag = tag.getCompound("tp");
        long last = tptag.getLong("warpDelay");
        long time = player.getServer().getWorld(0).getGameTime();
        if (last > time)
        {
            player.sendMessage(
                    CommandManager.makeFormattedComponent("Too Soon between Warp attempt", TextFormatting.RED, false));
            return;
        }
        if (warp != null)
        {
            IPermissionHandler manager = PermissionAPI.getPermissionHandler();
            PlayerContext context = new PlayerContext(player);
            if (!manager.hasPermission(player.getGameProfile(), "thutessentials.warp." + warpName, context))
                throw new CommandException("You may not use this warp.");
            ITextComponent teleMess = CommandManager.makeFormattedComponent("Warped to " + warpName,
                    TextFormatting.GREEN);
            PlayerMover.setMove(player, ThutEssentials.instance.config.warpActivateDelay, warp[3],
                    new BlockPos(warp[0], warp[1], warp[2]), teleMess, Spawn.INTERUPTED);
            tptag.putLong("warpDelay", time + ConfigManager.INSTANCE.warpReUseDelay);
            tag.setTag("tp", tptag);
            PlayerDataHandler.saveCustomData(player);
        }
        else throw new CommandException("Warp " + warpName + " not found.");
    }
}
