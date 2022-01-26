package thut.essentials.commands.land.management;

import java.util.Collection;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.server.level.ServerPlayer;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.util.PermNodes;
import thut.essentials.util.PermNodes.DefaultPermissionLevel;

public class Kick
{

    public static void register(final CommandDispatcher<CommandSourceStack> commandDispatcher)
    {
        final String name = "kick_team_member";
        if (!Essentials.config.commandBlacklist.contains(name))
        {
            String perm;
            PermNodes.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /"
                    + name);

            LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name).requires(cs -> CommandManager
                    .hasPerm(cs, perm));
            command = command.then(Commands.argument("player", GameProfileArgument.gameProfile()).executes(ctx -> Kick
                    .kick(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "player"))));
            commandDispatcher.register(command);
        }
    }

    private static int kick(final CommandSourceStack source, final Collection<GameProfile> collection)
            throws CommandSyntaxException
    {
        int i = 0;
        for (final GameProfile player : collection)
            i += Kick.kick(source, player);
        return i;
    }

    private static int kick(final CommandSourceStack source, final GameProfile player) throws CommandSyntaxException
    {
        final ServerPlayer user = source.getPlayerOrException();
        final LandTeam teamA = LandManager.getTeam(user);
        final LandTeam teamB = LandManager.getTeam(player.getId());
        if (teamA != teamB)
        {
            source.sendFailure(CommandManager.makeFormattedComponent("thutessentials.teams.kick.mustbeteam"));
            return 1;
        }
        if (!teamA.hasRankPerm(user.getUUID(), LandTeam.KICK))
        {
            source.sendFailure(CommandManager.makeFormattedComponent("thutessentials.teams.kick.teamperms"));
            return 1;
        }
        LandManager.getInstance().removeFromTeam(player.getId());
        Essentials.config.sendFeedback(source, "thutessentials.teams.kicked", false, player.getName());
        return 0;
    }
}
