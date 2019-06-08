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

public class Kick extends BaseCommand
{

    public Kick()
    {
        super("kickfromteam", 0);
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
        PlayerEntity kicker = getPlayerBySender(sender);
        String toKick = args[0];
        GameProfile profile = getProfile(server, toKick);
        LandTeam team = LandManager.getTeam(profile.getId());
        LandTeam team1 = LandManager.getTeam(kicker);
        System.out.println(team1+" "+team);
        System.out.println(profile);
        if (team != team1 || team == LandManager.getDefaultTeam()) throw new CommandException("You cannot do that.");
        if (toKick.equalsIgnoreCase(sender.getName()) || team1.hasRankPerm(kicker.getUniqueID(), LandTeam.KICK))
        {
            LandManager.getInstance().removeFromTeam(profile.getId());
            sender.sendMessage(new StringTextComponent("Removed " + toKick + " From Team."));
        }
        else
        {
            throw new CommandException("You do not have permission to do that");
        }
    }

}
