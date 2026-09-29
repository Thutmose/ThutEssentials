package thut.essentials.commands.land.claims;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.util.ChatHelper;
import thut.essentials.util.PermNodes;
import thut.essentials.util.PermNodes.DefaultPermissionLevel;

public class Claim
{
    public static final String BYPASSLIMIT = "thutessentials.land.claim.nolimit";

    public static void register(final CommandDispatcher<CommandSourceStack> commandDispatcher)
    {
        final String name = "claim";
        if (Essentials.config.commandBlacklist.contains(name)) return;
        String perm;
        PermNodes.registerBooleanNode(perm = "command." + name, DefaultPermissionLevel.ALL,
                "Can the player use /" + name);
        PermNodes.registerBooleanNode(Claim.BYPASSLIMIT, DefaultPermissionLevel.OP,
                "Permission to bypass the land per player limit for a team.");

        // Setup with name and permission
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name)
                .requires(cs -> CommandManager.hasPerm(cs, perm));

        // Entire chunk
        command = command.executes(ctx -> Claim.execute(ctx.getSource(), true, true, false));
        commandDispatcher.register(command);

        command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs, perm));
        command = command
                .then(Commands.literal("up").executes(ctx -> Claim.execute(ctx.getSource(), true, false, false)));
        commandDispatcher.register(command);

        command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs, perm));
        command = command
                .then(Commands.literal("down").executes(ctx -> Claim.execute(ctx.getSource(), false, true, false)));
        commandDispatcher.register(command);

        command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs, perm));
        command = command
                .then(Commands.literal("here").executes(ctx -> Claim.execute(ctx.getSource(), false, false, true)));
        commandDispatcher.register(command);

        command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs, perm));
        command = command.then(Commands.literal("check").executes(ctx -> Claim.executeCheck(ctx.getSource())));
        commandDispatcher.register(command);
    }

    private static int executeCheck(final CommandSourceStack source) throws CommandSyntaxException
    {
        final Player player = source.getPlayerOrException();
        final LandTeam team = LandManager.getTeam(player);
        final long count = LandManager.getInstance().countLand(team.teamName);
        final int teamCount = team.member.size();
        final long maxLand = team.maxLand < 0 ? teamCount * Essentials.config.teamBlocksPerPlayer : team.maxLand;
        ChatHelper.sendSystemMessage(player,
                Essentials.config.getMessage("thutessentials.claim.claimed.count", count, maxLand));
        return 0;
    }

    private static int execute(final CommandSourceStack source, boolean up, boolean down, boolean here)
            throws CommandSyntaxException
    {
        final ServerPlayer player = source.getPlayerOrException();
        final LandTeam team = LandManager.getTeam(player);
        if (!team.hasRankPerm(player.getUUID(), LandTeam.CLAIMPERM))
        {
            ChatHelper.sendSystemMessage(player,
                    Essentials.config.getMessage("thutessentials.claim.notallowed.teamperms"));
            return 1;
        }
        if (here) down = up = false;
        boolean finalUp = up;
        boolean finalDown = down;
        player.getServer().execute(() -> {
            final int y = player.blockPosition().getY() >> 4;
            claimChunk(player, finalUp, finalDown, player.chunkPosition(), y);
        });
        return 0;
    }

    public static void claimChunk(ServerPlayer player, boolean up, boolean down, ChunkPos pos, int y)
    {
        var dim = player.serverLevel();
        int min = down ? dim.getMinBuildHeight() : y * 16;
        int max = up ? dim.getMaxBuildHeight() : y * 16 + 16;
        int x0 = pos.getMinBlockX();
        int x1 = pos.getMaxBlockX() + 1;
        int z0 = pos.getMinBlockZ();
        int z1 = pos.getMaxBlockZ() + 1;
        BoundingBox box = new BoundingBox(x0, min, z0, x1, max, z1);
        claimBox(player, box);
    }

    public static void claimBox(ServerPlayer player, BoundingBox box)
    {
        final LandTeam team = LandManager.getTeam(player);
        if (!team.hasRankPerm(player.getUUID(), LandTeam.CLAIMPERM))
        {
            ChatHelper.sendSystemMessage(player,
                    Essentials.config.getMessage("thutessentials.claim.notallowed.teamperms"));
            return;
        }
        final boolean noLimit = PermNodes.getBooleanPerm(player, Claim.BYPASSLIMIT);
        var dim = player.serverLevel();

        long resp = LandManager.getInstance().claimVolume(team.teamName, !noLimit, dim, box);
        boolean notclaimed = resp == -1;
        boolean claimed = resp > 0;

        if (notclaimed) ChatHelper.sendSystemMessage(player,
                Essentials.config.getMessage("thutessentials.claim.warn.alreadyclaimed", notclaimed));
        if (claimed) ChatHelper.sendSystemMessage(player,
                Essentials.config.getMessage("thutessentials.claim.claimed.num", resp, team.teamName));
        else ChatHelper.sendSystemMessage(player,
                Essentials.config.getMessage("thutessentials.claim.claimed.failed", team.teamName));
    }
}
