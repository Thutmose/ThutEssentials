package thut.essentials.commands.misc;

import java.util.Random;
import java.util.function.Predicate;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.block.BlockState;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.Heightmap.Type;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.util.Coordinate;
import thut.essentials.util.PlayerDataHandler;
import thut.essentials.util.PlayerMover;

public class RTP
{
    public static BlockPos centre = null;

    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        final String name = "rtp";
        if (Essentials.config.commandBlacklist.contains(name)) return;
        String perm;
        PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /" + name);

        // Setup with name and permission
        LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs,
                perm));
        // Register the execution.
        command = command.executes(ctx -> RTP.execute(ctx.getSource()));

        // Actually register the command.
        commandDispatcher.register(command);
    }

    private static int execute(final CommandSource source) throws CommandSyntaxException
    {
        final PlayerEntity player = source.asPlayer();
        final CompoundNBT tag = PlayerDataHandler.getCustomDataTag(player);
        final CompoundNBT tptag = tag.getCompound("tp");
        final long last = tptag.getLong("rtpDelay");
        final long time = player.getServer().getWorld(DimensionType.OVERWORLD).getGameTime();
        if (last > time && Essentials.config.rtpReUseDelay > 0)
        {
            player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.tp.tosoon", TextFormatting.RED,
                    false));
            return 1;
        }
        final Coordinate spot = RTP.getRTPSpot(player);
        if (spot != null)
        {
            final Predicate<Entity> callback = t ->
            {
                if (!(t instanceof PlayerEntity)) return false;
                tptag.putLong("bedDelay", time + Essentials.config.rtpReUseDelay);
                tag.put("tp", tptag);
                PlayerDataHandler.saveCustomData((PlayerEntity) t);
                return true;
            };
            final ITextComponent teleMess = CommandManager.makeFormattedComponent("thutessentials.rtp.succeed",
                    TextFormatting.GREEN);
            PlayerMover.setMove(player, Essentials.config.rtpActivateDelay, spot.dim, new BlockPos(spot.x, spot.y,
                    spot.z), teleMess, PlayerMover.INTERUPTED, callback, false);
            return 0;
        }
        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.rtp.fail", TextFormatting.RED, false));
        return 1;
    }

    private static Coordinate getRTPSpot(final PlayerEntity player)
    {
        final World world = player.getEntityWorld();
        final Random rand = new Random();
        final int dx = rand.nextInt(Essentials.config.rtpDistance) * (rand.nextBoolean() ? 1 : -1);
        final int dz = rand.nextInt(Essentials.config.rtpDistance) * (rand.nextBoolean() ? 1 : -1);
        int x0 = RTP.centre == null ? world.getSpawnPoint().getX() : RTP.centre.getX();
        int z0 = RTP.centre == null ? world.getSpawnPoint().getZ() : RTP.centre.getZ();
        if (Essentials.config.rtpPlayerCentred)
        {
            x0 = player.getPosition().getX();
            z0 = player.getPosition().getZ();
        }
        final int x = x0 + dx;
        final int z = z0 + dz;
        // Ensure the chunk exists.
        world.getChunk(new BlockPos(x, 0, z));
        // Find the height at that location
        final int y = world.getHeight(Type.MOTION_BLOCKING, x, z);
        final DimensionType dim = player.dimension;
        Coordinate spot = new Coordinate(x, y + 1, z, dim.getId());
        BlockPos check = new BlockPos(spot.x, spot.y, spot.z);

        if (RTP.valid(check, world)) return spot;
        final int r = Essentials.config.backRangeCheck;
        for (int j = 0; j < r; j++)
            for (int i = 0; i < r; i++)
                for (int k = 0; k < r; k++)
                {
                    spot = new Coordinate(x + i, y + j, z + k, dim.getId());
                    check = new BlockPos(spot.x, spot.y, spot.z);
                    if (RTP.valid(check, world)) return spot;
                    spot = new Coordinate(x - i, y + j, z - k, dim.getId());
                    check = new BlockPos(spot.x, spot.y, spot.z);
                    if (RTP.valid(check, world)) return spot;
                }
        return null;
    }

    private static boolean valid(final BlockPos pos, final World world)
    {
        final BlockState state1 = world.getBlockState(pos);
        final BlockState state2 = world.getBlockState(pos.up());
        final boolean valid1 = state1 == null || !state1.getMaterial().isSolid();
        final boolean valid2 = state2 == null || !state2.getMaterial().isSolid();
        return valid1 && valid2;
    }
}
