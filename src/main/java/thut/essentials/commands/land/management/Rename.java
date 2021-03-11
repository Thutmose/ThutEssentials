package thut.essentials.commands.land.management;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.land.LandEventsHandler;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;

public class Rename
{

    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        // TODO configurable this.
        final String name = "rename_team";
        if (Essentials.config.commandBlacklist.contains(name)) return;
        final String perm = LandEventsHandler.PERMCREATETEAM;

        // Setup with name and permission
        LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> Edit.adminUse(cs, perm));

        // Set up the command's arguments
        command = command.then(Commands.argument("team_name", StringArgumentType.string()).executes(ctx -> Rename
                .execute(ctx.getSource(), StringArgumentType.getString(ctx, "team_name"))));

        // Actually register the command.
        commandDispatcher.register(command);
    }

    private static int execute(final CommandSource source, final String teamname) throws CommandSyntaxException
    {
        final ServerPlayerEntity player = source.getPlayerOrException();
        final LandTeam team = LandManager.getTeam(player);
        if (team == null || team == LandManager.getDefaultTeam())
        {
            source.sendFailure(CommandManager.makeFormattedComponent("thutessentials.team.notinateam"));
            return 1;
        }
        final String oldname = team.teamName;
        // This was run from server to just generate the team.
        try
        {
            LandManager.getInstance().renameTeam(oldname, teamname);
        }
        catch (final IllegalArgumentException e)
        {
            Essentials.config.sendError(source, e.getMessage(), oldname);
            return 1;
        }
        Essentials.config.sendFeedback(source, "thutessentials.team.renamed", false, oldname, teamname);
        return 0;
    }
}
