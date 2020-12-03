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
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.world.gen.Heightmap.Type;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
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
        final ServerPlayerEntity player = source.asPlayer();
        final CompoundNBT tag = PlayerDataHandler.getCustomDataTag(player);
        final CompoundNBT tptag = tag.getCompound("tp");
        final long last = tptag.getLong("rtpDelay");
        final long time = player.getServer().getWorld(World.OVERWORLD).getGameTime();
        if (last > time && Essentials.config.rtpReUseDelay > 0)
        {
            player.sendMessage(Essentials.config.getMessage("thutessentials.tp.tosoon"), Util.DUMMY_UUID);
            return 1;
        }
        final GlobalPos spot = RTP.getRTPSpot(player);
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
            final ITextComponent teleMess = Essentials.config.getMessage("thutessentials.rtp.succeed");
            PlayerMover.setMove(player, Essentials.config.rtpActivateDelay, spot, teleMess, PlayerMover.INTERUPTED,
                    callback, false);
            return 0;
        }
        player.sendMessage(Essentials.config.getMessage("thutessentials.rtp.fail"), Util.DUMMY_UUID);
        return 1;
    }

    private static GlobalPos getRTPSpot(final ServerPlayerEntity player)
    {
        final ServerWorld world = (ServerWorld) player.getEntityWorld();
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
        final RegistryKey<World> dim = world.getDimensionKey();
        GlobalPos spot = GlobalPos.getPosition(dim, new BlockPos(x, y + 1, z));
        final BlockPos check = spot.getPos();

        if (RTP.valid(check, world)) return spot;
        BlockPos test;
        final int r = Essentials.config.backRangeCheck;
        for (int j = 0; j < r; j++)
            for (int i = 0; i < r; i++)
                for (int k = 0; k < r; k++)
                {
                    test = new BlockPos(check.getX() + i, check.getY() + j, check.getX() + k);
                    spot = GlobalPos.getPosition(spot.getDimension(), test);
                    if (Back.valid(check, world)) return spot;
                    test = new BlockPos(check.getX() - i, check.getY() + j, check.getX() + k);
                    spot = GlobalPos.getPosition(spot.getDimension(), test);
                    if (Back.valid(check, world)) return spot;
                    test = new BlockPos(check.getX() - i, check.getY() + j, check.getX() - k);
                    spot = GlobalPos.getPosition(spot.getDimension(), test);
                    if (Back.valid(check, world)) return spot;
                    test = new BlockPos(check.getX() + i, check.getY() + j, check.getX() - k);
                    spot = GlobalPos.getPosition(spot.getDimension(), test);
                    if (Back.valid(check, world)) return spot;
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
