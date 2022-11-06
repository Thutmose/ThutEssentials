package thut.essentials.commands.land.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.entity.player.Player;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.land.LandEventsHandler;
import thut.essentials.util.PermNodes;
import thut.essentials.util.PermNodes.DefaultPermissionLevel;

public class Show
{
    public static void register(final CommandDispatcher<CommandSourceStack> commandDispatcher)
    {
        final String name = "show_land";
        if (Essentials.config.commandBlacklist.contains(name)) return;
        String perm;
        PermNodes.registerBooleanNode(perm = "command." + name, DefaultPermissionLevel.ALL,
                "Can the player see the ownership status of land nearby.");

        // Setup with name and permission
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs,
                perm));
        // No target argument version
        command = command.executes(ctx -> Show.execute(ctx.getSource()));

        // Actually register the command.
        commandDispatcher.register(command);
    }

    private static int execute(final CommandSourceStack source) throws CommandSyntaxException
    {
        final Player player = source.getPlayerOrException();
        if (LandEventsHandler.EntityEventHandler.showLandSet.remove(player.getUUID())) Essentials.config
                .sendFeedback(source, "thutessentials.team.landdisplay.unset", false);
        else if (LandEventsHandler.EntityEventHandler.showLandSet.add(player.getUUID())) Essentials.config
                .sendFeedback(source, "thutessentials.team.landdisplay.set", false);
        return 0;
    }

}
