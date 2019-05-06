package thut.essentials.commands.kits;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import thut.essentials.util.BaseCommand;
import thut.essentials.util.KitManager;

public class ReloadKit extends BaseCommand
{

    public ReloadKit()
    {
        super("reloadkits", 4);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        KitManager.init();
        sender.sendMessage(new TextComponentString("Reloaded Kits"));
    }

}
