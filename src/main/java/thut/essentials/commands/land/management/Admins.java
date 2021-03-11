package thut.essentials.commands.land.management;

import java.util.Collection;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.GameProfileArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.Util;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;

public class Admins
{
    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        String name = "list_team_admins";
        if (!Essentials.config.commandBlacklist.contains(name))
        {
            String perm;
            PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /"
                    + name);

            LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> CommandManager
                    .hasPerm(cs, perm));
            command = command.executes(ctx -> Admins.list(ctx.getSource()));
            commandDispatcher.register(command);
        }

        name = "remove_team_admin";
        if (!Essentials.config.commandBlacklist.contains(name))
        {
            String perm;
            PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /"
                    + name);

            LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> Edit.adminUse(cs,
                    perm));
            command = command.then(Commands.argument("player", GameProfileArgument.gameProfile()).executes(ctx -> Admins
                    .remove(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "player"))));
            commandDispatcher.register(command);
        }

        name = "add_team_admin";
        if (!Essentials.config.commandBlacklist.contains(name))
        {
            String perm;
            PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /"
                    + name);

            LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> Edit.adminUse(cs,
                    perm));
            command = command.then(Commands.argument("player", GameProfileArgument.gameProfile()).executes(ctx -> Admins
                    .add(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "player"))));
            commandDispatcher.register(command);
        }
    }

    private static int list(final CommandSource source) throws CommandSyntaxException
    {
        final ServerPlayerEntity player = source.getPlayerOrException();
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

    private static int remove(final CommandSource source, final GameProfile player) throws CommandSyntaxException
    {
        final ServerPlayerEntity user = source.getPlayerOrException();
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

    private static int remove(final CommandSource source, final Collection<GameProfile> collection)
            throws CommandSyntaxException
    {
        int i = 0;
        for (final GameProfile player : collection)
            i += Admins.remove(source, player);
        return i;
    }

    private static int add(final CommandSource source, final Collection<GameProfile> collection)
            throws CommandSyntaxException
    {
        int i = 0;
        for (final GameProfile player : collection)
            i += Admins.add(source, player);
        return i;
    }

    private static int add(final CommandSource source, final GameProfile player) throws CommandSyntaxException
    {
        final ServerPlayerEntity user = source.getPlayerOrException();
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
