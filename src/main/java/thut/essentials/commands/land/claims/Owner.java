package thut.essentials.commands.land.claims;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.util.PermNodes;
import thut.essentials.util.PermNodes.DefaultPermissionLevel;

public class Owner
{
    public static void register(final CommandDispatcher<CommandSourceStack> commandDispatcher)
    {
        final String name = "land_owner";
        if (Essentials.config.commandBlacklist.contains(name)) return;
        String perm;
        PermNodes.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /" + name);

        // Setup with name and permission
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name)
                .requires(cs -> CommandManager.hasPerm(cs, perm));
        // Register the execution.
        command = command.executes(ctx -> Owner.execute(ctx.getSource()));

        // Actually register the command.
        commandDispatcher.register(command);
    }

    private static int execute(final CommandSourceStack source) throws CommandSyntaxException
    {
        final Player player = source.getPlayerOrException();
        final int x = Mth.floor(player.blockPosition().getX() >> 4);
        final int y = Mth.floor(player.blockPosition().getY() >> 4);
        final int z = Mth.floor(player.blockPosition().getZ() >> 4);
        final Level dim = player.getCommandSenderWorld();
        if (y < dim.getMinSection() || y > dim.getMaxSection()) return 1;
        final BlockPos b = new BlockPos(x, y, z);
        final LandTeam owner = LandManager.getInstance().getLandOwner(dim, b, true);

        if (!LandManager.isWild(owner))
            player.sendMessage(Essentials.config.getMessage("thutessentials.claim.ownedby", owner.teamName),
                    Util.NIL_UUID);
        else player.sendMessage(Essentials.config.getMessage("thutessentials.claim.unowned"), Util.NIL_UUID);
        return 0;
    }
}
