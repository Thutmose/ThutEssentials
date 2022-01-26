package thut.essentials.commands.land.management;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.commands.land.util.Members;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.land.LandManager.PlayerRank;
import thut.essentials.land.LandSaveHandler;
import thut.essentials.util.PermNodes;
import thut.essentials.util.PermNodes.DefaultPermissionLevel;

public class Ranks
{
    public static void register(final CommandDispatcher<CommandSourceStack> commandDispatcher)
    {
        final String name = "edit_ranks";
        if (Essentials.config.commandBlacklist.contains(name)) return;

        String perm;
        PermNodes.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /" + name);

        LiteralArgumentBuilder<CommandSourceStack> base = Commands.literal(name).requires(cs -> Edit.adminUse(cs, perm));
        LiteralArgumentBuilder<CommandSourceStack> command;

        command = base.then(Commands.literal("list_ranks").executes(ctx -> Ranks.list_ranks(ctx.getSource())));
        commandDispatcher.register(command);
        base = Commands.literal(name).requires(cs -> Edit.adminUse(cs, perm));
        command = base.then(Commands.literal("list_members").then(Commands.argument("rank", StringArgumentType.string())
                .executes(ctx -> Ranks.list_members(ctx.getSource(), StringArgumentType.getString(ctx, "rank")))));
        commandDispatcher.register(command);
        base = Commands.literal(name).requires(cs -> Edit.adminUse(cs, perm));

        command = base.then(Commands.literal("add_rank").then(Commands.argument("rank", StringArgumentType.string())
                .executes(ctx -> Ranks.add_rank(ctx.getSource(), StringArgumentType.getString(ctx, "rank")))));
        commandDispatcher.register(command);
        base = Commands.literal(name).requires(cs -> Edit.adminUse(cs, perm));
        command = base.then(Commands.literal("del_rank").then(Commands.argument("rank", StringArgumentType.string())
                .executes(ctx -> Ranks.del_rank(ctx.getSource(), StringArgumentType.getString(ctx, "rank")))));
        commandDispatcher.register(command);
        base = Commands.literal(name).requires(cs -> Edit.adminUse(cs, perm));

        command = base.then(Commands.literal("add_perm").then(Commands.argument("rank", StringArgumentType.string())
                .then(Commands.argument("perm", StringArgumentType.string()).executes(ctx -> Ranks.add_perm(ctx
                        .getSource(), StringArgumentType.getString(ctx, "rank"), StringArgumentType.getString(ctx,
                                "perm"))))));
        commandDispatcher.register(command);
        base = Commands.literal(name).requires(cs -> Edit.adminUse(cs, perm));
        command = base.then(Commands.literal("del_perm").then(Commands.argument("rank", StringArgumentType.string())
                .then(Commands.argument("perm", StringArgumentType.string()).executes(ctx -> Ranks.del_perm(ctx
                        .getSource(), StringArgumentType.getString(ctx, "rank"), StringArgumentType.getString(ctx,
                                "perm"))))));
        commandDispatcher.register(command);
        base = Commands.literal(name).requires(cs -> Edit.adminUse(cs, perm));

        command = base.then(Commands.literal("set_rank").then(Commands.argument("rank", StringArgumentType.string())
                .then(Commands.argument("player", EntityArgument.player()).executes(ctx -> Ranks.set_rank(ctx
                        .getSource(), EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx,
                                "rank"))))));
        commandDispatcher.register(command);
        base = Commands.literal(name).requires(cs -> Edit.adminUse(cs, perm));
        command = base.then(Commands.literal("rem_rank").then(Commands.argument("rank", StringArgumentType.string())
                .then(Commands.argument("player", EntityArgument.player()).executes(ctx -> Ranks.rem_rank(ctx
                        .getSource(), EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx,
                                "rank"))))));
        commandDispatcher.register(command);

    }

    private static int set_rank(final CommandSourceStack source, final ServerPlayer player, final String rankName)
            throws CommandSyntaxException
    {
        final LandTeam landTeam = LandManager.getTeam(source.getPlayerOrException());
        final PlayerRank rank = landTeam.rankMap.get(rankName);
        if (rank == null)
        {
            Essentials.config.sendError(source, "thutessentials.team.rank.notfound", rankName);
            return 1;
        }
        rank.members.add(player.getUUID());
        landTeam._ranksMembers.put(player.getUUID(), rank);
        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.team.rank.set", null, false, player
                .getDisplayName(), rankName), Util.NIL_UUID);
        LandSaveHandler.saveTeam(landTeam.teamName);
        return 0;
    }

    private static int rem_rank(final CommandSourceStack source, final ServerPlayer player, final String rankName)
            throws CommandSyntaxException
    {
        final LandTeam landTeam = LandManager.getTeam(source.getPlayerOrException());
        final PlayerRank rank = landTeam.rankMap.get(rankName);
        if (rank == null)
        {
            Essentials.config.sendError(source, "thutessentials.team.rank.notfound", rankName);
            return 1;
        }
        rank.members.remove(player.getUUID());
        landTeam._ranksMembers.remove(player.getUUID());
        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.team.rank.rem", null, false, player
                .getDisplayName(), rankName), Util.NIL_UUID);
        LandSaveHandler.saveTeam(landTeam.teamName);
        return 0;
    }

    private static int list_ranks(final CommandSourceStack source) throws CommandSyntaxException
    {
        final ServerPlayer player = source.getPlayerOrException();
        final LandTeam landTeam = LandManager.getTeam(player);
        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.team.rank.header"), Util.NIL_UUID);
        final List<String> ranks = Lists.newArrayList(landTeam.rankMap.keySet());
        Collections.sort(ranks);
        for (final String s : ranks)
            player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.team.rank.entry", null, false, s),
                    Util.NIL_UUID);
        return 0;
    }

    private static int add_perm(final CommandSourceStack source, final String rankName, final String perm)
            throws CommandSyntaxException
    {
        final ServerPlayer player = source.getPlayerOrException();
        final LandTeam landTeam = LandManager.getTeam(player);
        final PlayerRank rank = landTeam.rankMap.get(rankName);
        if (rank == null)
        {
            Essentials.config.sendError(source, "thutessentials.team.rank.notfound", rankName);
            return 1;
        }
        if (rank.perms.add(perm)) player.sendMessage(CommandManager.makeFormattedComponent(
                "thutessentials.team.rank.perm.set", null, false, rankName, perm), Util.NIL_UUID);
        else player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.team.rank.perm.had", null, false,
                rankName), Util.NIL_UUID);
        LandSaveHandler.saveTeam(landTeam.teamName);
        return 0;
    }

    private static int del_perm(final CommandSourceStack source, final String rankName, final String perm)
            throws CommandSyntaxException
    {
        final ServerPlayer player = source.getPlayerOrException();
        final LandTeam landTeam = LandManager.getTeam(player);
        final PlayerRank rank = landTeam.rankMap.get(rankName);
        if (rank == null)
        {
            Essentials.config.sendError(source, "thutessentials.team.rank.notfound", rankName);
            return 1;
        }
        if (rank.perms.remove(perm)) player.sendMessage(CommandManager.makeFormattedComponent(
                "thutessentials.team.rank.perm.unset", null, false, rankName), Util.NIL_UUID);
        else player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.team.rank.perm.nohad", null,
                false, rankName), Util.NIL_UUID);
        LandSaveHandler.saveTeam(landTeam.teamName);
        return 0;
    }

    private static int add_rank(final CommandSourceStack source, final String rankName) throws CommandSyntaxException
    {
        final ServerPlayer player = source.getPlayerOrException();
        final LandTeam landTeam = LandManager.getTeam(player);
        final PlayerRank rank = landTeam.rankMap.get(rankName);
        if (rank != null)
        {
            Essentials.config.sendError(source, "thutessentials.team.rank.alreadyexists", rankName);
            return 1;
        }
        landTeam.rankMap.put(rankName, new PlayerRank());
        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.team.rank.added", null, false,
                rankName), Util.NIL_UUID);
        LandSaveHandler.saveTeam(landTeam.teamName);
        return 0;
    }

    private static int del_rank(final CommandSourceStack source, final String rankName) throws CommandSyntaxException
    {
        final ServerPlayer player = source.getPlayerOrException();
        final LandTeam landTeam = LandManager.getTeam(player);
        final PlayerRank rank = landTeam.rankMap.get(rankName);
        if (rank == null)
        {
            Essentials.config.sendError(source, "thutessentials.team.rank.notfound", rankName);
            return 1;
        }
        landTeam.rankMap.remove(rankName);
        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.team.rank.deleted", null, false,
                rankName), Util.NIL_UUID);
        LandSaveHandler.saveTeam(landTeam.teamName);
        return 0;
    }

    private static int list_members(final CommandSourceStack source, final String rankName) throws CommandSyntaxException
    {
        final ServerPlayer player = source.getPlayerOrException();
        final LandTeam landTeam = LandManager.getTeam(player);
        final PlayerRank rank = landTeam.rankMap.get(rankName);
        if (rank == null)
        {
            Essentials.config.sendError(source, "thutessentials.team.rank.notfound", rankName);
            return 1;
        }
        final Collection<UUID> c = rank.members;
        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.team.rank.memheader", null, false,
                rankName), Util.NIL_UUID);
        final Component list = Members.getMembers(source.getServer(), c, false);
        player.sendMessage(list, Util.NIL_UUID);
        return 0;
    }
}
