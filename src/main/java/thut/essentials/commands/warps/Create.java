package thut.essentials.commands.warps;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.land.LandManager.KGobalPos;
import thut.essentials.util.WarpManager;

public class Create
{
    public static void register(final CommandDispatcher<CommandSourceStack> commandDispatcher)
    {
        final String name = "set_warp";
        if (!Essentials.config.commandBlacklist.contains(name))
        {
            String perm;
            PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.OP, "Can the player use /"
                    + name);
            // Setup with name and permission
            LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name).requires(cs -> CommandManager
                    .hasPerm(cs, perm));

            command = command.then(Commands.argument("warp_name", StringArgumentType.string()).executes(ctx -> Create
                    .execute(ctx.getSource(), StringArgumentType.getString(ctx, "warp_name"))));
            commandDispatcher.register(command);
        }
    }

    private static int execute(final CommandSourceStack source, final String warpName, final BlockPos center,
            final ResourceKey<Level> registryKey) throws CommandSyntaxException
    {
        final ServerPlayer player = source.getPlayerOrException();
        final int ret = WarpManager.setWarp(KGobalPos.getPosition(registryKey, center), warpName);
        Component message;
        switch (ret)
        {
        case 0:
            message = CommandManager.makeFormattedComponent("thutessentials.warps.added", null, false, warpName);
            player.sendMessage(message, Util.NIL_UUID);
            break;
        case 1:
            message = CommandManager.makeFormattedComponent("thutessentials.warps.exists", null, false, warpName);
            player.sendMessage(message, Util.NIL_UUID);
            break;
        }
        return ret;
    }

    private static int execute(final CommandSourceStack source, final String warpName) throws CommandSyntaxException
    {
        return Create.execute(source, warpName, new BlockPos(source.getPosition()), source.getLevel().dimension());
    }
}
