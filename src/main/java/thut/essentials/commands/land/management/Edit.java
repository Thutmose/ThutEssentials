package thut.essentials.commands.land.management;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.land.LandSaveHandler;
import thut.essentials.util.CoordinateUtls;
import thut.essentials.util.PermNodes;
import thut.essentials.util.PermNodes.DefaultPermissionLevel;
import thut.essentials.util.RuleManager;

public class Edit
{

    private static final String PERMRESERVELAND        = "thutessentials.land.toggle.reserve";
    private static final String PERMTOGGLEMOBS         = "thutessentials.land.toggle.mobspawn";
    private static final String PERMTOGGLEEXPLODE      = "thutessentials.land.toggle.explode";
    private static final String PERMTOGGLEFF           = "thutessentials.land.toggle.friendlyfire";
    private static final String PERMTOGGLEPLAYERDAMAGE = "thutessentials.land.toggle.playerdamage";
    private static final String PERMTOGGLENPCDAMAGE    = "thutessentials.land.toggle.npcdamage";
    private static final String PERMTOGGLEFAKEPLAYERS  = "thutessentials.land.toggle.fakeplayers";

    public static boolean adminUse(final CommandSourceStack source, final String perm)
    {
        if (perm != null && !CommandManager.hasPerm(source, perm)) return false;
        try
        {
            final ServerPlayer player = source.getPlayerOrException();
            final LandTeam landTeam = LandManager.getTeam(player);
            return landTeam.isAdmin(player);
        }
        catch (final CommandSyntaxException e)
        {
            return false;
        }

    }

    public static boolean permUse(final CommandSourceStack source, final String perm)
    {
        try
        {
            final ServerPlayer player = source.getPlayerOrException();
            final LandTeam landTeam = LandManager.getTeam(player);
            return landTeam.hasRankPerm(player.getUUID(), perm);
        }
        catch (final CommandSyntaxException e)
        {
            return false;
        }

    }

    public static void register(final CommandDispatcher<CommandSourceStack> commandDispatcher)
    {
        PermNodes.registerNode(Edit.PERMTOGGLEEXPLODE, DefaultPermissionLevel.OP,
                "Allowed to toggle explosions on/off in their team land");
        PermNodes.registerNode(Edit.PERMTOGGLEMOBS, DefaultPermissionLevel.OP,
                "Allowed to toggle mob spawns on/off in their team land");
        PermNodes.registerNode(Edit.PERMRESERVELAND, DefaultPermissionLevel.OP,
                "Allowed to toggle reserved status on/off for their team");
        PermNodes.registerNode(Edit.PERMTOGGLEFF, DefaultPermissionLevel.OP,
                "Allowed to toggle friendly fire on/off for their team");
        PermNodes.registerNode(Edit.PERMTOGGLEPLAYERDAMAGE, DefaultPermissionLevel.OP,
                "Allowed to toggle player damage on/off in their team land");
        PermNodes.registerNode(Edit.PERMTOGGLENPCDAMAGE, DefaultPermissionLevel.ALL,
                "Allowed to toggle npc damage on/off in their team land");
        PermNodes.registerNode(Edit.PERMTOGGLEFAKEPLAYERS, DefaultPermissionLevel.ALL,
                "Allowed to toggle whether fakeplayers are ignored for land stuff.");

        final String name = "edit_team";
        if (Essentials.config.commandBlacklist.contains(name)) return;

        String perm;
        PermNodes.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /" + name);

        final LiteralArgumentBuilder<CommandSourceStack> base = Commands.literal(name).requires(cs -> CommandManager.hasPerm(
                cs, perm));
        LiteralArgumentBuilder<CommandSourceStack> command;

        command = base.then(Commands.literal("public").requires(cs -> Edit.adminUse(cs, null)).executes(ctx -> Edit
                .toggle_public(ctx.getSource())));
        commandDispatcher.register(command);

        command = base.then(Commands.literal("any_place").requires(cs -> Edit.adminUse(cs, null)).executes(ctx -> Edit
                .toggle_any_place(ctx.getSource())));
        commandDispatcher.register(command);

        command = base.then(Commands.literal("frames").requires(cs -> Edit.adminUse(cs, null)).executes(ctx -> Edit
                .toggle_frames(ctx.getSource())));
        commandDispatcher.register(command);

        command = base.then(Commands.literal("fake_players").requires(cs -> Edit.adminUse(cs,
                Edit.PERMTOGGLEFAKEPLAYERS)).executes(ctx -> Edit.toggle_fake_players(ctx.getSource())));
        commandDispatcher.register(command);

        command = base.then(Commands.literal("any_break").requires(cs -> Edit.adminUse(cs, null)).executes(ctx -> Edit
                .toggle_any_break(ctx.getSource())));
        commandDispatcher.register(command);

        command = base.then(Commands.literal("reserve").requires(cs -> Edit.adminUse(cs, Edit.PERMRESERVELAND))
                .executes(ctx -> Edit.toggle_reserve(ctx.getSource())));
        commandDispatcher.register(command);

        command = base.then(Commands.literal("no_player_damage").requires(cs -> Edit.adminUse(cs,
                Edit.PERMTOGGLEPLAYERDAMAGE)).executes(ctx -> Edit.toggle_no_player_damage(ctx.getSource())));
        commandDispatcher.register(command);

        command = base.then(Commands.literal("no_npc_damage").requires(cs -> Edit.adminUse(cs,
                Edit.PERMTOGGLENPCDAMAGE)).executes(ctx -> Edit.toggle_no_npc_damage(ctx.getSource())));
        commandDispatcher.register(command);

        command = base.then(Commands.literal("friendly_fire").requires(cs -> Edit.adminUse(cs, Edit.PERMTOGGLEFF))
                .executes(ctx -> Edit.toggle_ff(ctx.getSource())));
        commandDispatcher.register(command);

        command = base.then(Commands.literal("no_mob_spawn").requires(cs -> Edit.adminUse(cs, Edit.PERMTOGGLEMOBS))
                .executes(ctx -> Edit.toggle_no_mob_spawn(ctx.getSource())));
        commandDispatcher.register(command);

        command = base.then(Commands.literal("no_explosions").requires(cs -> Edit.adminUse(cs, Edit.PERMTOGGLEEXPLODE))
                .executes(ctx -> Edit.toggle_no_booms(ctx.getSource())));
        commandDispatcher.register(command);

        command = base.then(Commands.literal("prefix").requires(cs -> Edit.permUse(cs, LandTeam.SETPREFIX)).then(
                Commands.argument("words", StringArgumentType.greedyString()).executes(ctx -> Edit.set_prefix(ctx
                        .getSource(), StringArgumentType.getString(ctx, "words")))));
        commandDispatcher.register(command);

        command = base.then(Commands.literal("enter").requires(cs -> Edit.permUse(cs, LandTeam.EDITMESSAGES)).then(
                Commands.argument("words", StringArgumentType.greedyString()).executes(ctx -> Edit.set_enter(ctx
                        .getSource(), StringArgumentType.getString(ctx, "words")))));
        commandDispatcher.register(command);

        command = base.then(Commands.literal("exit").requires(cs -> Edit.permUse(cs, LandTeam.EDITMESSAGES)).then(
                Commands.argument("words", StringArgumentType.greedyString()).executes(ctx -> Edit.set_exit(ctx
                        .getSource(), StringArgumentType.getString(ctx, "words")))));
        commandDispatcher.register(command);

        command = base.then(Commands.literal("deny").requires(cs -> Edit.permUse(cs, LandTeam.EDITMESSAGES)).then(
                Commands.argument("words", StringArgumentType.greedyString()).executes(ctx -> Edit.set_deny(ctx
                        .getSource(), StringArgumentType.getString(ctx, "words")))));
        commandDispatcher.register(command);

        command = base.then(Commands.literal("home").requires(cs -> Edit.permUse(cs, LandTeam.SETHOME)).executes(
                ctx -> Edit.set_home(ctx.getSource())));
        commandDispatcher.register(command);
    }

    private static int set_prefix(final CommandSourceStack source, String prefix) throws CommandSyntaxException
    {
        prefix = RuleManager.format(prefix);
        final ServerPlayer player = source.getPlayerOrException();
        final LandTeam landTeam = LandManager.getTeam(player);
        if (prefix.length() > Essentials.config.prefixLength) prefix = prefix.substring(0,
                Essentials.config.prefixLength);
        landTeam.prefix = prefix;
        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.team.prefix.set", null, false, prefix),
                Util.NIL_UUID);
        Edit.refreshTeam(landTeam, source.getServer());
        LandSaveHandler.saveTeam(landTeam.teamName);
        return 0;
    }

    private static int set_enter(final CommandSourceStack source, String message) throws CommandSyntaxException
    {
        message = RuleManager.format(message);
        final ServerPlayer player = source.getPlayerOrException();
        final LandTeam landTeam = LandManager.getTeam(player);
        landTeam.enterMessage = message;
        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.team.enter.set", null, false, message),
                Util.NIL_UUID);
        LandSaveHandler.saveTeam(landTeam.teamName);
        return 0;
    }

    private static int set_exit(final CommandSourceStack source, String message) throws CommandSyntaxException
    {
        message = RuleManager.format(message);
        final ServerPlayer player = source.getPlayerOrException();
        final LandTeam landTeam = LandManager.getTeam(player);
        landTeam.exitMessage = message;
        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.team.exit.set", null, false, message),
                Util.NIL_UUID);
        LandSaveHandler.saveTeam(landTeam.teamName);
        return 0;
    }

    private static int set_deny(final CommandSourceStack source, String message) throws CommandSyntaxException
    {
        message = RuleManager.format(message);
        final ServerPlayer player = source.getPlayerOrException();
        final LandTeam landTeam = LandManager.getTeam(player);
        landTeam.denyMessage = message;
        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.team.deny.set", null, false, message),
                Util.NIL_UUID);
        LandSaveHandler.saveTeam(landTeam.teamName);
        return 0;
    }

    private static int set_home(final CommandSourceStack source) throws CommandSyntaxException
    {
        final ServerPlayer player = source.getPlayerOrException();
        final LandTeam landTeam = LandManager.getTeam(player);
        landTeam.team_home = CoordinateUtls.forMob(player);
        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.team.home.set", null, false,
                landTeam.team_home), Util.NIL_UUID);
        LandSaveHandler.saveTeam(landTeam.teamName);
        return 0;
    }

    private static int toggle_frames(final CommandSourceStack source) throws CommandSyntaxException
    {
        final ServerPlayer player = source.getPlayerOrException();
        final LandTeam landTeam = LandManager.getTeam(player);
        landTeam.protectFrames = !landTeam.protectFrames;
        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.team.frames.set", null, false,
                landTeam.protectFrames), Util.NIL_UUID);
        LandSaveHandler.saveTeam(landTeam.teamName);
        return 0;
    }

    private static int toggle_reserve(final CommandSourceStack source) throws CommandSyntaxException
    {
        final ServerPlayer player = source.getPlayerOrException();
        final LandTeam landTeam = LandManager.getTeam(player);
        landTeam.reserved = !landTeam.reserved;
        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.team.reserve.set", null, false,
                landTeam.reserved), Util.NIL_UUID);
        LandSaveHandler.saveTeam(landTeam.teamName);
        return 0;
    }

    private static int toggle_ff(final CommandSourceStack source) throws CommandSyntaxException
    {
        final ServerPlayer player = source.getPlayerOrException();
        final LandTeam landTeam = LandManager.getTeam(player);
        landTeam.friendlyFire = !landTeam.friendlyFire;
        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.team.ff.set", null, false,
                landTeam.friendlyFire), Util.NIL_UUID);
        LandSaveHandler.saveTeam(landTeam.teamName);
        return 0;
    }

    private static int toggle_no_player_damage(final CommandSourceStack source) throws CommandSyntaxException
    {
        final ServerPlayer player = source.getPlayerOrException();
        final LandTeam landTeam = LandManager.getTeam(player);
        landTeam.noPlayerDamage = !landTeam.noPlayerDamage;
        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.team.noplayerdamage.set", null, false,
                landTeam.noPlayerDamage), Util.NIL_UUID);
        LandSaveHandler.saveTeam(landTeam.teamName);
        return 0;
    }

    private static int toggle_no_npc_damage(final CommandSourceStack source) throws CommandSyntaxException
    {
        final ServerPlayer player = source.getPlayerOrException();
        final LandTeam landTeam = LandManager.getTeam(player);
        landTeam.noNPCDamage = !landTeam.noNPCDamage;
        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.team.nonpcdamage.set", null, false,
                landTeam.noNPCDamage), Util.NIL_UUID);
        LandSaveHandler.saveTeam(landTeam.teamName);
        return 0;
    }

    private static int toggle_no_mob_spawn(final CommandSourceStack source) throws CommandSyntaxException
    {
        final ServerPlayer player = source.getPlayerOrException();
        final LandTeam landTeam = LandManager.getTeam(player);
        landTeam.noMobSpawn = !landTeam.noMobSpawn;
        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.team.nomobspawn.set", null, false,
                landTeam.noMobSpawn), Util.NIL_UUID);
        LandSaveHandler.saveTeam(landTeam.teamName);
        return 0;
    }

    private static int toggle_no_booms(final CommandSourceStack source) throws CommandSyntaxException
    {
        final ServerPlayer player = source.getPlayerOrException();
        final LandTeam landTeam = LandManager.getTeam(player);
        landTeam.noExplosions = !landTeam.noExplosions;
        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.team.nobooms.set", null, false,
                landTeam.noExplosions), Util.NIL_UUID);
        LandSaveHandler.saveTeam(landTeam.teamName);
        return 0;
    }

    private static int toggle_any_break(final CommandSourceStack source) throws CommandSyntaxException
    {
        final ServerPlayer player = source.getPlayerOrException();
        final LandTeam landTeam = LandManager.getTeam(player);
        landTeam.anyBreak = !landTeam.anyBreak;
        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.team.anybreak.set", null, false,
                landTeam.anyBreak), Util.NIL_UUID);
        LandSaveHandler.saveTeam(landTeam.teamName);
        return 0;
    }

    private static int toggle_fake_players(final CommandSourceStack source) throws CommandSyntaxException
    {
        final ServerPlayer player = source.getPlayerOrException();
        final LandTeam landTeam = LandManager.getTeam(player);
        landTeam.fakePlayers = !landTeam.fakePlayers;
        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.team.fakeplayers.set", null, false,
                landTeam.fakePlayers), Util.NIL_UUID);
        LandSaveHandler.saveTeam(landTeam.teamName);
        return 0;
    }

    private static int toggle_any_place(final CommandSourceStack source) throws CommandSyntaxException
    {
        final ServerPlayer player = source.getPlayerOrException();
        final LandTeam landTeam = LandManager.getTeam(player);
        landTeam.anyPlace = !landTeam.anyPlace;
        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.team.anyplace.set", null, false,
                landTeam.anyPlace), Util.NIL_UUID);
        LandSaveHandler.saveTeam(landTeam.teamName);
        return 0;
    }

    private static int toggle_public(final CommandSourceStack source) throws CommandSyntaxException
    {
        final ServerPlayer player = source.getPlayerOrException();
        final LandTeam landTeam = LandManager.getTeam(player);
        landTeam.allPublic = !landTeam.allPublic;
        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.team.public.set", null, false,
                landTeam.allPublic), Util.NIL_UUID);
        LandSaveHandler.saveTeam(landTeam.teamName);
        return 0;
    }

    private static void refreshTeam(final LandTeam landTeam, final MinecraftServer server)
    {
        // TODO prefixes for teams!
    }
}
