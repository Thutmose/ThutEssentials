package thut.essentials.commands.warps;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import thut.essentials.util.BaseCommand;
import thut.essentials.util.WarpManager;

public class Warps extends BaseCommand
{

    public Warps()
    {
        super("warps", 0);
    }

    @Override
    public String getUsage(ICommandSource sender)
    {
        return "/" + getName();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        PlayerEntity player = getPlayerBySender(sender);
        WarpManager.sendWarpsList(player);
    }

}
