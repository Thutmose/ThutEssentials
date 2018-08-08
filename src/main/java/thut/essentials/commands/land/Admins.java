package thut.essentials.commands.land;

import java.util.Collection;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.util.BaseCommand;

public class Admins extends BaseCommand
{

    public Admins()
    {
        super("listTeamAdmins", 0);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        LandTeam team = LandManager.getTeam(getCommandSenderAsPlayer(sender));
        String teamName = team.teamName;
        sender.sendMessage(new TextComponentString("Admins of Team " + teamName));
        Collection<UUID> c = team.admin;
        for (UUID o : c)
        {
            GameProfile profile = getProfile(server, o);
            sender.sendMessage(new TextComponentString("" + profile.getName()));
        }
    }

}
