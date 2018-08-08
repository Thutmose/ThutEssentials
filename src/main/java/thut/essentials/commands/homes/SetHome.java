package thut.essentials.commands.homes;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import thut.essentials.util.BaseCommand;
import thut.essentials.util.HomeManager;

public class SetHome extends BaseCommand
{
    public SetHome()
    {
        super("sethome", 0);
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return "/" + getName()+" <optional|homeName>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        EntityPlayerMP player = getPlayerBySender(sender);
        String homeName = args.length > 0 ? args[0] : null;
        HomeManager.setHome(player, homeName);
    }

}
