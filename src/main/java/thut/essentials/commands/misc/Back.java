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
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
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
import thut.essentials.land.LandManager.KGobalPos;
import thut.essentials.util.CoordinateUtls;
import thut.essentials.util.PlayerDataHandler;
import thut.essentials.util.PlayerMover;

public class Back
{
    @SubscribeEvent
    public static void move(final MoveEvent event)
    {
        if (Essentials.config.back_on_tp) PlayerDataHandler.getCustomDataTag(event.getEntityLiving()
                .getCachedUniqueIdString()).put("backPos", CoordinateUtls.toNBT(event.getPos(), "backPos"));
    }

    @SubscribeEvent
    public static void death(final LivingDeathEvent event)
    {
        if (event.getEntityLiving() instanceof ServerPlayerEntity && Essentials.config.back_on_death)
        {
            final CompoundNBT tag = CoordinateUtls.toNBT(CoordinateUtls.forMob(event.getEntityLiving()), "back");
            PlayerDataHandler.getCustomDataTag(event.getEntityLiving().getCachedUniqueIdString()).put("backPos", tag);
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
        final long time = player.getServer().getWorld(World.OVERWORLD).getGameTime();
        if (last > time && Essentials.config.backReUseDelay > 0)
        {
            player.sendMessage(Essentials.config.getMessage("thutessentials.tp.tosoon"), Util.DUMMY_UUID);
            return 1;
        }
        if (tag.contains("backPos"))
        {
            final KGobalPos spot = Back.getBackSpot(CoordinateUtls.fromNBT(tag.getCompound("backPos")));
            if (spot == null)
            {
                player.sendMessage(Essentials.config.getMessage("thutessentials.back.noroom"), Util.DUMMY_UUID);
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
            PlayerMover.setMove(player, Essentials.config.backActivateDelay, spot, teleMess, PlayerMover.INTERUPTED,
                    callback, false);
            return 0;
        }
        player.sendMessage(Essentials.config.getMessage("thutessentials.back.noback"), Util.DUMMY_UUID);
        return 1;
    }

    private static KGobalPos getBackSpot(final KGobalPos pos)
    {
        KGobalPos spot = pos;
        if (pos == null) return null;
        final MinecraftServer server = LogicalSidedProvider.INSTANCE.get(LogicalSide.SERVER);
        final ServerWorld world = server.getWorld(pos.getDimension());
        if (world == null) return null;
        final BlockPos check = spot.getPos();
        if (Back.valid(check, world)) return spot;
        final int r = Essentials.config.backRangeCheck;
        BlockPos test;
        for (int j = 0; j < r; j++)
            for (int i = 0; i < r; i++)
                for (int k = 0; k < r; k++)
                {
                    test = new BlockPos(check.getX() + i, check.getY() + j, check.getX() + k);
                    spot = KGobalPos.getPosition(pos.getDimension(), test);
                    if (Back.valid(check, world)) return spot;
                    test = new BlockPos(check.getX() - i, check.getY() + j, check.getX() + k);
                    spot = KGobalPos.getPosition(pos.getDimension(), test);
                    if (Back.valid(check, world)) return spot;
                    test = new BlockPos(check.getX() - i, check.getY() + j, check.getX() - k);
                    spot = KGobalPos.getPosition(pos.getDimension(), test);
                    if (Back.valid(check, world)) return spot;
                    test = new BlockPos(check.getX() + i, check.getY() + j, check.getX() - k);
                    spot = KGobalPos.getPosition(pos.getDimension(), test);
                    if (Back.valid(check, world)) return spot;
                }
        return null;
    }

    static boolean valid(final BlockPos pos, final World world)
    {
        final BlockState state1 = world.getBlockState(pos);
        final BlockState state2 = world.getBlockState(pos.up());
        final boolean valid1 = state1 == null || !state1.getMaterial().isSolid();
        final boolean valid2 = state2 == null || !state2.getMaterial().isSolid();
        return valid1 && valid2;
    }
}
