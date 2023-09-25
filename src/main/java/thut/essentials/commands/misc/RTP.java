package thut.essentials.commands.misc;

import java.util.Random;
import java.util.function.Predicate;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.land.LandManager.KGobalPos;
import thut.essentials.util.ChatHelper;
import thut.essentials.util.PermNodes;
import thut.essentials.util.PermNodes.DefaultPermissionLevel;
import thut.essentials.util.PlayerDataHandler;
import thut.essentials.util.PlayerMover;

public class RTP
{
    public static BlockPos centre = null;

    public static void register(final CommandDispatcher<CommandSourceStack> commandDispatcher)
    {
        final String name = "rtp";
        if (Essentials.config.commandBlacklist.contains(name)) return;
        String perm;
        PermNodes.registerBooleanNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /" + name);

        // Setup with name and permission
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs,
                perm));
        // Register the execution.
        command = command.executes(ctx -> RTP.execute(ctx.getSource()));

        // Actually register the command.
        commandDispatcher.register(command);
    }

    private static int execute(final CommandSourceStack source) throws CommandSyntaxException
    {
        final ServerPlayer player = source.getPlayerOrException();
        final CompoundTag tag = PlayerDataHandler.getCustomDataTag(player);
        final CompoundTag tptag = tag.getCompound("tp");
        final long last = tptag.getLong("rtpDelay");
        final long time = player.getServer().getLevel(Level.OVERWORLD).getGameTime();
        if (last > time && Essentials.config.rtpReUseDelay > 0)
        {
            ChatHelper.sendSystemMessage(player, Essentials.config.getMessage("thutessentials.tp.tosoon"));
            return 1;
        }
        final KGobalPos spot = RTP.getRTPSpot(player);
        if (spot != null)
        {
            final Predicate<Entity> callback = t ->
            {
                if (!(t instanceof Player)) return false;
                tptag.putLong("bedDelay", time + Essentials.config.rtpReUseDelay);
                tag.put("tp", tptag);
                PlayerDataHandler.saveCustomData((Player) t);
                return true;
            };
            final Component teleMess = Essentials.config.getMessage("thutessentials.rtp.succeed");
            PlayerMover.setMove(player, Essentials.config.rtpActivateDelay, spot, teleMess, PlayerMover.INTERUPTED,
                    callback, false);
            return 0;
        }
        ChatHelper.sendSystemMessage(player, Essentials.config.getMessage("thutessentials.rtp.fail"));
        return 1;
    }

    private static KGobalPos getRTPSpot(final ServerPlayer player)
    {
        final ServerLevel world = (ServerLevel) player.getCommandSenderWorld();
        final Random rand = new Random();
        final int dx = rand.nextInt(Essentials.config.rtpDistance) * (rand.nextBoolean() ? 1 : -1);
        final int dz = rand.nextInt(Essentials.config.rtpDistance) * (rand.nextBoolean() ? 1 : -1);
        int x0 = RTP.centre == null ? world.getSharedSpawnPos().getX() : RTP.centre.getX();
        int z0 = RTP.centre == null ? world.getSharedSpawnPos().getZ() : RTP.centre.getZ();
        if (Essentials.config.rtpPlayerCentred)
        {
            x0 = player.blockPosition().getX();
            z0 = player.blockPosition().getZ();
        }
        final int x = x0 + dx;
        final int z = z0 + dz;
        // Ensure the chunk exists.
        world.getChunk(new BlockPos(x, 0, z));
        // Find the height at that location
        final int y = world.getHeight(Types.MOTION_BLOCKING, x, z);
        final ResourceKey<Level> dim = world.dimension();
        KGobalPos spot = KGobalPos.getPosition(dim, new BlockPos(x, y + 1, z));
        final BlockPos check = spot.getPos();

        if (RTP.valid(check, world)) return spot;
        BlockPos test;
        final int r = Essentials.config.backRangeCheck;
        for (int j = 0; j < r; j++)
            for (int i = 0; i < r; i++)
                for (int k = 0; k < r; k++)
                {
                    test = new BlockPos(check.getX() + i, check.getY() + j, check.getX() + k);
                    spot = KGobalPos.getPosition(spot.getDimension(), test);
                    if (Back.valid(check, world)) return spot;
                    test = new BlockPos(check.getX() - i, check.getY() + j, check.getX() + k);
                    spot = KGobalPos.getPosition(spot.getDimension(), test);
                    if (Back.valid(check, world)) return spot;
                    test = new BlockPos(check.getX() - i, check.getY() + j, check.getX() - k);
                    spot = KGobalPos.getPosition(spot.getDimension(), test);
                    if (Back.valid(check, world)) return spot;
                    test = new BlockPos(check.getX() + i, check.getY() + j, check.getX() - k);
                    spot = KGobalPos.getPosition(spot.getDimension(), test);
                    if (Back.valid(check, world)) return spot;
                }
        return null;
    }

    private static boolean valid(final BlockPos pos, final Level world)
    {
        final BlockState state1 = world.getBlockState(pos);
        final BlockState state2 = world.getBlockState(pos.above());
        final boolean valid1 = state1 == null || !state1.isSolid();
        final boolean valid2 = state2 == null || !state2.isSolid();
        return valid1 && valid2;
    }
}
