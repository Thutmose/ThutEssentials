package thut.essentials.commands.land.management;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.land.LandEventsHandler;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.util.PermNodes;
import thut.essentials.util.PermNodes.DefaultPermissionLevel;

public class Join
{

    public static void register(final CommandDispatcher<CommandSourceStack> commandDispatcher)
    {
        final SuggestionProvider<CommandSourceStack> suggestor = (ctx, sb) -> SharedSuggestionProvider
                .suggest(Join.getTeams(), sb);

        String name = "join_team";
        if (!Essentials.config.commandBlacklist.contains(name))
        {
            String perm;
            PermNodes.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /" + name);

            LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name)
                    .requires(cs -> CommandManager.hasPerm(cs, perm));
            command = command.then(Commands.argument("team", StringArgumentType.string()).suggests(suggestor)
                    .executes(ctx -> Join.execute(ctx.getSource(), null, StringArgumentType.getString(ctx, "team"))));
            commandDispatcher.register(command);
        }

        name = "leave_team";
        if (!Essentials.config.commandBlacklist.contains(name))
        {
            String perm;
            PermNodes.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /" + name);

            LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name)
                    .requires(cs -> CommandManager.hasPerm(cs, perm));
            command = command
                    .executes(ctx -> Join.execute(ctx.getSource(), null, LandManager.getDefaultTeam().teamName));
            commandDispatcher.register(command);
        }

        name = "add_to_team";
        if (!Essentials.config.commandBlacklist.contains(name))
        {
            String perm;
            PermNodes.registerNode(perm = "command." + name, DefaultPermissionLevel.OP, "Can the player use /" + name);

            LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name)
                    .requires(cs -> CommandManager.hasPerm(cs, perm));
            command = command.then(Commands.argument("team", StringArgumentType.string()).suggests(suggestor).then(
                    Commands.argument("player", EntityArgument.player()).executes(ctx -> Join.execute(ctx.getSource(),
                            EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "team")))));
            commandDispatcher.register(command);
        }
    }

    private static List<String> getTeams()
    {
        final List<String> teams = Lists.newArrayList(LandManager.getInstance()._teamMap.keySet());
        Collections.sort(teams);
        return teams;
    }

    private static int execute(final CommandSourceStack source, ServerPlayer player, final String team)
            throws CommandSyntaxException
    {
        final boolean forced = player != null;
        if (player == null) player = source.getPlayerOrException();

        final LandTeam teamtojoin = LandManager.getInstance().getTeam(team, false);
        final LandTeam oldTeam = LandManager.getTeam(player);
        if (oldTeam == teamtojoin)
        {
            source.sendFailure(CommandManager.makeFormattedComponent("thutessentials.team.join.alreadyin"));
            return 1;
        }
        if (teamtojoin == null)
        {
            source.sendFailure(CommandManager.makeFormattedComponent("thutessentials.team.notfound"));
            return 1;
        }

        boolean canJoinInvite = PermNodes.getBooleanPerm(player, LandEventsHandler.PERMJOINTEAMINVITED);
        canJoinInvite = canJoinInvite && LandManager.getInstance().hasInvite(player.getUUID(), team);
        final boolean canJoinNoInvite = forced
                || PermNodes.getBooleanPerm(player, LandEventsHandler.PERMJOINTEAMNOINVITE);
        canJoinInvite = canJoinInvite || teamtojoin.teamName.equalsIgnoreCase(Essentials.config.defaultTeamName);
        if (canJoinInvite && teamtojoin.member.size() == 0)
            canJoinInvite = !LandManager.getInstance().getTeam(team, false).reserved;
        if (canJoinInvite || canJoinNoInvite)
        {
            LandManager.getInstance().addToTeam(player.getUUID(), team);
            player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.team.joined", null, false,
                    teamtojoin.teamName), Util.NIL_UUID);
            return 0;
        }
        return 1;
    }
}
