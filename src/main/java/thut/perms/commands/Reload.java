package thut.perms.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.perms.Perms;
import thut.perms.management.GroupManager;

public class Reload
{

    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        // TODO configurable this.
        final String name = "reload_perms";
        if (Essentials.config.commandBlacklist.contains(name)) return;
        String perm;
        PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.OP,
                "Can the player reload the permissions from files.");

        // Setup with name and permission
        LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs,
                perm));
        // No target argument version
        command = command.executes(ctx -> Reload.execute(ctx.getSource()));

        // Actually register the command.
        commandDispatcher.register(command);
    }

    private static int execute(final CommandSource source) throws CommandSyntaxException
    {
        source.sendFeedback(new TranslationTextComponent("thutperms.reloaded"), true);
        final MinecraftServer server = source.getServer();
        Perms.loadPerms();
        GroupManager.get_instance()._server = server;
        // Reload player names, to apply the tags if they exist
        // for (final ServerPlayerEntity player :
        // server.getPlayerList().getPlayers())
        // {
        // // GroupManager.get_instance()._manager.createPlayer(player);
        // // player.refreshDisplayName();//Displayname not implemented in
        // // forge yet. FIXME get this working.
        // }
        return 0;
    }
}