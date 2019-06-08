package thut.essentials.commands.kits;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import thut.essentials.util.BaseCommand;
import thut.essentials.util.KitManager;

public class ReloadKit extends BaseCommand
{

    public ReloadKit()
    {
        super("reloadkits", 4);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        KitManager.init();
        sender.sendMessage(new StringTextComponent("Reloaded Kits"));
    }

}
