package thut.essentials.commands.land.management;

import com.mojang.authlib.GameProfile;

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
        super("setteamadmin", 0);
    }

    /** Return whether the specified command parameter index is a username
     * parameter. */
    @Override
    public boolean isUsernameIndex(String[] args, int index)
    {
        return index == 0;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        EntityPlayer user = getPlayerBySender(sender);
        GameProfile player = getProfile(server, args[0]);
        LandTeam teamA = LandManager.getTeam(user.getUniqueID());
        LandTeam teamB = LandManager.getTeam(player.getId());
        if (teamA != teamB) throw new CommandException("You must be in the same team to do that.");
        String teamName = teamA.teamName;
        if (LandManager.getInstance().isAdmin(user.getUniqueID()))
        {
            LandManager.getInstance().addAdmin(player.getId(), teamName);
            sender.sendMessage(new TextComponentString(player + " added as an Admin for Team " + teamName));
        }
        else
        {
            throw new CommandException("You do not have permission to do that");
        }
    }

}
