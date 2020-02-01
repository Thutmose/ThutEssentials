package thut.essentials.commands.land.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.land.LandSaveHandler;

public class Reload
{

    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        // TODO configurable this.
        final String name = "reload_teams";
        if (Essentials.config.commandBlacklist.contains(name)) return;
        String perm;
        PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.OP,
                "Can the player reload the teams list.");

        // Setup with name and permission
        LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs,
                perm));
        // No target argument version
        command = command.executes(ctx -> Reload.execute(ctx.getSource()));
    }

    private static int execute(final CommandSource source) throws CommandSyntaxException
    {
        source.sendFeedback(new TranslationTextComponent("thutessentials.teams.reloaded"), true);
        LandSaveHandler.loadGlobalData();
        return 0;
    }
}
