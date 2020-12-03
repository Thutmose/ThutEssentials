package thut.essentials.commands.land.util;

import java.util.UUID;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.util.RuleManager;

public class Chat
{

    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        // TODO configurable this.
        final String name = "team_chat";
        if (Essentials.config.commandBlacklist.contains(name)) return;
        String perm;
        PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL,
                "Can the player use their team chat.");

        // Setup with name and permission
        LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs,
                perm));

        // Set up the command's arguments
        command = command.then(Commands.argument("message", StringArgumentType.greedyString()).executes(ctx -> Chat
                .execute(ctx.getSource(), StringArgumentType.getString(ctx, "message"))));

        // Actually register the command.
        commandDispatcher.register(command);
    }

    private static int execute(final CommandSource source, final String message) throws CommandSyntaxException
    {
        final PlayerEntity sender = source.asPlayer();
        final MinecraftServer server = source.getServer();
        final LandTeam team = LandManager.getTeam(sender);

        final IFormattableTextComponent mess = new StringTextComponent("[Team]" + sender.getDisplayName()
                .getString()
                + ": ");
        mess.setStyle(mess.getStyle().setColor(Color.fromTextFormatting(TextFormatting.YELLOW)));
        mess.append(CommandManager.makeFormattedComponent(RuleManager.format(message), TextFormatting.AQUA, false));

        if (Essentials.config.logTeamChat) server.sendMessage(mess, Util.DUMMY_UUID);
        for (final UUID id : team.member)
            try
            {
                final PlayerEntity player = server.getPlayerList().getPlayerByUUID(id);
                if (player != null) player.sendMessage(mess, Util.DUMMY_UUID);
            }
            catch (final Exception e)
            {
                e.printStackTrace();
            }
        return 0;
    }
}
