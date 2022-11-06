package thut.essentials.commands.land.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.util.PermNodes;
import thut.essentials.util.PermNodes.DefaultPermissionLevel;

public class Check
{

    public static void register(final CommandDispatcher<CommandSourceStack> commandDispatcher)
    {
        // TODO configurable this.
        final String name = "my_team";
        if (Essentials.config.commandBlacklist.contains(name)) return;
        String perm;
        PermNodes.registerBooleanNode(perm = "command." + name, DefaultPermissionLevel.ALL,
                "Can the player see their own team's name.");

        // Setup with name and permission
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs,
                perm));
        // No target argument version
        command = command.executes(ctx -> Check.execute(ctx.getSource()));

        // Actually register the command.
        commandDispatcher.register(command);
    }

    private static int execute(final CommandSourceStack source) throws CommandSyntaxException
    {
        final LandTeam team = LandManager.getTeam(source.getPlayerOrException());
        Essentials.config.sendFeedback(source, "thutessentials.team.my_team", false, team.teamName);
        return 0;
    }
}
