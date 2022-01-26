package thut.essentials.commands.land.management;

import java.util.Collection;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.Util;
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

public class Admins
{
    public static void register(final CommandDispatcher<CommandSourceStack> commandDispatcher)
    {
        String name = "list_team_admins";
        if (!Essentials.config.commandBlacklist.contains(name))
        {
            String perm;
            PermNodes.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /"
                    + name);

            LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name).requires(cs -> CommandManager
                    .hasPerm(cs, perm));
            command = command.executes(ctx -> Admins.list(ctx.getSource()));
            commandDispatcher.register(command);
        }

        name = "remove_team_admin";
        if (!Essentials.config.commandBlacklist.contains(name))
        {
            String perm;
            PermNodes.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /"
                    + name);

            LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name).requires(cs -> Edit.adminUse(cs,
                    perm));
            command = command.then(Commands.argument("player", GameProfileArgument.gameProfile()).executes(ctx -> Admins
                    .remove(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "player"))));
            commandDispatcher.register(command);
        }

        name = "add_team_admin";
        if (!Essentials.config.commandBlacklist.contains(name))
        {
            String perm;
            PermNodes.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /"
                    + name);

            LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name).requires(cs -> Edit.adminUse(cs,
                    perm));
            command = command.then(Commands.argument("player", GameProfileArgument.gameProfile()).executes(ctx -> Admins
                    .add(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "player"))));
            commandDispatcher.register(command);
        }
    }

    private static int list(final CommandSourceStack source) throws CommandSyntaxException
    {
        final ServerPlayer player = source.getPlayerOrException();
        final LandTeam team = LandManager.getTeam(player);
        final String teamName = team.teamName;
        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.team.admins.header", null, false,
                teamName), Util.NIL_UUID);
        final Collection<UUID> c = team.admin;
        for (final UUID o : c)
        {
            final GameProfile profile = CommandManager.getProfile(source.getServer(), o);
            player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.team.admins.entry", null, false,
                    profile.getName()), Util.NIL_UUID);
        }
        return 0;
    }

    private static int remove(final CommandSourceStack source, final GameProfile player) throws CommandSyntaxException
    {
        final ServerPlayer user = source.getPlayerOrException();
        final LandTeam teamA = LandManager.getTeam(user);
        final LandTeam teamB = LandManager.getTeam(player.getId());
        if (teamA != teamB)
        {
            source.sendFailure(CommandManager.makeFormattedComponent("thutessentials.team.admins.mustbeteam"));
            return 1;
        }
        final String teamName = teamA.teamName;
        LandManager.getInstance().removeAdmin(player.getId(), teamName);
        source.sendSuccess(CommandManager.makeFormattedComponent("thutessentials.team.admin.removed", null, false,
                player.getName(), teamName), false);
        return 0;
    }

    private static int remove(final CommandSourceStack source, final Collection<GameProfile> collection)
            throws CommandSyntaxException
    {
        int i = 0;
        for (final GameProfile player : collection)
            i += Admins.remove(source, player);
        return i;
    }

    private static int add(final CommandSourceStack source, final Collection<GameProfile> collection)
            throws CommandSyntaxException
    {
        int i = 0;
        for (final GameProfile player : collection)
            i += Admins.add(source, player);
        return i;
    }

    private static int add(final CommandSourceStack source, final GameProfile player) throws CommandSyntaxException
    {
        final ServerPlayer user = source.getPlayerOrException();
        final LandTeam teamA = LandManager.getTeam(user);
        final LandTeam teamB = LandManager.getTeam(player.getId());
        if (teamA != teamB)
        {
            source.sendFailure(CommandManager.makeFormattedComponent("thutessentials.team.admins.mustbeteam"));
            return 1;
        }
        final String teamName = teamA.teamName;
        LandManager.getInstance().addAdmin(player.getId(), teamName);
        source.sendSuccess(CommandManager.makeFormattedComponent("thutessentials.team.admin.added", null, false, player
                .getName(), teamName), false);
        return 0;

    }

}
