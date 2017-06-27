package thut.essentials.commands.land;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.util.BaseCommand;

public class SetAdmin extends BaseCommand
{

    public SetAdmin()
    {
        super("setTeamAdmin", 0);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        EntityPlayer user = getCommandSenderAsPlayer(sender);
        EntityPlayer player = getPlayer(server, sender, args[0]);
        LandTeam teamA = LandManager.getTeam(user);
        LandTeam teamB = LandManager.getTeam(player);
        if (teamA != teamB) throw new CommandException("You must be in the same team to do that.");
        String teamName = teamA.teamName;
        if (LandManager.getInstance().isAdmin(user.getUniqueID()))
        {
            LandManager.getInstance().addAdmin(player.getUniqueID(), teamName);
            sender.sendMessage(new TextComponentString(player + " added as an Admin for Team " + teamName));
        }
        else
        {
            throw new CommandException("You do not have permission to do that");
        }
    }

}
