package thut.essentials.commands.land.management;

import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
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
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;

public class Invite
{

    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        String name = "team_invites";
        if (!Essentials.config.commandBlacklist.contains(name))
        {
            String perm;
            PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /"
                    + name);

            LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> CommandManager
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

            LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> CommandManager
                    .hasPerm(cs, perm));
            command = command.then(Commands.argument("player", EntityArgument.player()).executes(ctx -> Invite
                    .execute_invite(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"))));
            commandDispatcher.register(command);
        }
    }

    private static int execute_invites(final CommandSource source) throws CommandSyntaxException
    {
        final ServerPlayerEntity player = source.asPlayer();
        final List<String> c = LandManager.getInstance().getInvites(player.getUniqueID());
        if (c.isEmpty())
        {
            player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.team.invite.none"));
            return 1;
        }
        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.team.invites"));
        final String cmd = "join_team";
        for (final String element : c)
        {
            final String command = "/" + cmd + " " + element;
            final ITextComponent message = CommandManager.makeFormattedCommandLink("thutessentials.team.invite.link",
                    command, null, false, c);
            player.sendMessage(message);
        }
        return 0;
    }

    private static int execute_invite(final CommandSource source, final ServerPlayerEntity invitee)
            throws CommandSyntaxException
    {
        final ServerPlayerEntity inviter = source.asPlayer();

        if (inviter == invitee)
        {
            source.sendErrorMessage(CommandManager.makeFormattedComponent("thutessentials.team.invite.noself"));
            return 1;
        }
        final LandTeam landTeam = LandManager.getTeam(inviter);
        final LandTeam oldTeam = LandManager.getTeam(invitee);
        if (landTeam == oldTeam)
        {
            source.sendErrorMessage(CommandManager.makeFormattedComponent("thutessentials.team.invite.alreadyin", null,
                    false, invitee.getDisplayName().getFormattedText()));
            return 1;
        }
        if (!landTeam.hasRankPerm(inviter.getUniqueID(), LandTeam.INVITE))
        {
            source.sendErrorMessage(CommandManager.makeFormattedComponent("thutessentials.team.invite.noteamperms"));
            return 1;
        }
        final String team = landTeam.teamName;
        if (LandManager.getInstance().hasInvite(invitee.getUniqueID(), team))
        {
            source.sendErrorMessage(CommandManager.makeFormattedComponent("thutessentials.team.invite.alreadyinvited",
                    null, false, invitee.getDisplayName().getFormattedText()));
            return 1;
        }
        final boolean invite = LandManager.getInstance().invite(inviter.getUniqueID(), invitee.getUniqueID());
        if (!invite)
        {
            source.sendErrorMessage(CommandManager.makeFormattedComponent("thutessentials.team.invite.failed"));
            return 1;
        }

        final String cmd = "join_team";
        final String command = "/" + cmd + " " + team;
        final ITextComponent header = CommandManager.makeFormattedComponent(
                "thutessentials.team.invite.invited_recieve", null, false, landTeam.teamName, inviter.getDisplayName());
        final ITextComponent message = CommandManager.makeFormattedCommandLink("thutessentials.team.invite.link",
                command, null, false, landTeam.teamName);
        invitee.sendMessage(header);
        invitee.sendMessage(message);
        inviter.sendMessage(CommandManager.makeFormattedComponent("thutessentials.team.invite.invited_sent", null,
                false, invitee.getDisplayName().getFormattedText()));

        return 0;
    }
}
