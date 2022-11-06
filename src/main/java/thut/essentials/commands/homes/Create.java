package thut.essentials.commands.homes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.util.ChatHelper;
import thut.essentials.util.HomeManager;
import thut.essentials.util.PermNodes;
import thut.essentials.util.PermNodes.DefaultPermissionLevel;

public class Create
{
    public static void register(final CommandDispatcher<CommandSourceStack> commandDispatcher)
    {
        final String name = "set_home";
        if (!Essentials.config.commandBlacklist.contains(name))
        {
            String perm;
            PermNodes.registerBooleanNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /"
                    + name);
            // Setup with name and permission
            LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name).requires(cs -> CommandManager
                    .hasPerm(cs, perm));

            // Home name argument version.
            command = command.then(Commands.argument("home_name", StringArgumentType.string()).executes(ctx -> Create
                    .execute(ctx.getSource(), StringArgumentType.getString(ctx, "home_name"))));
            commandDispatcher.register(command);

            // No argument version.
            command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs, perm));
            command = command.executes(ctx -> Create.execute(ctx.getSource(), null));
            commandDispatcher.register(command);
        }
    }

    private static int execute(final CommandSourceStack source, String homeName) throws CommandSyntaxException
    {
        if (homeName == null) homeName = "Home";
        final ServerPlayer player = source.getPlayerOrException();
        final int ret = HomeManager.setHome(player, homeName);
        Component message;
        switch (ret)
        {
        case 0:
            message = CommandManager.makeFormattedComponent("thutessentials.homes.added", null, false, homeName);
            ChatHelper.sendSystemMessage(player, message);
            break;
        case 1:
            message = CommandManager.makeFormattedComponent("thutessentials.homes.toomany");
            ChatHelper.sendSystemMessage(player, message);
            break;
        case 2:
            message = CommandManager.makeFormattedComponent("thutessentials.homes.noperms");
            ChatHelper.sendSystemMessage(player, message);
            break;
        case 3:
            message = CommandManager.makeFormattedComponent("thutessentials.homes.exists", null, false, homeName);
            ChatHelper.sendSystemMessage(player, message);
            break;
        }
        return ret;
    }
}
