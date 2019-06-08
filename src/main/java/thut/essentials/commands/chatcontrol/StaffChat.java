package thut.essentials.commands.chatcontrol;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import thut.essentials.commands.CommandManager;
import thut.essentials.util.BaseCommand;
import thut.essentials.util.ConfigManager;
import thut.essentials.util.RuleManager;

public class StaffChat extends BaseCommand
{
    final static Field staffField;

    static
    {
        Field temp = null;
        try
        {
            temp = ConfigManager.class.getDeclaredField("staff");
        }
        catch (SecurityException | NoSuchFieldException e)
        {
            e.printStackTrace();
        }
        staffField = temp;
    }

    public StaffChat()
    {
        super("staff", 2);
    }

    /** Return whether the specified command parameter index is a username
     * parameter. */
    @Override
    public boolean isUsernameIndex(String[] args, int index)
    {
        return index == 1 && (args[0].equals("!add") || args[0].equals("!remove"));
    }

    @Override
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        if (args[0].equals("!add"))
        {
            List<String> rulesList = Lists.newArrayList(ConfigManager.INSTANCE.staff);
            String playerName = args[1];
            UUID id = null;
            try
            {
                id = UUID.fromString(playerName);
            }
            catch (Exception e)
            {
            }
            GameProfile profile = new GameProfile(id, playerName);
            profile = TileEntitySkull.updateGameprofile(profile);
            if (profile.getId() == null) { throw new CommandException("Error, cannot find profile for " + playerName); }
            rulesList.add(profile.getId().toString());
            try
            {
                ConfigManager.INSTANCE.updateField(staffField, rulesList.toArray(new String[0]));
                sender.sendMessage(new StringTextComponent("Added to Staff: " + args[1]));
            }
            catch (Exception e)
            {
                sender.sendMessage(new StringTextComponent("Error adding to Staff"));
                e.printStackTrace();
            }
        }
        else if (args[0].equals("!remove"))
        {
            List<String> rulesList = Lists.newArrayList(ConfigManager.INSTANCE.staff);
            String playerName = args[1];
            GameProfile profile = new GameProfile(null, playerName);
            profile = TileEntitySkull.updateGameprofile(profile);
            if (profile.getId() == null) { throw new CommandException("Error, cannot find profile for " + playerName); }
            rulesList.remove(profile.getId().toString());
            try
            {
                ConfigManager.INSTANCE.updateField(staffField, rulesList.toArray(new String[0]));
                sender.sendMessage(new StringTextComponent("Removed from Staff: " + args[1]));
            }
            catch (Exception e)
            {
                sender.sendMessage(new StringTextComponent("Error remvoing from Staff"));
                e.printStackTrace();
            }
        }
        else
        {
            String message = args[0];
            for (int i = 1; i < args.length; i++)
            {
                message = message + " " + args[i];
            }
            message = RuleManager.format(message);
            ITextComponent mess = new StringTextComponent(
                    "[Staff]" + sender.getDisplayName().getFormattedText() + ": ");
            mess.getStyle().setColor(TextFormatting.YELLOW);
            mess.appendSibling(CommandManager.makeFormattedComponent(message, TextFormatting.AQUA, false));
            server.sendMessage(mess);
            for (String s : ConfigManager.INSTANCE.staff)
            {
                try
                {
                    UUID id = UUID.fromString(s);
                    PlayerEntity player = server.getPlayerList().getPlayerByUUID(id);
                    if (player != null)
                    {
                        player.sendMessage(mess);
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

            }
        }
    }

}
