package thut.essentials.commands.land;

import java.util.Map;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.ClickEvent.Action;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.util.BaseCommand;

public class ListTeams extends BaseCommand
{

    public ListTeams()
    {
        super("listteams", 0);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        sendTeamList(sender);
    }

    public void sendTeamList(ICommandSender sender)
    {
        Map<String, LandTeam> teamMap = LandManager.getInstance()._teamMap;
        sender.sendMessage(new TextComponentString(TextFormatting.AQUA + "Team List:"));
        for (String s : teamMap.keySet())
        {
            LandTeam team = teamMap.get(s);
            String emptyTip = "";
            String lastSeenTip = "[" + (sender.getServer().getEntityWorld().getTotalWorldTime() - team.lastSeen) / 24000
                    + " MC Days]";
            if (team.member.size() == 0) emptyTip = "(EMPTY)";
            ITextComponent message = new TextComponentString(TextFormatting.AQUA + "[" + TextFormatting.YELLOW + s
                    + TextFormatting.AQUA + "] " + emptyTip + " " + lastSeenTip);
            ClickEvent event = new ClickEvent(Action.RUN_COMMAND, "/listMembers " + s);
            message.setStyle(new Style());
            message.getStyle().setClickEvent(event);
            sender.sendMessage(message);
        }
    }

}
