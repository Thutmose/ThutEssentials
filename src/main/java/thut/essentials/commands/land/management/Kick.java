package thut.essentials.commands.land.management;

import java.util.Collection;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.GameProfileArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;

public class Kick
{

    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        final String name = "kick_team_member";
        if (!Essentials.config.commandBlacklist.contains(name))
        {
            String perm;
            PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /"
                    + name);

            LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> CommandManager
                    .hasPerm(cs, perm));
            command = command.then(Commands.argument("player", GameProfileArgument.gameProfile()).executes(ctx -> Kick
                    .kick(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "player"))));
            commandDispatcher.register(command);
        }
    }

    private static int kick(final CommandSource source, final Collection<GameProfile> collection)
            throws CommandSyntaxException
    {
        int i = 0;
        for (final GameProfile player : collection)
            i += Kick.kick(source, player);
        return i;
    }

    private static int kick(final CommandSource source, final GameProfile player) throws CommandSyntaxException
    {
        final ServerPlayerEntity user = source.asPlayer();
        final LandTeam teamA = LandManager.getTeam(user);
        final LandTeam teamB = LandManager.getTeam(player.getId());
        if (teamA != teamB)
        {
            source.sendErrorMessage(CommandManager.makeFormattedComponent("thutessentials.teams.kick.mustbeteam"));
            return 1;
        }
        if (!teamA.hasRankPerm(user.getUniqueID(), LandTeam.KICK))
        {
            source.sendErrorMessage(CommandManager.makeFormattedComponent("thutessentials.teams.kick.teamperms"));
            return 1;
        }
        LandManager.getInstance().removeFromTeam(player.getId());
        Essentials.config.sendFeedback(source, "thutessentials.teams.kicked", false, player.getName());
        return 0;
    }
}
