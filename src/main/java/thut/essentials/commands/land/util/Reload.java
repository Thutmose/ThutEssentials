package thut.essentials.commands.land.util;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
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
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        sender.sendMessage(new StringTextComponent("Reloading Teams and Land from disk."));
        ConfigManager.INSTANCE = new ConfigManager(ConfigManager.INSTANCE.getConfigFile());
        LandSaveHandler.loadGlobalData();
    }
}
