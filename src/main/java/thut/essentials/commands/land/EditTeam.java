package thut.essentials.commands.land;

import java.util.UUID;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.land.LandSaveHandler;
import thut.essentials.util.BaseCommand;
import thut.essentials.util.ConfigManager;
import thut.essentials.util.Coordinate;
import thut.essentials.util.RuleManager;

public class EditTeam extends BaseCommand
{
    private static final String PERMRESERVELAND        = "thutessentials.land.toggle.reserve";
    private static final String PERMTOGGLEMOBS         = "thutessentials.land.toggle.mobspawn";
    private static final String PERMTOGGLEEXPLODE      = "thutessentials.land.toggle.explode";
    private static final String PERMTOGGLEFF           = "thutessentials.land.toggle.friendlyfire";
    private static final String PERMTOGGLEPLAYERDAMAGE = "thutessentials.land.toggle.playerdamage";

    public EditTeam()
    {
        super("editteam", 0, "editTeam");
        PermissionAPI.registerNode(PERMTOGGLEEXPLODE, DefaultPermissionLevel.OP,
                "Allowed to toggle explosions on/off in their team land");
        PermissionAPI.registerNode(PERMTOGGLEMOBS, DefaultPermissionLevel.OP,
                "Allowed to toggle mob spawns on/off in their team land");
        PermissionAPI.registerNode(PERMRESERVELAND, DefaultPermissionLevel.OP,
                "Allowed to toggle reserved status on/off for their team");
        PermissionAPI.registerNode(PERMTOGGLEFF, DefaultPermissionLevel.OP,
                "Allowed to toggle friendly fire on/off for their team");
        PermissionAPI.registerNode(PERMTOGGLEPLAYERDAMAGE, DefaultPermissionLevel.OP,
                "Allowed to toggle player damage on/off in their team land");
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        EntityPlayer player = getPlayerBySender(sender);
        LandTeam landTeam = LandManager.getTeam(player);
        String arg = args[0];
        String message = "";
        if (args.length > 1) message = args[1];
        for (int i = 2; i < args.length; i++)
        {
            message = message + " " + args[i];
        }
        message = RuleManager.format(message);
        if (arg.equalsIgnoreCase("public"))
        {
            if (!landTeam.isAdmin(player)) throw new CommandException("You are not allowed to do that.");
            landTeam.allPublic = parseBoolean(message);
            sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Set all public to " + message));
            LandSaveHandler.saveTeam(landTeam.teamName);
            return;
        }
        if (arg.equalsIgnoreCase("anyPlace"))
        {
            if (!landTeam.isAdmin(player)) throw new CommandException("You are not allowed to do that.");
            landTeam.anyPlace = parseBoolean(message);
            sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Set anyPlace to " + message));
            LandSaveHandler.saveTeam(landTeam.teamName);
            return;
        }
        if (arg.equalsIgnoreCase("anyBreak"))
        {
            if (!landTeam.isAdmin(player)) throw new CommandException("You are not allowed to do that.");
            landTeam.anyBreak = parseBoolean(message);
            sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Set anyBreak to " + message));
            LandSaveHandler.saveTeam(landTeam.teamName);
            return;
        }
        if (arg.equalsIgnoreCase("exit"))
        {
            if (!landTeam.hasRankPerm(player.getUniqueID(), LandTeam.EDITMESSAGES))
                throw new CommandException("You are not allowed to do that.");
            landTeam.exitMessage = message;
            sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Set Exit Message to " + message));
            LandSaveHandler.saveTeam(landTeam.teamName);
            return;
        }
        if (arg.equalsIgnoreCase("enter"))
        {
            if (!landTeam.hasRankPerm(player.getUniqueID(), LandTeam.EDITMESSAGES))
                throw new CommandException("You are not allowed to do that.");
            landTeam.enterMessage = message;
            sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Set Enter Message to " + message));
            LandSaveHandler.saveTeam(landTeam.teamName);
            return;
        }
        if (arg.equalsIgnoreCase("deny"))
        {
            if (!landTeam.hasRankPerm(player.getUniqueID(), LandTeam.EDITMESSAGES))
                throw new CommandException("You are not allowed to do that.");
            landTeam.denyMessage = message;
            sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Set Deny Message to " + message));
            LandSaveHandler.saveTeam(landTeam.teamName);
            return;
        }
        if (arg.equalsIgnoreCase("prefix"))
        {
            if (!landTeam.hasRankPerm(player.getUniqueID(), LandTeam.SETPREFIX))
                throw new CommandException("You are not allowed to do that.");
            if (message.length() > ConfigManager.INSTANCE.prefixLength)
                message = message.substring(0, ConfigManager.INSTANCE.prefixLength);
            landTeam.prefix = message;
            sender.sendMessage(
                    new TextComponentString(TextFormatting.GREEN + "Set Prefix to " + TextFormatting.RESET + message));
            refreshTeam(landTeam, server);
            LandSaveHandler.saveTeam(landTeam.teamName);
            return;
        }
        if (arg.equalsIgnoreCase("home"))
        {
            if (!landTeam.hasRankPerm(player.getUniqueID(), LandTeam.SETHOME))
                throw new CommandException("You are not allowed to do that.");
            landTeam.home = new Coordinate(player.getPosition(), player.dimension);
            sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Set Team Home to " + landTeam.home));
            LandSaveHandler.saveTeam(landTeam.teamName);
            return;
        }
        if (arg.equalsIgnoreCase("reserve") && PermissionAPI.hasPermission(player, PERMRESERVELAND))
        {
            landTeam.reserved = Boolean.parseBoolean(message);
            sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "reserved set to " + landTeam.reserved));
            LandSaveHandler.saveTeam(landTeam.teamName);
            return;
        }
        if (arg.equalsIgnoreCase("noPlayerDamage") && PermissionAPI.hasPermission(player, PERMTOGGLEPLAYERDAMAGE))
        {
            landTeam.noPlayerDamage = Boolean.parseBoolean(message);
            sender.sendMessage(
                    new TextComponentString(TextFormatting.GREEN + "noPlayerDamage set to " + landTeam.noPlayerDamage));
            LandSaveHandler.saveTeam(landTeam.teamName);
            return;
        }
        if (arg.equalsIgnoreCase("friendlyFire") && PermissionAPI.hasPermission(player, PERMTOGGLEFF))
        {
            landTeam.friendlyFire = Boolean.parseBoolean(message);
            sender.sendMessage(
                    new TextComponentString(TextFormatting.GREEN + "friendlyFire set to " + landTeam.friendlyFire));
            LandSaveHandler.saveTeam(landTeam.teamName);
            return;
        }
        if (arg.equalsIgnoreCase("noMobSpawn") && PermissionAPI.hasPermission(player, PERMTOGGLEMOBS))
        {
            landTeam.noMobSpawn = Boolean.parseBoolean(message);
            sender.sendMessage(
                    new TextComponentString(TextFormatting.GREEN + "noMobSpawn set to " + landTeam.noMobSpawn));
            LandSaveHandler.saveTeam(landTeam.teamName);
            return;
        }
        if (arg.equalsIgnoreCase("noExplosions") && PermissionAPI.hasPermission(player, PERMTOGGLEEXPLODE))
        {
            landTeam.noExplosions = Boolean.parseBoolean(message);
            sender.sendMessage(
                    new TextComponentString(TextFormatting.GREEN + "noExplosions set to " + landTeam.noExplosions));
            LandSaveHandler.saveTeam(landTeam.teamName);
            return;
        }
    }

    public static void refreshTeam(LandTeam team, MinecraftServer server)
    {
        for (UUID id : team.member)
        {
            try
            {
                EntityPlayer player = server.getPlayerList().getPlayerByUUID(id);
                if (player != null)
                {
                    player.refreshDisplayName();
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }
}
