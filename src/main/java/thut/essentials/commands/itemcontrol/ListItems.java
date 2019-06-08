package thut.essentials.commands.itemcontrol;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import thut.essentials.itemcontrol.ItemControl;
import thut.essentials.util.BaseCommand;
import thut.essentials.util.ConfigManager;

public class ListItems extends BaseCommand
{
    public ListItems()
    {
        super("baditems", 0);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        sender.sendMessage(
                new StringTextComponent("Current Blackist State: " + ConfigManager.INSTANCE.itemControlEnabled));
        if (ConfigManager.INSTANCE.itemControlEnabled)
        {
            sender.sendMessage(new StringTextComponent("Current Item Blacklist:"));
            for (String s : ItemControl.blacklist)
            {
                sender.sendMessage(new StringTextComponent(s));
            }
        }
    }
}
