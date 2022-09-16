package thut.essentials.commands.land.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.util.ChatHelper;
import thut.essentials.util.PermNodes;
import thut.essentials.util.PermNodes.DefaultPermissionLevel;
import thut.essentials.util.PlayerDataHandler;
import thut.essentials.util.PlayerMover;

public class Home
{

    public static void register(final CommandDispatcher<CommandSourceStack> commandDispatcher)
    {
        final String name = "team_home";
        if (Essentials.config.commandBlacklist.contains(name)) return;
        String perm;
        PermNodes.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL,
                "Can the player use the team_home commant.");

        // Setup with name and permission
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs,
                perm));
        // No target argument version
        command = command.executes(ctx -> Home.execute(ctx.getSource()));

        // Actually register the command.
        commandDispatcher.register(command);
    }

    private static int execute(final CommandSourceStack source) throws CommandSyntaxException
    {
        final Player player = source.getPlayerOrException();
        final LandTeam team = LandManager.getTeam(player);

        if (team.team_home == null)
        {
            source.sendFailure(Essentials.config.getMessage("thutessentials.team.nohomeset"));
            return 1;
        }

        final CompoundTag tag = PlayerDataHandler.getCustomDataTag(player);
        final CompoundTag tptag = tag.getCompound("tp");
        final long last = tptag.getLong("homeDelay");
        final long time = player.getServer().getLevel(Level.OVERWORLD).getGameTime();
        if (last > time && Essentials.config.homeReUseDelay > 0)
        {
            ChatHelper.sendSystemMessage(player, Essentials.config.getMessage("thutessentials.tp.tosoon"));
            return 1;
        }

        final Component teleMess = Essentials.config.getMessage("Warping to your Team's Home");
        tptag.putLong("homeDelay", time + Essentials.config.homeReUseDelay);
        tag.put("tp", tptag);
        PlayerDataHandler.saveCustomData(player);
        PlayerMover.setMove(player, Essentials.config.homeActivateDelay, team.team_home, teleMess,
                PlayerMover.INTERUPTED);
        return 0;
    }
}
