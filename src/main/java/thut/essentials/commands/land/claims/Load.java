package thut.essentials.commands.land.claims;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.util.Coordinate;

public class Load
{
    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        if (!Essentials.config.chunkLoading) return;

        final String name = "chunkload";
        if (Essentials.config.commandBlacklist.contains(name)) return;
        String perm;
        PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /" + name);

        // Setup with name and permission
        LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs,
                perm));
        // Register the execution.
        command = command.executes(ctx -> Load.execute(ctx.getSource()));

        // Actually register the command.
        commandDispatcher.register(command);
    }

    private static int execute(final CommandSource source) throws CommandSyntaxException
    {
        final PlayerEntity player = source.asPlayer();
        final LandTeam team = LandManager.getTeam(player);
        if (!team.hasRankPerm(player.getUniqueID(), LandTeam.CLAIMPERM))
        {
            player.sendMessage(Essentials.config.getMessage("thutessentials.claim.notallowed.teamperms"));
            return 1;
        }
        final int maxLoaded = team.maxLoaded != -1 ? team.maxLoaded : Essentials.config.maxChunkloads;
        if (team.land.getLoaded().size() >= maxLoaded)
        {
            player.sendMessage(Essentials.config.getMessage("thutessentials.claim.load.maxexceeded"));
            return 1;
        }
        final int x = MathHelper.floor(player.getPosition().getX() >> 4);
        final int y = MathHelper.floor(player.getPosition().getY() >> 4);
        final int z = MathHelper.floor(player.getPosition().getZ() >> 4);
        if (y < 0 || y > 15) return 1;
        final int dim = player.dimension.getId();
        final Coordinate chunk = new Coordinate(x, 0, z, dim);
        final LandTeam owner = LandManager.getInstance().getLandOwner(chunk);

        if (owner == team && !team.land.getLoaded().contains(chunk) && LandManager.getInstance().loadLand(chunk, team))
            player.sendMessage(Essentials.config.getMessage("thutessentials.claim.loaded", maxLoaded - team.land
                    .getLoaded().size()));
        else
        {
            // TODO failed message here.
        }

        return 0;
    }
}
