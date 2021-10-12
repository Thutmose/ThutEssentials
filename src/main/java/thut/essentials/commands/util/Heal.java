package thut.essentials.commands.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;

public class Heal
{
    public static void register(final CommandDispatcher<CommandSourceStack> commandDispatcher)
    {
        final String name = "heal";
        if (Essentials.config.commandBlacklist.contains(name)) return;
        String perm;
        PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.OP, "Can the player use /" + name);
        // Setup with name and permission
        final LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name).requires(cs -> CommandManager
                .hasPerm(cs, perm));
        command.executes(ctx -> Heal.execute(ctx.getSource())).then(Commands.argument("player", EntityArgument.entity())
                .executes(ctx -> Heal.execute(ctx.getSource(), EntityArgument.getEntity(ctx, "player"))));
        // Actually register the command.
        commandDispatcher.register(command);
    }

    private static int execute(final CommandSourceStack source) throws CommandSyntaxException
    {
        return Heal.execute(source, source.getPlayerOrException());
    }

    private static int execute(final CommandSourceStack source, final Entity entity)
    {
        if (entity instanceof LivingEntity)
        {
            final LivingEntity living = (LivingEntity) entity;
            living.setHealth(living.getMaxHealth());
        }
        if (entity instanceof ServerPlayer)
        {
            final ServerPlayer player = (ServerPlayer) entity;
            player.getFoodData().setFoodLevel(20);
        }
        return 0;
    }
}
