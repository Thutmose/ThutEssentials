package thut.essentials.commands.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.util.PermNodes;
import thut.essentials.util.PermNodes.DefaultPermissionLevel;

public class Repair
{
    public static void register(final CommandDispatcher<CommandSourceStack> commandDispatcher)
    {
        final String name = "repair";
        if (Essentials.config.commandBlacklist.contains(name)) return;
        String perm;
        PermNodes.registerBooleanNode(perm = "command." + name, DefaultPermissionLevel.OP, "Can the player use /" + name);
        // Setup with name and permission
        final LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name).requires(cs -> CommandManager
                .hasPerm(cs, perm));
        command.executes(ctx -> Repair.execute(ctx.getSource())).then(Commands.argument("player", EntityArgument
                .player()).executes(ctx -> Repair.execute(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"))));
        // Actually register the command.
        commandDispatcher.register(command);
    }

    private static int execute(final CommandSourceStack source) throws CommandSyntaxException
    {
        return Repair.execute(source, source.getPlayerOrException());
    }

    private static int execute(final CommandSourceStack source, final ServerPlayer player)
    {
        final ItemStack stack = player.getMainHandItem();
        if (stack != null && stack.isDamaged()) stack.setDamageValue(0);
        return 0;
    }
}
