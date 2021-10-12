package thut.essentials.commands.land.management;

import java.util.UUID;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.land.LandEventsHandler;
import thut.essentials.land.LandManager;

public class Create
{

    public static void register(final CommandDispatcher<CommandSourceStack> commandDispatcher)
    {
        // TODO configurable this.
        final String name = "create_team";
        if (Essentials.config.commandBlacklist.contains(name)) return;
        final String perm = LandEventsHandler.PERMCREATETEAM;

        // Setup with name and permission
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs,
                perm));

        // Set up the command's arguments
        command = command.then(Commands.argument("team_name", StringArgumentType.string()).executes(ctx -> Create
                .execute(ctx.getSource(), StringArgumentType.getString(ctx, "team_name"))));

        // Actually register the command.
        commandDispatcher.register(command);
    }

    private static int execute(final CommandSourceStack source, final String teamname)
    {
        UUID toAdd = null;
        try
        {
            toAdd = source.getPlayerOrException().getUUID();
        }
        catch (final Exception e)
        {
            // This was run from server to just generate the team.
        }
        try
        {
            LandManager.getInstance().createTeam(toAdd, teamname);
        }
        catch (final IllegalArgumentException e)
        {
            Essentials.config.sendError(source, e.getMessage());
            return 1;
        }
        Essentials.config.sendFeedback(source, "thutessentials.team.created", false, teamname);
        return 0;
    }
}
