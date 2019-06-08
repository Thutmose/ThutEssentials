package thut.essentials.commands.land.management;

import com.mojang.authlib.GameProfile;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
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
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        PlayerEntity user = getPlayerBySender(sender);
        GameProfile player = getProfile(server, args[0]);
        LandTeam teamA = LandManager.getTeam(user.getUniqueID());
        LandTeam teamB = LandManager.getTeam(player.getId());
        if (teamA != teamB) throw new CommandException("You must be in the same team to do that.");
        String teamName = teamA.teamName;
        if (LandManager.getInstance().isAdmin(user.getUniqueID()))
        {
            LandManager.getInstance().addAdmin(player.getId(), teamName);
            sender.sendMessage(new StringTextComponent(player + " added as an Admin for Team " + teamName));
        }
        else
        {
            throw new CommandException("You do not have permission to do that");
        }
    }

}
