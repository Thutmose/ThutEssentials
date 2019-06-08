package thut.essentials.commands.land.management;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.land.LandManager.PlayerRank;
import thut.essentials.land.LandSaveHandler;
import thut.essentials.util.BaseCommand;

public class EditRanks extends BaseCommand
{

    public EditRanks()
    {
        super("teamranks", 0);
    }

    /** Return whether the specified command parameter index is a username
     * parameter. */
    @Override
    public boolean isUsernameIndex(String[] args, int index)
    {
        return index == 2;
    }

    @Override
    public String getUsage(ICommandSource sender)
    {
        return super.getUsage(sender) + " <arg> <player> <value>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        PlayerEntity player = getPlayerBySender(sender);
        LandTeam landTeam = LandManager.getTeam(player);
        if (!landTeam.isAdmin(player)) throw new CommandException("Only Team Admins may manage ranks");
        if (args.length == 0) throw new CommandException(getUsage(sender));
        String type = args[0];
        String rankName;
        PlayerEntity target;
        PlayerRank rank;
        String perm;
        boolean added;
        switch (type)
        {
        case "addRank":
            rankName = args[1];
            rank = landTeam.rankMap.get(rankName);
            if (rank != null) throw new CommandException("Rank " + rankName + " already exists.");
            landTeam.rankMap.put(rankName, new PlayerRank());
            player.sendMessage(new StringTextComponent("Added Rank " + rankName));
            LandSaveHandler.saveTeam(landTeam.teamName);
            break;
        case "setRank":
            rankName = args[1];
            target = getPlayer(server, sender, args[2]);
            rank = landTeam.rankMap.get(rankName);
            if (rank == null) throw new CommandException("Rank " + rankName + " does not exist.");
            rank.members.add(target.getUniqueID());
            landTeam._ranksMembers.put(target.getUniqueID(), rank);
            target.refreshDisplayName();
            player.sendMessage(new StringTextComponent("Added " + target.getName() + " to Rank " + rankName));
            LandSaveHandler.saveTeam(landTeam.teamName);
            break;
        case "setPerm":
            rankName = args[1];
            rank = landTeam.rankMap.get(rankName);
            if (rank == null) throw new CommandException("Rank " + rankName + " does not exist.");
            perm = args[2];
            added = rank.perms.add(perm);
            if (added) player.sendMessage(new StringTextComponent("Allowed " + perm));
            else player.sendMessage(new StringTextComponent("Already has " + perm));
            break;
        case "delPerm":
            rankName = args[1];
            rank = landTeam.rankMap.get(rankName);
            if (rank == null) throw new CommandException("Rank " + rankName + " does not exist.");
            perm = args[2];
            added = rank.perms.remove(perm);
            if (added) player.sendMessage(new StringTextComponent("Removed " + perm));
            else player.sendMessage(new StringTextComponent("Did not have " + perm));
            break;
        case "setPrefix":
            rankName = args[1];
            rank = landTeam.rankMap.get(rankName);
            if (rank == null) throw new CommandException("Rank " + rankName + " does not exist.");
            perm = args[2];
            rank.prefix = perm;
            if (perm.trim().isEmpty()) rank.prefix = null;
            added = rank.prefix != null;
            if (added) player.sendMessage(new StringTextComponent("Set Prefix to " + rank.prefix));
            else player.sendMessage(new StringTextComponent("Removed Rank Prefix"));
            break;
        case "listRanks":
            Set<String> ranks = landTeam.rankMap.keySet();
            player.sendMessage(new StringTextComponent("Ranks in your team:"));
            for (String s : ranks)
            {
                player.sendMessage(new StringTextComponent("  " + s));
            }
            break;
        case "listMembers":
            rankName = args[1];
            rank = landTeam.rankMap.get(rankName);
            if (rank == null) throw new CommandException("Rank " + rankName + " does not exist.");
            Collection<UUID> c = rank.members;
            player.sendMessage(new StringTextComponent("Members of " + rankName));
            for (UUID o : c)
            {
                GameProfile profile = getProfile(server, o);
                sender.sendMessage(new StringTextComponent("  " + profile.getName()));
            }
            break;
        }
    }

}
