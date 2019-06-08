package thut.essentials.commands.land.util;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import thut.essentials.land.LandManager;
import thut.essentials.util.BaseCommand;

public class Team extends BaseCommand
{

    public Team()
    {
        super("myteam", 0);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        PlayerEntity player = getPlayerBySender(sender);
        sender.sendMessage(
                new StringTextComponent("Currently a member of Team " + LandManager.getTeam(player).teamName));
    }

}
