package thut.essentials.commands.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;

public class Fly
{

    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        final String name = "fly";
        if (Essentials.config.commandBlacklist.contains(name)) return;
        String perm;
        PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.OP, "Can the player use /" + name);
        // Setup with name and permission
        final LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> CommandManager
                .hasPerm(cs, perm));
        command.executes(ctx -> Fly.execute(ctx.getSource())).then(Commands.argument("player", EntityArgument.entity())
                .executes(ctx -> Fly.execute(ctx.getSource(), EntityArgument.getEntity(ctx, "player"))));
        // Actually register the command.
        commandDispatcher.register(command);
    }

    private static int execute(final CommandSource source) throws CommandSyntaxException
    {
        return Fly.execute(source, source.asPlayer());
    }

    private static int execute(final CommandSource source, final Entity entity)
    {
        if (entity instanceof ServerPlayerEntity)
        {
            final ServerPlayerEntity player = (ServerPlayerEntity) entity;
            if (player.abilities.isCreativeMode) return 0;
            player.abilities.allowFlying = !player.abilities.allowFlying;
            if (!player.abilities.allowFlying) player.abilities.isFlying = false;
            player.sendPlayerAbilities();
            player.sendStatusMessage(Essentials.config.getMessage("thutessentials.fly.set."
                    + player.abilities.allowFlying), false);
        }
        return 0;
    }

}
