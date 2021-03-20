package thut.essentials.commands.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;

public class Repair
{
    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        final String name = "repair";
        if (Essentials.config.commandBlacklist.contains(name)) return;
        String perm;
        PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.OP, "Can the player use /" + name);
        // Setup with name and permission
        final LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> CommandManager
                .hasPerm(cs, perm));
        command.executes(ctx -> Repair.execute(ctx.getSource())).then(Commands.argument("player", EntityArgument
                .player()).executes(ctx -> Repair.execute(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"))));
        // Actually register the command.
        commandDispatcher.register(command);
    }

    private static int execute(final CommandSource source) throws CommandSyntaxException
    {
        return Repair.execute(source, source.getPlayerOrException());
    }

    private static int execute(final CommandSource source, final ServerPlayerEntity player)
    {
        final ItemStack stack = player.getMainHandItem();
        if (stack != null && stack.isDamaged()) stack.setDamageValue(0);
        return 0;
    }
}
