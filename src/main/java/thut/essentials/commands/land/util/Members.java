package thut.essentials.commands.land.util;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.util.PermNodes;
import thut.essentials.util.PermNodes.DefaultPermissionLevel;

public class Members
{

    public static void register(final CommandDispatcher<CommandSourceStack> commandDispatcher)
    {
        // TODO configurable this.
        final String name = "team_members";
        if (Essentials.config.commandBlacklist.contains(name)) return;
        String perm;
        PermNodes.registerBooleanNode(perm = "command." + name, DefaultPermissionLevel.ALL,
                "Can the player see the list members of a team.");

        // Setup with name and permission
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs,
                perm));

        command = command.then(Commands.argument("team_name", StringArgumentType.string()).executes(ctx -> Members
                .execute(ctx.getSource(), StringArgumentType.getString(ctx, "team_name"))));

        // Actually register the command.
        commandDispatcher.register(command);
    }

    private static int execute(final CommandSourceStack source, final String teamname)
    {
        final LandTeam team = LandManager.getInstance().getTeam(teamname, false);
        if (team == null)
        {
            Essentials.config.sendError(source, "thutessentials.team.notfound");
            return 1;
        }
        Essentials.config.sendFeedback(source, "thutessentials.team.members", false, teamname);
        source.sendSuccess(Members.getMembers(source.getServer(), team, true), false);
        return 0;
    }

    public static MutableComponent getMembers(final MinecraftServer server, final LandTeam team,
            final boolean tabbed)
    {
        final Collection<UUID> c = team.member;
        return Members.getMembers(server, c, tabbed);
    }

    public static MutableComponent getMembers(final MinecraftServer server, final Collection<UUID> c,
            final boolean tabbed)
    {
        final MutableComponent mess = Component.literal("");
        final List<UUID> ids = Lists.newArrayList(c);
        for (int i = 0; i < ids.size(); i++)
        {
            final UUID o = ids.get(i);
            if (o == null) continue;
            final GameProfile profile = CommandManager.getProfile(server, o);
            if (tabbed) mess.append("    ");
            if (profile.getName() != null) mess.append(profile.getName());
            else mess.append("<unknown> " + o);
            if (i < ids.size() - 1) mess.append("\n");
        }
        return mess;
    }
}
