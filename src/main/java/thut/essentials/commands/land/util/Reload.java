package thut.essentials.commands.land.util;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import thut.essentials.land.LandSaveHandler;
import thut.essentials.util.BaseCommand;
import thut.essentials.util.ConfigManager;

public class Reload extends BaseCommand
{

    public Reload()
    {
        super("reloadteams", 4);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        sender.sendMessage(new TextComponentString("Reloading Teams and Land from disk."));
        ConfigManager.INSTANCE = new ConfigManager(ConfigManager.INSTANCE.getConfigFile());
        LandSaveHandler.loadGlobalData();
    }
}
