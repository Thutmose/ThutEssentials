package thut.essentials.commands.land.management;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.util.PermNodes;
import thut.essentials.util.PermNodes.DefaultPermissionLevel;

public class Delete
{
    public static void register(final CommandDispatcher<CommandSourceStack> commandDispatcher)
    {
        final String name = "delete_team";
        if (!Essentials.config.commandBlacklist.contains(name))
        {
            String perm;
            PermNodes.registerBooleanNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /"
                    + name);

            LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name).requires(cs -> Edit.adminUse(cs,
                    perm));
            command = command.executes(ctx -> Delete.execute(ctx.getSource()));
            commandDispatcher.register(command);
        }
    }

    private static int execute(final CommandSourceStack source) throws CommandSyntaxException
    {
        final ServerPlayer player = source.getPlayerOrException();
        final LandTeam team = LandManager.getTeam(player);
        if (team == null || team == LandManager.getDefaultTeam())
        {
            source.sendFailure(CommandManager.makeFormattedComponent("thutessentials.team.notinateam"));
            return 1;
        }
        LandManager.getInstance().removeTeam(team.teamName);
        Essentials.config.sendFeedback(source, "thutessentials.team.delete.done", false);
        return 0;
    }
}
