package thut.essentials.commands.land;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.util.BaseCommand;
import thut.essentials.util.ConfigManager;

public class Delete extends BaseCommand
{
    private static final String PERMCLEANUP = "thutessentials.land.delete_team.all_empty";

    public Delete()
    {
        super("deleteTeam", 0);
        PermissionAPI.registerNode(PERMCLEANUP, DefaultPermissionLevel.OP,
                "Permission to bypass the land per player limit for a team.");
    }

    public void removeEmptyTeams()
    {
        Set<String> toRemove = new HashSet<>();
        Map<String, LandTeam> teamMap = LandManager.getInstance()._teamMap;
        for (String s : teamMap.keySet())
        {
            LandTeam team = teamMap.get(s);
            if (team.member.size() == 0 && !team.reserved && team != LandManager.getDefaultTeam()) toRemove.add(s);
        }
        for (String s : toRemove)
        {
            LandManager.getInstance().removeTeam(s);
        }
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        if (args.length == 1 && args[0].equals("empty"))
        {
            EntityPlayer player = getPlayerBySender(sender);
            if (player == null || PermissionAPI.hasPermission(player, PERMCLEANUP))
            {
                removeEmptyTeams();
                return;
            }
            else throw new CommandException("You do not have permission to do that.");
        }

        EntityPlayer player = getPlayerBySender(sender);
        LandTeam team = LandManager.getTeam(player);
        if (team == null) throw new CommandException("You are not in a team.");
        if (!LandManager.getInstance().isAdmin(player.getUniqueID())
                || team.teamName.equalsIgnoreCase(ConfigManager.INSTANCE.defaultTeamName))
        {
            sender.sendMessage(new TextComponentString("You are not Authorized to delete your team"));
            return;
        }
        LandManager.getInstance().removeTeam(team.teamName);
        return;
    }
}