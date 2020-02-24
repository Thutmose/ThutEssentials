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

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.commands.land.util.Members;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.land.LandManager.PlayerRank;
import thut.essentials.land.LandSaveHandler;

public class Ranks
{
    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        final String name = "edit_ranks";
        if (Essentials.config.commandBlacklist.contains(name)) return;

        String perm;
        PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /" + name);

        LiteralArgumentBuilder<CommandSource> base = Commands.literal(name).requires(cs -> Edit.adminUse(cs, perm));
        LiteralArgumentBuilder<CommandSource> command;

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
                                "perm"))))));
        commandDispatcher.register(command);
        base = Commands.literal(name).requires(cs -> Edit.adminUse(cs, perm));
        command = base.then(Commands.literal("rem_rank").then(Commands.argument("rank", StringArgumentType.string())
                .then(Commands.argument("player", EntityArgument.player()).executes(ctx -> Ranks.rem_rank(ctx
                        .getSource(), EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx,
                                "perm"))))));
        commandDispatcher.register(command);

    }

    private static int set_rank(final CommandSource source, final ServerPlayerEntity player, final String rankName)
            throws CommandSyntaxException
    {
        final LandTeam landTeam = LandManager.getTeam(player);
        final PlayerRank rank = landTeam.rankMap.get(rankName);
        if (rank == null)
        {
            Essentials.config.sendError(source, "thutessentials.team.rank.notfound", rankName);
            return 1;
        }
        rank.members.add(player.getUniqueID());
        landTeam._ranksMembers.put(player.getUniqueID(), rank);
        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.team.rank.set", null, false, player
                .getDisplayName(), rankName));
        LandSaveHandler.saveTeam(landTeam.teamName);
        return 0;
    }

    private static int rem_rank(final CommandSource source, final ServerPlayerEntity player, final String rankName)
            throws CommandSyntaxException
    {
        final LandTeam landTeam = LandManager.getTeam(player);
        final PlayerRank rank = landTeam.rankMap.get(rankName);
        if (rank == null)
        {
            Essentials.config.sendError(source, "thutessentials.team.rank.notfound", rankName);
            return 1;
        }
        rank.members.remove(player.getUniqueID());
        landTeam._ranksMembers.remove(player.getUniqueID());
        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.team.rank.rem", null, false, player
                .getDisplayName(), rankName));
        LandSaveHandler.saveTeam(landTeam.teamName);
        return 0;
    }

    private static int list_ranks(final CommandSource source) throws CommandSyntaxException
    {
        final ServerPlayerEntity player = source.asPlayer();
        final LandTeam landTeam = LandManager.getTeam(player);
        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.team.rank.header"));
        final List<String> ranks = Lists.newArrayList(landTeam.rankMap.keySet());
        Collections.sort(ranks);
        for (final String s : ranks)
            player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.team.rank.entry", null, false, s));
        return 0;
    }

    private static int add_perm(final CommandSource source, final String rankName, final String perm)
            throws CommandSyntaxException
    {
        final ServerPlayerEntity player = source.asPlayer();
        final LandTeam landTeam = LandManager.getTeam(player);
        final PlayerRank rank = landTeam.rankMap.get(rankName);
        if (rank == null)
        {
            Essentials.config.sendError(source, "thutessentials.team.rank.notfound", rankName);
            return 1;
        }
        if (rank.perms.add(perm)) player.sendMessage(CommandManager.makeFormattedComponent(
                "thutessentials.team.rank.perm.set", null, false, rankName));
        else player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.team.rank.perm.had", null, false,
                rankName));
        LandSaveHandler.saveTeam(landTeam.teamName);
        return 0;
    }

    private static int del_perm(final CommandSource source, final String rankName, final String perm)
            throws CommandSyntaxException
    {
        final ServerPlayerEntity player = source.asPlayer();
        final LandTeam landTeam = LandManager.getTeam(player);
        final PlayerRank rank = landTeam.rankMap.get(rankName);
        if (rank == null)
        {
            Essentials.config.sendError(source, "thutessentials.team.rank.notfound", rankName);
            return 1;
        }
        if (rank.perms.remove(perm)) player.sendMessage(CommandManager.makeFormattedComponent(
                "thutessentials.team.rank.perm.unset", null, false, rankName));
        else player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.team.rank.perm.nohad", null,
                false, rankName));
        LandSaveHandler.saveTeam(landTeam.teamName);
        return 0;
    }

    private static int add_rank(final CommandSource source, final String rankName) throws CommandSyntaxException
    {
        final ServerPlayerEntity player = source.asPlayer();
        final LandTeam landTeam = LandManager.getTeam(player);
        final PlayerRank rank = landTeam.rankMap.get(rankName);
        if (rank != null)
        {
            Essentials.config.sendError(source, "thutessentials.team.rank.alreadyexists", rankName);
            return 1;
        }
        landTeam.rankMap.put(rankName, new PlayerRank());
        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.team.rank.added", null, false,
                rankName));
        LandSaveHandler.saveTeam(landTeam.teamName);
        return 0;
    }

    private static int del_rank(final CommandSource source, final String rankName) throws CommandSyntaxException
    {
        final ServerPlayerEntity player = source.asPlayer();
        final LandTeam landTeam = LandManager.getTeam(player);
        final PlayerRank rank = landTeam.rankMap.get(rankName);
        if (rank == null)
        {
            Essentials.config.sendError(source, "thutessentials.team.rank.notfound", rankName);
            return 1;
        }
        landTeam.rankMap.remove(rankName);
        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.team.rank.deleted", null, false,
                rankName));
        LandSaveHandler.saveTeam(landTeam.teamName);
        return 0;
    }

    private static int list_members(final CommandSource source, final String rankName) throws CommandSyntaxException
    {
        final ServerPlayerEntity player = source.asPlayer();
        final LandTeam landTeam = LandManager.getTeam(player);
        final PlayerRank rank = landTeam.rankMap.get(rankName);
        if (rank == null)
        {
            Essentials.config.sendError(source, "thutessentials.team.rank.notfound", rankName);
            return 1;
        }
        final Collection<UUID> c = rank.members;
        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.team.rank.memheader", null, false,
                rankName));
        final ITextComponent list = Members.getMembers(source.getServer(), c, false);
        player.sendMessage(list);
        return 0;
    }
}
