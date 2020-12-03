package thut.essentials.commands.land.management;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.Util;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.land.LandManager.Relation;

public class Relations
{
    public static List<String>        perms     = Lists.newArrayList();
    public static Map<String, String> perm_info = Maps.newHashMap();
    static
    {
        Relations.perms.add(LandTeam.BREAK);
        Relations.perms.add(LandTeam.PLACE);
        Relations.perms.add(LandTeam.PUBLIC);
        Relations.perms.add(LandTeam.ALLY);
        for (final String s : Relations.perms)
            Relations.perm_info.put(s, "thutessentials.team.info." + s);
        Collections.sort(Relations.perms);
    }

    public static SuggestionProvider<CommandSource> suggestTeams()
    {
        return (ctx, sb) ->
        {
            final List<String> values = Lists.newArrayList();
            for (final String s : LandManager.getInstance()._teamMap.keySet())
                values.add(s);
            Collections.sort(values);
            return net.minecraft.command.ISuggestionProvider.suggest(values, sb);
        };
    }

    public static SuggestionProvider<CommandSource> suggestPerms()
    {
        return (ctx, sb) -> net.minecraft.command.ISuggestionProvider.suggest(Relations.perms, sb);
    }

    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        final String name = "team_relations";
        if (Essentials.config.commandBlacklist.contains(name)) return;

        String perm;
        PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /" + name);

        LiteralArgumentBuilder<CommandSource> base = Commands.literal(name).requires(cs -> Edit.adminUse(cs, perm));
        LiteralArgumentBuilder<CommandSource> command;

        command = base.then(Commands.literal("list").executes(ctx -> Relations.list(ctx.getSource())));
        commandDispatcher.register(command);
        base = Commands.literal(name).requires(cs -> Edit.adminUse(cs, perm));

        command = base.then(Commands.literal("relations").then(Commands.argument("team", StringArgumentType.string())
                .suggests(Relations.suggestTeams()).executes(ctx -> Relations.relations(ctx.getSource(),
                        StringArgumentType.getString(ctx, "team")))));
        commandDispatcher.register(command);
        base = Commands.literal(name).requires(cs -> Edit.adminUse(cs, perm));

        command = base.then(Commands.literal("relations_all").executes(ctx -> Relations.relations_all(ctx
                .getSource())));
        commandDispatcher.register(command);
        base = Commands.literal(name).requires(cs -> Edit.adminUse(cs, perm));

        command = base.then(Commands.literal("set").then(Commands.argument("team", StringArgumentType.string())
                .suggests(Relations.suggestTeams()).then(Commands.argument("perm", StringArgumentType.string())
                        .suggests(Relations.suggestPerms()).executes(ctx -> Relations.set(ctx.getSource(),
                                StringArgumentType.getString(ctx, "team"), StringArgumentType.getString(ctx,
                                        "perm"))))));
        commandDispatcher.register(command);
        base = Commands.literal(name).requires(cs -> Edit.adminUse(cs, perm));

        command = base.then(Commands.literal("unset").then(Commands.argument("team", StringArgumentType.string())
                .suggests(Relations.suggestTeams()).then(Commands.argument("perm", StringArgumentType.string())
                        .suggests(Relations.suggestPerms()).executes(ctx -> Relations.unset(ctx.getSource(),
                                StringArgumentType.getString(ctx, "team"), StringArgumentType.getString(ctx,
                                        "perm"))))));
        commandDispatcher.register(command);
    }

    private static int list(final CommandSource source)
    {
        Essentials.config.sendFeedback(source, "thutessentials.team.relations.header", false);
        for (final String s : Relations.perms)
            Essentials.config.sendFeedback(source, Relations.perm_info.get(s), false, s);
        return 0;
    }

    private static int relations_all(final CommandSource source) throws CommandSyntaxException
    {
        final ServerPlayerEntity player = source.asPlayer();
        final LandTeam landTeam = LandManager.getTeam(player);
        final List<String> keys = Lists.newArrayList(landTeam.relations.keySet());
        if (keys.isEmpty()) player.sendMessage(CommandManager.makeFormattedComponent(
                "thutessentials.team.relations.none"), Util.DUMMY_UUID);
        for (final String team : keys)
            Relations.relations(source, team);
        return 0;
    }

    private static int relations(final CommandSource source, final String team) throws CommandSyntaxException
    {
        final ServerPlayerEntity player = source.asPlayer();
        final LandTeam landTeam = LandManager.getTeam(player);
        final Relation relate = landTeam.relations.get(team);
        if (relate == null)
        {
            Essentials.config.sendError(source, "thutessentials.team.relations.notfound", team);
            return 1;
        }
        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.team.relations.with", null, false,
                team), Util.DUMMY_UUID);
        for (final String s : relate.perms)
            player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.team.relations.entry", null, false,
                    s), Util.DUMMY_UUID);
        return 0;
    }

    private static int set(final CommandSource source, final String other, final String perm)
            throws CommandSyntaxException
    {
        final ServerPlayerEntity player = source.asPlayer();
        final LandTeam landTeam = LandManager.getTeam(player);
        if (!Relations.perms.contains(perm))
        {
            Essentials.config.sendError(source, "thutessentials.team.relations.noperm");
            return 1;
        }
        final LandTeam team = LandManager.getInstance().getTeam(other, false);
        if (team == null)
        {
            Essentials.config.sendError(source, "thutessentials.team.notfound");
            return 1;
        }
        Relation relation = landTeam.relations.get(other);
        if (relation == null) landTeam.relations.put(other, relation = new Relation());
        if (relation.perms.add(perm)) player.sendMessage(CommandManager.makeFormattedComponent(
                "thutessentials.team.relations.set", null, false, perm), Util.DUMMY_UUID);
        else player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.team.relations.had", null, false,
                other), Util.DUMMY_UUID);
        return 0;
    }

    private static int unset(final CommandSource source, final String other, final String perm)
            throws CommandSyntaxException
    {
        final ServerPlayerEntity player = source.asPlayer();
        final LandTeam landTeam = LandManager.getTeam(player);
        if (!Relations.perms.contains(perm))
        {
            Essentials.config.sendError(source, "thutessentials.team.relations.noperm");
            return 1;
        }
        final LandTeam team = LandManager.getInstance().getTeam(other, false);
        if (team == null)
        {
            Essentials.config.sendError(source, "thutessentials.team.notfound");
            return 1;
        }
        Relation relation = landTeam.relations.get(other);
        if (relation == null) landTeam.relations.put(other, relation = new Relation());
        if (relation.perms.remove(perm)) player.sendMessage(CommandManager.makeFormattedComponent(
                "thutessentials.team.relations.unset", null, false, perm, other), Util.DUMMY_UUID);
        else player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.team.relations.nohad", null,
                false, other), Util.DUMMY_UUID);
        return 0;
    }

}
