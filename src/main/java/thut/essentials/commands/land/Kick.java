package thut.essentials.commands.land;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.util.BaseCommand;

public class Kick extends BaseCommand
{

    public Kick()
    {
        super("kickFromTeam", 0);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        EntityPlayer kicker = getCommandSenderAsPlayer(sender);
        String toKick = args[0];
        EntityPlayer kickee = getPlayer(server, sender, toKick);
        LandTeam team = LandManager.getTeam(kickee);
        LandTeam team1 = LandManager.getTeam(kicker);
        if (team != team1 || team == LandManager.getDefaultTeam()) throw new CommandException("You cannot do that.");
        if (toKick.equalsIgnoreCase(sender.getName()) || team1.hasPerm(kicker.getUniqueID(), LandTeam.KICK))
        {
            LandManager.getInstance().removeFromTeam(kickee.getUniqueID());
            sender.sendMessage(new TextComponentString("Removed " + toKick + " From Team."));
        }
        else
        {
            throw new CommandException("You do not have permission to do that");
        }
    }

}
