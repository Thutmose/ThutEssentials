package thut.essentials.commands.land.management;

import java.util.Collection;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.util.BaseCommand;

public class Admins extends BaseCommand
{

    public Admins()
    {
        super("listteamadmins", 0);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        LandTeam team = LandManager.getTeam(getPlayerBySender(sender));
        String teamName = team.teamName;
        sender.sendMessage(new StringTextComponent("Admins of Team " + teamName));
        Collection<UUID> c = team.admin;
        for (UUID o : c)
        {
            GameProfile profile = getProfile(server, o);
            sender.sendMessage(new StringTextComponent("" + profile.getName()));
        }
    }

}
