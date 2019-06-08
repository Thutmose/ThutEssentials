package thut.essentials.commands.land.management;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.util.BaseCommand;

public class Leave extends BaseCommand
{

    public Leave()
    {
        super("leaveteam", 0);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        PlayerEntity player = getPlayerBySender(sender);
        LandTeam team = LandManager.getTeam(player);
        LandTeam def = LandManager.getDefaultTeam();
        if (team == def) throw new CommandException("You cannot leave the default team");
        LandManager.getInstance().removeFromTeam(player.getUniqueID());
        sender.sendMessage(new StringTextComponent("Left Team " + team.teamName));
    }

}
