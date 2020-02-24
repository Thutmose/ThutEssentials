package thut.essentials.commands.land.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.land.LandEventsHandler;

public class Show
{
    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        final String name = "show_land";
        if (Essentials.config.commandBlacklist.contains(name)) return;
        String perm;
        PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL,
                "Can the player see the ownership status of land nearby.");

        // Setup with name and permission
        LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs,
                perm));
        // No target argument version
        command = command.executes(ctx -> Show.execute(ctx.getSource()));

        // Actually register the command.
        commandDispatcher.register(command);
    }

    private static int execute(final CommandSource source) throws CommandSyntaxException
    {
        final PlayerEntity player = source.asPlayer();
        if (LandEventsHandler.EntityEventHandler.showLandSet.remove(player.getUniqueID())) Essentials.config
                .sendFeedback(source, "thutessentials.team.landdisplay.unset", false);
        else if (LandEventsHandler.EntityEventHandler.showLandSet.add(player.getUniqueID())) Essentials.config
                .sendFeedback(source, "thutessentials.team.landdisplay.set", false);
        return 0;
    }

}
