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
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.events.MoveEvent;
import thut.essentials.util.Coordinate;
import thut.essentials.util.PlayerDataHandler;
import thut.essentials.util.PlayerMover;

public class Back
{
    @SubscribeEvent
    public static void move(final MoveEvent event)
    {
        if (Essentials.config.back_on_tp) PlayerDataHandler.getCustomDataTag(event.getEntityLiving()
                .getCachedUniqueIdString()).putIntArray("prevPos", event.getPos());
    }

    @SubscribeEvent
    public static void death(final LivingDeathEvent event)
    {
        if (event.getEntityLiving() instanceof ServerPlayerEntity && Essentials.config.back_on_death)
        {
            final BlockPos pos = event.getEntityLiving().getPosition();
            final int[] loc = new int[] { pos.getX(), pos.getY(), pos.getZ(), event.getEntityLiving().dimension
                    .getId() };
            PlayerDataHandler.getCustomDataTag(event.getEntityLiving().getCachedUniqueIdString()).putIntArray("prevPos",
                    loc);
            PlayerDataHandler.saveCustomData(event.getEntityLiving().getCachedUniqueIdString());
        }
    }

    private static boolean registered = false;

    public static void register(final CommandDispatcher<CommandSource> commandDispatcher)
    {
        final String name = "back";
        if (Essentials.config.commandBlacklist.contains(name)) return;
        String perm;
        PermissionAPI.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /" + name);

        // Register to bus
        if (!Back.registered) MinecraftForge.EVENT_BUS.register(Back.class);
        Back.registered = true;

        // Setup with name and permission
        LiteralArgumentBuilder<CommandSource> command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs,
                perm));
        // Register the execution.
        command = command.executes(ctx -> Back.execute(ctx.getSource()));

        // Actually register the command.
        commandDispatcher.register(command);
    }

    private static int execute(final CommandSource source) throws CommandSyntaxException
    {
        final PlayerEntity player = source.asPlayer();
        final CompoundNBT tag = PlayerDataHandler.getCustomDataTag(player);
        final CompoundNBT tptag = tag.getCompound("tp");
        final long last = tptag.getLong("backDelay");
        final long time = player.getServer().getWorld(DimensionType.OVERWORLD).getGameTime();
        if (last > time && Essentials.config.backReUseDelay > 0)
        {
            player.sendMessage(Essentials.config.getMessage("thutessentials.tp.tosoon"));
            return 1;
        }
        if (tag.contains("prevPos"))
        {
            final int[] pos = tag.getIntArray("prevPos");
            final Coordinate spot = Back.getBackSpot(pos);
            if (spot == null)
            {

                player.sendMessage(Essentials.config.getMessage("thutessentials.back.noroom"));
                return 1;

            }
            final Predicate<Entity> callback = t ->
            {
                if (!(t instanceof PlayerEntity)) return false;
                PlayerDataHandler.getCustomDataTag(t.getCachedUniqueIdString()).remove("prevPos");
                tptag.putLong("backDelay", time + Essentials.config.backReUseDelay);
                tag.put("tp", tptag);
                PlayerDataHandler.saveCustomData((PlayerEntity) t);
                return true;
            };
            final ITextComponent teleMess = Essentials.config.getMessage("thutessentials.back.succeed");
            PlayerMover.setMove(player, Essentials.config.backActivateDelay, spot.dim, new BlockPos(spot.x, spot.y,
                    spot.z), teleMess, PlayerMover.INTERUPTED, callback, false);
            return 0;
        }
        player.sendMessage(Essentials.config.getMessage("thutessentials.back.noback"));
        return 1;
    }

    private static Coordinate getBackSpot(final int[] pos)
    {
        Coordinate spot = new Coordinate(pos[0], pos[1], pos[2], pos[3]);
        final MinecraftServer server = LogicalSidedProvider.INSTANCE.get(LogicalSide.SERVER);
        final ServerWorld world = server.getWorld(DimensionType.getById(pos[3]));
        if (world == null) return null;
        BlockPos check = new BlockPos(spot.x, spot.y, spot.z);
        if (Back.valid(check, world)) return spot;
        final int r = Essentials.config.backRangeCheck;
        for (int j = 0; j < r; j++)
            for (int i = 0; i < r; i++)
                for (int k = 0; k < r; k++)
                {
                    spot = new Coordinate(pos[0] + i, pos[1] + j, pos[2] + k, pos[3]);
                    check = new BlockPos(spot.x, spot.y, spot.z);
                    if (Back.valid(check, world)) return spot;
                    spot = new Coordinate(pos[0] - i, pos[1] + j, pos[2] - k, pos[3]);
                    check = new BlockPos(spot.x, spot.y, spot.z);
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
