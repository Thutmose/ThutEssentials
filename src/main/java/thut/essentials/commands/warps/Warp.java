package thut.essentials.commands.warps;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import thut.essentials.util.BaseCommand;
import thut.essentials.util.WarpManager;

public class Warp extends BaseCommand
{

    public Warp()
    {
        super("warp", 0);
    }

    @Override
    public String getUsage(ICommandSource sender)
    {
        return "/" + getName() + " <warpName>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        PlayerEntity player = getPlayerBySender(sender);
        String warpName = args[0];
        for (int i = 1; i < args.length; i++)
        {
            warpName = warpName + " " + args[i];
        }
        WarpManager.attemptWarp(player, warpName);
    }

}
