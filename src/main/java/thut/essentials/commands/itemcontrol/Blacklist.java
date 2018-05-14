package thut.essentials.commands.itemcontrol;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import thut.essentials.itemcontrol.ItemControl;
import thut.essentials.util.BaseCommand;
import thut.essentials.util.ConfigManager;

public class Blacklist extends BaseCommand
{
    public Blacklist()
    {
        super("blacklist", 2);
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return "/" + getName() + " list|add|remove <items>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        if (args.length == 0) throw new CommandException(getUsage(sender));
        String arg = args[0];

        if (arg.equalsIgnoreCase("list"))
        {
            sender.sendMessage(
                    new TextComponentString("Current Blackist State: " + ConfigManager.INSTANCE.itemControlEnabled));
            if (ConfigManager.INSTANCE.itemControlEnabled)
            {
                sender.sendMessage(new TextComponentString("Current Item Blacklist:"));
                for (String s : ItemControl.blacklist)
                {
                    sender.sendMessage(new TextComponentString(s));
                }
            }
        }
        else
        {
            if (args.length == 1) throw new CommandException(getUsage(sender));
            String itemname = args[1];
            String message = "";
            if (arg.equalsIgnoreCase("add"))
            {
                if (ItemControl.blacklist.contains(itemname)) throw new CommandException("already in blacklist");
                ItemControl.blacklist.add(itemname);
                message = itemname + " added to blacklist";
            }
            else if (arg.equalsIgnoreCase("remove"))
            {
                if (!ItemControl.blacklist.contains(itemname)) throw new CommandException("item is not in blacklist");
                ItemControl.blacklist.remove(itemname);
                message = itemname + " removed from blacklist";
            }
            List<String> blacklist = Lists.newArrayList(ItemControl.blacklist);
            Collections.sort(blacklist);
            try
            {
                ConfigManager.INSTANCE.updateField(ConfigManager.class.getDeclaredField("itemBlacklist"),
                        blacklist.toArray(new String[0]));
                sender.sendMessage(new TextComponentString(message));
            }
            catch (Exception e)
            {
                throw new CommandException("Something when wrong updating the config list");
            }
        }
    }

}
