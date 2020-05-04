package thut.essentials.commands.misc;

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
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.util.Coordinate;
import thut.essentials.util.PlayerDataHandler;
import thut.essentials.util.PlayerMover;

public class Bed
{
    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        final String name = "bed";
        if (Essentials.config.commandBlacklist.contains(name)) return;
        String perm;
        PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /" + name);

        // Setup with name and permission
        LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs,
                perm));
        // Register the execution.
        command = command.executes(ctx -> Bed.execute(ctx.getSource()));

        // Actually register the command.
        commandDispatcher.register(command);
    }

    private static int execute(final CommandSource source) throws CommandSyntaxException
    {
        final PlayerEntity player = source.asPlayer();
        final CompoundNBT tag = PlayerDataHandler.getCustomDataTag(player);
        final CompoundNBT tptag = tag.getCompound("tp");
        final long last = tptag.getLong("bedDelay");
        final long time = player.getServer().getWorld(DimensionType.OVERWORLD).getGameTime();
        if (last > time && Essentials.config.bedReUseDelay > 0)
        {
            player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.tp.tosoon", TextFormatting.RED,
                    false));
            return 1;
        }
        final Coordinate spot = Bed.getBedSpot(player);
        if (spot != null)
        {
            final Predicate<Entity> callback = t ->
            {
                if (!(t instanceof PlayerEntity)) return false;
                tptag.putLong("bedDelay", time + Essentials.config.bedReUseDelay);
                tag.put("tp", tptag);
                PlayerDataHandler.saveCustomData((PlayerEntity) t);
                return true;
            };
            final ITextComponent teleMess = CommandManager.makeFormattedComponent("thutessentials.bed.succeed",
                    TextFormatting.GREEN);
            PlayerMover.setMove(player, Essentials.config.bedActivateDelay, spot.dim, new BlockPos(spot.x, spot.y,
                    spot.z), teleMess, PlayerMover.INTERUPTED, callback, false);
            return 0;
        }
        player.sendMessage(CommandManager.makeFormattedComponent("thutessentials.bed.nobed", TextFormatting.RED,
                false));
        return 1;
    }

    private static Coordinate getBedSpot(final PlayerEntity player)
    {
        DimensionType dim = player.dimension;
        BlockPos bed = player.getBedLocation(dim);
        if (bed == null) bed = player.getBedLocation(dim = player.getSpawnDimension());
        if (bed == null) return null;
        Coordinate spot = new Coordinate(bed.getX(), bed.getY(), bed.getZ(), dim.getId());
        final MinecraftServer server = LogicalSidedProvider.INSTANCE.get(LogicalSide.SERVER);
        final ServerWorld world = server.getWorld(dim);
        if (world == null) return null;
        BlockPos check = new BlockPos(spot.x, spot.y, spot.z);
        if (Bed.valid(check, world)) return spot;
        final int r = Essentials.config.backRangeCheck;
        for (int j = 0; j < r; j++)
            for (int i = 0; i < r; i++)
                for (int k = 0; k < r; k++)
                {
                    spot = new Coordinate(bed.getX() + i, bed.getY() + j, bed.getZ() + k, dim.getId());
                    check = new BlockPos(spot.x, spot.y, spot.z);
                    if (Bed.valid(check, world)) return spot;
                    spot = new Coordinate(bed.getX() - i, bed.getY() + j, bed.getZ() - k, dim.getId());
                    check = new BlockPos(spot.x, spot.y, spot.z);
                    if (Bed.valid(check, world)) return spot;
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
