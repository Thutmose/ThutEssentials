package thut.essentials.commands.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.util.PermNodes;
import thut.essentials.util.PermNodes.DefaultPermissionLevel;

public class Fly
{

    public static void register(final CommandDispatcher<CommandSourceStack> commandDispatcher)
    {
        final String name = "fly";
        if (Essentials.config.commandBlacklist.contains(name)) return;
        String perm;
        PermNodes.registerBooleanNode(perm = "command." + name, DefaultPermissionLevel.OP, "Can the player use /" + name);
        // Setup with name and permission
        final LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name).requires(cs -> CommandManager
                .hasPerm(cs, perm));
        command.executes(ctx -> Fly.execute(ctx.getSource())).then(Commands.argument("player", EntityArgument.entity())
                .executes(ctx -> Fly.execute(ctx.getSource(), EntityArgument.getEntity(ctx, "player"))));
        // Actually register the command.
        commandDispatcher.register(command);
    }

    private static int execute(final CommandSourceStack source) throws CommandSyntaxException
    {
        return Fly.execute(source, source.getPlayerOrException());
    }

    private static int execute(final CommandSourceStack source, final Entity entity)
    {
        if (entity instanceof ServerPlayer)
        {
            final ServerPlayer player = (ServerPlayer) entity;
            if (player.getAbilities().instabuild) return 0;
            player.getAbilities().mayfly = !player.getAbilities().mayfly;
            if (!player.getAbilities().mayfly) player.getAbilities().flying = false;
            player.onUpdateAbilities();
            player.displayClientMessage(Essentials.config.getMessage("thutessentials.fly.set." + player
                    .getAbilities().mayfly), false);
        }
        return 0;
    }

}
