package thut.essentials.commands.misc;

import java.util.Collection;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.GameProfileArgument;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.util.NameManager;

public class Nick
{
    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        final String name = "nick";
        String perm;
        PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.OP, "Can the player use /" + name);
        if (Essentials.config.commandBlacklist.contains(name)) return;

        LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs,
                perm));

        command = command.then(Commands.argument("player", GameProfileArgument.gameProfile()).then(Commands.argument(
                "nick", StringArgumentType.greedyString()).executes(ctx -> Nick.execute(ctx.getSource(),
                        GameProfileArgument.getGameProfiles(ctx, "player"), StringArgumentType.getString(ctx,
                                "nick")))));
        // Actually register the command.
        commandDispatcher.register(command);
        NameManager.init();
    }

    private static int execute(final CommandSource source, final Collection<GameProfile> target, String nick)
    {
        if (nick.length() > 16) nick = nick.substring(0, 16);
        for (final GameProfile p : target)
            NameManager.setName(nick, p, source.getServer());
        return 0;
    }

}
