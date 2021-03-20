package thut.essentials.commands.land.util;

import java.util.Map;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;

public class Teams
{

    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        final String name = "team_teams";
        if (Essentials.config.commandBlacklist.contains(name)) return;
        String perm;
        PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL,
                "Can the player see the list of teams.");

        // Setup with name and permission
        LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs,
                perm));
        // No target argument version
        command = command.executes(ctx -> Teams.execute(ctx.getSource()));

        // Actually register the command.
        commandDispatcher.register(command);
    }

    private static int execute(final CommandSource source)
    {
        Essentials.config.sendFeedback(source, "thutessentials.team.teams", false);
        final Map<String, LandTeam> teamMap = LandManager.getInstance()._teamMap;
        for (final String s : teamMap.keySet())
        {
            final LandTeam team = teamMap.get(s);
            String emptyTip = "";
            final String lastSeenTip = "[" + (source.getServer().getNextTickTime() - team.lastSeen) / 1000 * 3600 + "h]";
            if (team.member.size() == 0) emptyTip = "(EMPTY)";
            final IFormattableTextComponent message = new StringTextComponent(TextFormatting.AQUA + "["
                    + TextFormatting.YELLOW + s + TextFormatting.AQUA + "] " + emptyTip + " " + lastSeenTip);

            final ClickEvent event = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/team_members " + s);
            final IFormattableTextComponent tooltip = Members.getMembers(source.getServer(), team, false);
            final HoverEvent event2 = new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltip);
            message.setStyle(message.getStyle().withClickEvent(event).withHoverEvent(event2));
            source.sendSuccess(message, false);
        }
        return 0;
    }
}
