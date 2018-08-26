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

public class ListMembers extends BaseCommand
{

    public ListMembers()
    {
        super("listMembers", 0);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        LandTeam team = LandManager.getTeam(getPlayerBySender(sender));
        if (args.length == 1) team = LandManager.getInstance().getTeam(args[0], false);
        if (team == null) throw new CommandException("No team found by name " + args[0]);
        String teamName = team.teamName;
        sender.sendMessage(new TextComponentString("Members of Team " + teamName));
        Collection<UUID> c = team.member;
        for (UUID o : c)
        {
            if (o == null) continue;
            GameProfile profile = getProfile(server, o);
            sender.sendMessage(new TextComponentString("    " + profile.getName()));
        }
    }
}
