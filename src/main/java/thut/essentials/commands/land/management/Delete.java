package thut.essentials.commands.land.management;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;

public class Delete
{
    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        final String name = "delete_team";
        if (!Essentials.config.commandBlacklist.contains(name))
        {
            String perm;
            PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /"
                    + name);

            LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> Edit.adminUse(cs,
                    perm));
            command = command.executes(ctx -> Delete.execute(ctx.getSource()));
            commandDispatcher.register(command);
        }
    }

    private static int execute(final CommandSource source) throws CommandSyntaxException
    {
        final ServerPlayerEntity player = source.asPlayer();
        final LandTeam team = LandManager.getTeam(player);
        if (team == null || team == LandManager.getDefaultTeam())
        {
            source.sendErrorMessage(CommandManager.makeFormattedComponent("thutessentials.team.notinateam"));
            return 1;
        }
        LandManager.getInstance().removeTeam(team.teamName);
        Essentials.config.sendFeedback(source, "thutessentials.team.delete.done", false);
        return 0;
    }
}
