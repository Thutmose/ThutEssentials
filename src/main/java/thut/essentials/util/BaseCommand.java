package thut.essentials.util;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.UserListOpsEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import thut.essentials.commands.CommandManager;

public abstract class BaseCommand extends CommandBase
{
    static final Map<String, Integer> permsMap = Maps.newHashMap();

    final String                      key;
    final int                         perm;

    public BaseCommand(String key, int perms, String... aliases)
    {
        this.key = key;
        List<String> alii = CommandManager.commands.get(key);
        if (alii == null) CommandManager.commands.put(key, alii = Lists.newArrayList(key));
        for (String s : aliases)
            if (!alii.contains(s)) alii.add(s);
        if (!alii.contains(key.toLowerCase(Locale.ENGLISH))) alii.add(key.toLowerCase(Locale.ENGLISH));
        perm = getPermissionLevel(perms);
    }

    private int getPermissionLevel(int deault_)
    {
        if (permsMap.containsKey(key)) return permsMap.get(key);
        for (String s : getAliases())
            if (permsMap.containsKey(s)) return permsMap.get(s);
        return deault_;
    }

    @Override
    public int getRequiredPermissionLevel()
    {
        return perm;
    }

    /** Check if the given ICommandSender has permission to execute this
     * command */
    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender)
    {
        if (!(sender instanceof EntityPlayer) || !server.isDedicatedServer()) return true;
        EntityPlayer player = null;
        try
        {
            player = getCommandSenderAsPlayer(sender);
        }
        catch (PlayerNotFoundException e)
        {
            return false;
        }
        if (server.getPlayerList().canSendCommands(player.getGameProfile())) return true;
        UserListOpsEntry userlistopsentry = server.getPlayerList().getOppedPlayers().getEntry(player.getGameProfile());
        return userlistopsentry != null ? userlistopsentry.getPermissionLevel() >= perm : perm <= 0;
    }

    @Override
    public String getName()
    {
        if (CommandManager.commands.get(key) == null) { return key; }
        return CommandManager.commands.get(key).get(0);
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return "/" + getName();
    }

    @Override
    public List<String> getAliases()
    {
        if (CommandManager.commands.get(key) != null) { return CommandManager.commands.get(key); }
        return Collections.<String> emptyList();
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
            @Nullable BlockPos pos)
    {
        int last = args.length - 1;
        if (last >= 0 && isUsernameIndex(args,
                last)) { return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames()); }
        return Collections.<String> emptyList();
    }

    public void destroy()
    {
        MinecraftForge.EVENT_BUS.unregister(this);
    }
}
