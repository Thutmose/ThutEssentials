package thut.essentials.commands.land;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.util.BaseCommand;

public class Invite extends BaseCommand
{

    public Invite()
    {
        super("teamInvite", 0);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        String player = args[0];
        EntityPlayer inviter = getCommandSenderAsPlayer(sender);
        EntityPlayer invitee = getPlayer(server, sender, player);
        LandTeam landTeam = LandManager.getTeam(inviter);
        if (!landTeam.hasPerm(inviter.getUniqueID(), LandTeam.INVITE))
            throw new CommandException("You are not allowed to do that.");
        String team = landTeam.teamName;
        boolean invite = LandManager.getInstance().invite(inviter.getUniqueID(), invitee.getUniqueID());
        if (!invite) throw new CommandException("Invite not successful.");
        String links = "";
        String cmd = "joinTeam";
        String command = "/" + cmd + " " + team;
        String abilityJson = "{\"text\":\"" + team
                + "\",\"color\":\"green\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"" + command + ""
                + "\"}}";
        links = abilityJson;
        invitee.sendMessage(new TextComponentString("New Invite to Team " + team));
        ITextComponent message = ITextComponent.Serializer.jsonToComponent("[\" [\"," + links + ",\"]\"]");
        inviter.sendMessage(new TextComponentString("Invite sent"));
        invitee.sendMessage(message);
    }
}
