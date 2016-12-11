package thut.essentials.commands.land;

import java.util.UUID;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import thut.essentials.commands.CommandManager;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.util.BaseCommand;
import thut.essentials.util.ConfigManager;
import thut.essentials.util.RuleManager;

public class Chat extends BaseCommand
{

    public Chat()
    {
        super("tchat", 0);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        String message = args[0];
        for (int i = 1; i < args.length; i++)
        {
            message = message + " " + args[i];
        }
        message = RuleManager.format(message);
        ITextComponent mess = new TextComponentString("[Team]" + sender.getDisplayName().getFormattedText() + ": ");
        mess.getStyle().setColor(TextFormatting.YELLOW);
        mess.appendSibling(CommandManager.makeFormattedComponent(message, TextFormatting.AQUA, false));
        LandTeam team = LandManager.getTeam(getCommandSenderAsPlayer(sender));
        if (ConfigManager.INSTANCE.logTeamChat) server.addChatMessage(mess);
        for (UUID id : team.member)
        {
            try
            {
                EntityPlayer player = server.getPlayerList().getPlayerByUUID(id);
                if (player != null)
                {
                    player.addChatMessage(mess);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

        }
    }
}
