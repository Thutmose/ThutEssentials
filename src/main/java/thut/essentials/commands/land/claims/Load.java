package thut.essentials.commands.land.claims;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.KGobalPos;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.util.ChatHelper;
import thut.essentials.util.PermNodes;
import thut.essentials.util.PermNodes.DefaultPermissionLevel;

public class Load
{
    public static void register(final CommandDispatcher<CommandSourceStack> commandDispatcher)
    {
        if (!Essentials.config.chunkLoading) return;

        final String name = "chunkload";
        if (Essentials.config.commandBlacklist.contains(name)) return;
        String perm;
        PermNodes.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /" + name);

        // Setup with name and permission
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name)
                .requires(cs -> CommandManager.hasPerm(cs, perm));
        // Register the execution.
        command = command.executes(ctx -> Load.execute(ctx.getSource()));

        // Actually register the command.
        commandDispatcher.register(command);
    }

    private static int execute(final CommandSourceStack source) throws CommandSyntaxException
    {
        final Player player = source.getPlayerOrException();
        final LandTeam team = LandManager.getTeam(player);
        if (!team.hasRankPerm(player.getUUID(), LandTeam.CLAIMPERM))
        {
            ChatHelper.sendSystemMessage(player,
                    Essentials.config.getMessage("thutessentials.claim.notallowed.teamperms"));
            return 1;
        }
        final int maxLoaded = team.maxLoaded != -1 ? team.maxLoaded : Essentials.config.maxChunkloads;
        if (team.land.getLoaded().size() >= maxLoaded)
        {
            ChatHelper.sendSystemMessage(player, Essentials.config.getMessage("thutessentials.claim.load.maxexceeded"));
            return 1;
        }
        final int x = Mth.floor(player.blockPosition().getX() >> 4);
        final int y = Mth.floor(player.blockPosition().getY() >> 4);
        final int z = Mth.floor(player.blockPosition().getZ() >> 4);
        if (y < 0 || y > 15) return 1;
        final ResourceKey<Level> dim = player.getCommandSenderWorld().dimension();
        final BlockPos b = new BlockPos(x, 0, z);
        final KGobalPos chunk = KGobalPos.getPosition(dim, b);
        final LandTeam owner = LandManager.getInstance().getLandOwner(chunk);

        if (owner == team && !team.land.getLoaded().contains(chunk) && LandManager.getInstance().loadLand(chunk, team))
            ChatHelper.sendSystemMessage(player, Essentials.config.getMessage("thutessentials.claim.loaded",
                    maxLoaded - team.land.getLoaded().size()));
        else
        {
            // TODO failed message here.
        }

        return 0;
    }
}
