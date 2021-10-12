package thut.essentials.commands.land.management;

import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;

public class Invite
{

    public static void register(final CommandDispatcher<CommandSourceStack> commandDispatcher)
    {
        String name = "team_invites";
        if (!Essentials.config.commandBlacklist.contains(name))
        {
            String perm;
            PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /"
                    + name);

            LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name).requires(cs -> CommandManager
                    .hasPerm(cs, perm));
            command = command.executes(ctx -> Invite.execute_invites(ctx.getSource()));
            commandDispatcher.register(command);
        }

        name = "team_invite";
        if (!Essentials.config.commandBlacklist.contains(name))
        {
            String perm;
            PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /"
                    + name);

            LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name).requires(cs -> CommandManager
                    .hasPerm(cs, perm));
            command = command.then(Commands.argument("player", EntityArgument.player()).executes(ctx -> Invite
                    .execute_invite(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"))));
            commandDispatcher.register(command);
        }
    }

    private static int execute_invites(final CommandSourceStack source) throws CommandSyntaxException
    {
        final ServerPlayer player = source.getPlayerOrException();
        final List<String> c = LandManager.getInstance().getInvites(player.getUUID());
        if (c.isEmpty())
        {
            player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.team.invite.none"),
                    Util.NIL_UUID);
            return 1;
        }
        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.team.invites"), Util.NIL_UUID);
        final String cmd = "join_team";
        for (final String element : c)
        {
            final String command = "/" + cmd + " " + element;
            final Component message = CommandManager.makeFormattedCommandLink("thutessentials.team.invite.link",
                    command, null, false, c);
            player.sendMessage(message, Util.NIL_UUID);
        }
        return 0;
    }

    private static int execute_invite(final CommandSourceStack source, final ServerPlayer invitee)
            throws CommandSyntaxException
    {
        final ServerPlayer inviter = source.getPlayerOrException();

        if (inviter == invitee)
        {
            source.sendFailure(CommandManager.makeFormattedComponent("thutessentials.team.invite.noself"));
            return 1;
        }
        final LandTeam landTeam = LandManager.getTeam(inviter);
        final LandTeam oldTeam = LandManager.getTeam(invitee);
        if (landTeam == oldTeam)
        {
            source.sendFailure(CommandManager.makeFormattedComponent("thutessentials.team.invite.alreadyin", null,
                    false, invitee.getDisplayName().getString()));
            return 1;
        }
        if (!landTeam.hasRankPerm(inviter.getUUID(), LandTeam.INVITE))
        {
            source.sendFailure(CommandManager.makeFormattedComponent("thutessentials.team.invite.noteamperms"));
            return 1;
        }
        final String team = landTeam.teamName;
        if (LandManager.getInstance().hasInvite(invitee.getUUID(), team))
        {
            source.sendFailure(CommandManager.makeFormattedComponent("thutessentials.team.invite.alreadyinvited",
                    null, false, invitee.getDisplayName().getString()));
            return 1;
        }
        final boolean invite = LandManager.getInstance().invite(inviter.getUUID(), invitee.getUUID());
        if (!invite)
        {
            source.sendFailure(CommandManager.makeFormattedComponent("thutessentials.team.invite.failed"));
            return 1;
        }

        final String cmd = "join_team";
        final String command = "/" + cmd + " " + team;
        final Component header = CommandManager.makeFormattedComponent(
                "thutessentials.team.invite.invited_recieve", null, false, landTeam.teamName, inviter.getDisplayName());
        final Component message = CommandManager.makeFormattedCommandLink("thutessentials.team.invite.link",
                command, null, false, landTeam.teamName);
        invitee.sendMessage(header, Util.NIL_UUID);
        invitee.sendMessage(message, Util.NIL_UUID);
        inviter.sendMessage(CommandManager.makeFormattedComponent("thutessentials.team.invite.invited_sent", null,
                false, invitee.getDisplayName().getString()), Util.NIL_UUID);

        return 0;
    }
}
