package thut.essentials.commands.land.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;

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
    }

    private static int execute(final CommandSource source, final String message) throws CommandSyntaxException
    {
        final PlayerEntity sender = source.asPlayer();
        // source.sendFeedback(new
        // TranslationTextComponent("thutessentials.team.created", teamname),
        // true);
        return 0;
    }
}
