package thut.essentials.commands.land.management;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.land.LandManager.Relation;
import thut.essentials.land.LandSaveHandler;
import thut.essentials.util.BaseCommand;

public class EditRelations extends BaseCommand
{
    public static List<String>        perms     = Lists.newArrayList();
    public static Map<String, String> perm_info = Maps.newHashMap();

    static
    {
        perms.add(LandTeam.BREAK);
        perms.add(LandTeam.PLACE);
        perms.add(LandTeam.PUBLIC);
        perms.add(LandTeam.ALLY);

        perm_info.put(LandTeam.BREAK, "Allowd to break blocks.");
        perm_info.put(LandTeam.PLACE, "Allowd to place blocks.");
        perm_info.put(LandTeam.PUBLIC, "Everything counts as if Public Toggle was used.");
        perm_info.put(LandTeam.ALLY, "Counts as \"Ally\" by anything that uses that.");

        Collections.sort(perms);
    }

    public EditRelations()
    {
        super("teamrelations", 0);
    }

    /** Return whether the specified command parameter index is a username
     * parameter. */
    @Override
    public boolean isUsernameIndex(String[] args, int index)
    {
        return index == 2;
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return super.getUsage(sender) + " <relations|perms|set|unset> if set/unset, then <team> <perm>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        EntityPlayer player = getPlayerBySender(sender);
        LandTeam landTeam = LandManager.getTeam(player);
        if (!landTeam.isAdmin(player)) throw new CommandException("Only Team Admins may manage relations");
        if (args.length == 0) throw new CommandException(getUsage(sender));
        String type = args[0];
        List<String> keys = Lists.newArrayList(landTeam.relations.keySet());
        String other;
        String perm;
        Relation relation;
        LandTeam team;
        switch (type)
        {
        // List off the relations, as well as any perms they might have.
        case "relations":
            player.sendMessage(new TextComponentString(TextFormatting.AQUA + "Relations of " + landTeam.teamName));
            for (String s : keys)
            {
                relation = landTeam.relations.get(s);
                player.sendMessage(new TextComponentString(TextFormatting.AQUA + "    " + s));
                for (String s1 : relation.perms)
                {
                    player.sendMessage(new TextComponentString(TextFormatting.AQUA + "        " + s1));
                }
            }
            break;
        // List off all of the valid permissions for relations.
        case "perms":
            player.sendMessage(new TextComponentString(TextFormatting.AQUA + "Allowed Relation Permissions:"));
            for (String s : perms)
            {
                player.sendMessage(
                        new TextComponentString(TextFormatting.AQUA + "    " + s + " - " + perm_info.get(s)));
            }
            break;
        // Set the given perm to the given relation, makes relation if not
        // there.
        case "set":
            if (args.length != 3) throw new CommandException(getUsage(sender));
            other = args[1];
            perm = args[2];
            if (!perms.contains(perm))
                throw new CommandException("Use " + super.getUsage(sender) + " perms for a list of valid options");
            team = LandManager.getInstance().getTeam(other, false);
            if (team == null) throw new CommandException(other + " is not an existing team.");
            relation = landTeam.relations.get(other);
            if (relation == null)
            {
                landTeam.relations.put(other, relation = new Relation());
            }
            if (relation.perms.add(perm))
                player.sendMessage(new TextComponentString(TextFormatting.AQUA + "Set perm " + perm + " for " + other));
            else player.sendMessage(new TextComponentString(TextFormatting.AQUA + other + " already had " + perm));
            LandSaveHandler.saveTeam(other);
            break;
        // unsets the given perm, removes the relation if empty.
        case "unset":
            if (args.length != 3) throw new CommandException(getUsage(sender));
            other = args[1];
            perm = args[2];
            if (!perms.contains(perm))
                throw new CommandException("Use " + super.getUsage(sender) + " perms for a list of valid options");
            team = LandManager.getInstance().getTeam(other, false);
            if (team == null) throw new CommandException(other + " is not an existing team.");
            relation = landTeam.relations.get(other);
            if (relation == null)
            {
                player.sendMessage(new TextComponentString(TextFormatting.AQUA + "No Relations with " + other));
                return;
            }
            if (relation.perms.remove(perm)) player.sendMessage(
                    new TextComponentString(TextFormatting.AQUA + "Removed perm " + perm + " for " + other));
            else player
                    .sendMessage(new TextComponentString(TextFormatting.AQUA + other + " does not have perm " + perm));
            if (relation.perms.isEmpty()) landTeam.relations.remove(other);
            LandSaveHandler.saveTeam(other);
            break;
        }
    }
}
