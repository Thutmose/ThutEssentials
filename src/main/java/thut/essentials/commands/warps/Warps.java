package thut.essentials.commands.warps;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.util.WarpManager;

public class Warps
{
    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        String name = "warps";
        if (!Essentials.config.commandBlacklist.contains(name))
        {
            String perm;
            PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /"
                    + name);
            // Setup with name and permission
            LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> CommandManager
                    .hasPerm(cs, perm));
            // Register the execution.
            command = command.executes(ctx -> Warps.execute(ctx.getSource()));

            // Actually register the command.
            commandDispatcher.register(command);
        }
        name = "warp";
        if (!Essentials.config.commandBlacklist.contains(name))
        {
            String perm;
            PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /"
                    + name);
            // Setup with name and permission
            LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> CommandManager
                    .hasPerm(cs, perm));

            // Home name argument version.
            command = command.then(Commands.argument("warp_name", StringArgumentType.string()).executes(ctx -> Warps
                    .execute(ctx.getSource(), StringArgumentType.getString(ctx, "warp_name"))));
            commandDispatcher.register(command);
        }
    }

    private static int execute(final CommandSource source) throws CommandSyntaxException
    {
        final ServerPlayerEntity player = source.asPlayer();
        WarpManager.sendWarpsList(player);
        return 0;
    }

    private static int execute(final CommandSource source, final String warpName) throws CommandSyntaxException
    {
        final ServerPlayerEntity player = source.asPlayer();
        final int ret = WarpManager.attemptWarp(player, warpName);
        ITextComponent message;
        switch (ret)
        {
        case 0:
            message = CommandManager.makeFormattedComponent("thutessentials.warps.warping", null, false, warpName);
            player.sendMessage(message, Util.DUMMY_UUID);
            break;
        case 1:
            message = CommandManager.makeFormattedComponent("thutessentials.tp.tosoon");
            player.sendMessage(message, Util.DUMMY_UUID);
            break;
        case 2:
            message = CommandManager.makeFormattedComponent("thutessentials.warps.noperms");
            player.sendMessage(message, Util.DUMMY_UUID);
            break;
        case 3:
            message = CommandManager.makeFormattedComponent("thutessentials.homes.noexists_use");
            player.sendMessage(message, Util.DUMMY_UUID);
            break;
        }
        return ret;
    }
}
