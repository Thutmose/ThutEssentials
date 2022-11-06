package thut.essentials.commands.land.util;

import java.util.UUID;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.util.ChatHelper;
import thut.essentials.util.PermNodes;
import thut.essentials.util.PermNodes.DefaultPermissionLevel;
import thut.essentials.util.RuleManager;

public class Chat
{

    public static void register(final CommandDispatcher<CommandSourceStack> commandDispatcher)
    {
        // TODO configurable this.
        final String name = "team_chat";
        if (Essentials.config.commandBlacklist.contains(name)) return;
        String perm;
        PermNodes.registerBooleanNode(perm = "command." + name, DefaultPermissionLevel.ALL,
                "Can the player use their team chat.");

        // Setup with name and permission
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs,
                perm));

        // Set up the command's arguments
        command = command.then(Commands.argument("message", StringArgumentType.greedyString()).executes(ctx -> Chat
                .execute(ctx.getSource(), StringArgumentType.getString(ctx, "message"))));

        // Actually register the command.
        commandDispatcher.register(command);
    }

    private static int execute(final CommandSourceStack source, final String message) throws CommandSyntaxException
    {
        final Player sender = source.getPlayerOrException();
        final MinecraftServer server = source.getServer();
        final LandTeam team = LandManager.getTeam(sender);

        final MutableComponent mess = Component.literal("[Team]" + sender.getDisplayName()
                .getString()
                + ": ");
        mess.setStyle(mess.getStyle().withColor(TextColor.fromLegacyFormat(ChatFormatting.YELLOW)));
        mess.append(CommandManager.makeFormattedComponent(RuleManager.format(message), ChatFormatting.AQUA, false));

        if (Essentials.config.logTeamChat) server.sendSystemMessage(mess);
        for (final UUID id : team.member)
            try
            {
                final Player player = server.getPlayerList().getPlayer(id);
                if (player != null) ChatHelper.sendSystemMessage(player, mess);
            }
            catch (final Exception e)
            {
                e.printStackTrace();
            }
        return 0;
    }
}
