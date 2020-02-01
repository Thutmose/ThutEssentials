package thut.essentials.commands.land.util;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;

public class Members
{

    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        // TODO configurable this.
        final String name = "team_members";
        if (Essentials.config.commandBlacklist.contains(name)) return;
        String perm;
        PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL,
                "Can the player see the list members of a team.");

        // Setup with name and permission
        LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs,
                perm));
        // No target argument version
        command = command.then(Commands.argument("team_name", StringArgumentType.string()).executes(ctx -> Members
                .execute(ctx.getSource(), StringArgumentType.getString(ctx, "team_name"))));
    }

    private static int execute(final CommandSource source, final String teamname)
    {
        final LandTeam team = LandManager.getInstance().getTeam(teamname, false);
        if (team == null)
        {
            source.sendErrorMessage(new TranslationTextComponent("thutessentials.team.notfound"));
            return 1;
        }
        source.sendFeedback(new TranslationTextComponent("thutessentials.team.members", teamname), true);
        source.sendFeedback(Members.getMembers(source.getServer(), team, true), true);
        return 0;
    }

    public static ITextComponent getMembers(final MinecraftServer server, final LandTeam team, final boolean tabbed)
    {
        final StringTextComponent mess = new StringTextComponent("");
        final Collection<UUID> c = team.member;
        final List<UUID> ids = Lists.newArrayList(c);
        for (int i = 0; i < ids.size(); i++)
        {
            final UUID o = ids.get(i);
            if (o == null) continue;
            final GameProfile profile = CommandManager.getProfile(server, o);
            if (tabbed) mess.appendText("    ");
            if (profile.getName() != null) mess.appendText(profile.getName());
            else mess.appendText("<unknown> " + o);
            if (i < ids.size() - 1) mess.appendText("\n");
        }
        return mess;
    }
}
