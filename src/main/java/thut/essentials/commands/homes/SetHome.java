package thut.essentials.commands.homes;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.ServerPlayerEntity;
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
    public String getUsage(ICommandSource sender)
    {
        return "/" + getName()+" <optional|homeName>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        ServerPlayerEntity player = getPlayerBySender(sender);
        String homeName = args.length > 0 ? args[0] : null;
        HomeManager.setHome(player, homeName);
    }

}
