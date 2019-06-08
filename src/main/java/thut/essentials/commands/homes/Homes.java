package thut.essentials.commands.homes;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import thut.essentials.util.BaseCommand;
import thut.essentials.util.HomeManager;

public class Homes extends BaseCommand
{
    public Homes()
    {
        super("homes", 0);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        ServerPlayerEntity player = getPlayerBySender(sender);
        HomeManager.sendHomeList(player);
    }

}
