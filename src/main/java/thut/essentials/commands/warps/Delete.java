package thut.essentials.commands.warps;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.util.HomeManager;

public class Delete
{
    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        final String name = "del_warp";
        if (!Essentials.config.commandBlacklist.contains(name))
        {
            String perm;
            PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.OP, "Can the player use /"
                    + name);
            // Setup with name and permission
            LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> CommandManager
                    .hasPerm(cs, perm));

            // Home name argument version.
            command = command.then(Commands.argument("home_name", StringArgumentType.string()).executes(ctx -> Delete
                    .execute(ctx.getSource(), StringArgumentType.getString(ctx, "home_name"))));
            commandDispatcher.register(command);

            // No argument version.
            command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs, perm));
            command = command.executes(ctx -> Delete.execute(ctx.getSource(), null));
            commandDispatcher.register(command);
        }
    }

    private static int execute(final CommandSource source, String homeName) throws CommandSyntaxException
    {
        if (homeName == null) homeName = "Home";
        final ServerPlayerEntity player = source.asPlayer();
        final int ret = HomeManager.setHome(player, homeName);
        ITextComponent message;
        switch (ret)
        {
        case 0:
            message = CommandManager.makeFormattedComponent("thutessentials.homes.removed", null, false, homeName);
            player.sendMessage(message);
            break;
        case 1:
            message = CommandManager.makeFormattedComponent("thutessentials.homes.noexists", null, false, homeName);
            player.sendMessage(message);
            break;
        }
        return ret;
    }
}
