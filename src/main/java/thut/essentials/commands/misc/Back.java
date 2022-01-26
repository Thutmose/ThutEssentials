package thut.essentials.commands.misc;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import thut.essentials.Essentials;
import thut.essentials.commands.CommandManager;
import thut.essentials.events.MoveEvent;
import thut.essentials.land.LandManager.KGobalPos;
import thut.essentials.util.CoordinateUtls;
import thut.essentials.util.PermNodes;
import thut.essentials.util.PermNodes.DefaultPermissionLevel;
import thut.essentials.util.PlayerDataHandler;
import thut.essentials.util.PlayerMover;

public class Back
{
    @SubscribeEvent
    public static void move(final MoveEvent event)
    {
        if (Essentials.config.back_on_tp) PlayerDataHandler.getCustomDataTag(event.getEntityLiving()
                .getStringUUID()).put("backPos", CoordinateUtls.toNBT(event.getPos(), "backPos"));
    }

    @SubscribeEvent
    public static void death(final LivingDeathEvent event)
    {
        if (event.getEntityLiving() instanceof ServerPlayer && Essentials.config.back_on_death)
        {
            final CompoundTag tag = CoordinateUtls.toNBT(CoordinateUtls.forMob(event.getEntityLiving()), "back");
            PlayerDataHandler.getCustomDataTag(event.getEntityLiving().getStringUUID()).put("backPos", tag);
            PlayerDataHandler.saveCustomData(event.getEntityLiving().getStringUUID());
        }
    }

    private static boolean registered = false;

    public static void register(final CommandDispatcher<CommandSourceStack> commandDispatcher)
    {
        final String name = "back";
        if (Essentials.config.commandBlacklist.contains(name)) return;
        String perm;
        PermNodes.registerNode(perm = "command." + name, DefaultPermissionLevel.ALL, "Can the player use /" + name);

        // Register to bus
        if (!Back.registered) MinecraftForge.EVENT_BUS.register(Back.class);
        Back.registered = true;

        // Setup with name and permission
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name).requires(cs -> CommandManager.hasPerm(cs,
                perm));
        // Register the execution.
        command = command.executes(ctx -> Back.execute(ctx.getSource()));

        // Actually register the command.
        commandDispatcher.register(command);
    }

    private static int execute(final CommandSourceStack source) throws CommandSyntaxException
    {
        final Player player = source.getPlayerOrException();
        final CompoundTag tag = PlayerDataHandler.getCustomDataTag(player);
        final CompoundTag tptag = tag.getCompound("tp");
        final long last = tptag.getLong("backDelay");
        final long time = player.getServer().getLevel(Level.OVERWORLD).getGameTime();
        if (last > time && Essentials.config.backReUseDelay > 0)
        {
            player.sendMessage(Essentials.config.getMessage("thutessentials.tp.tosoon"), Util.NIL_UUID);
            return 1;
        }
        if (tag.contains("backPos"))
        {
            final KGobalPos spot = Back.getBackSpot(CoordinateUtls.fromNBT(tag.getCompound("backPos")));
            if (spot == null)
            {
                player.sendMessage(Essentials.config.getMessage("thutessentials.back.noroom"), Util.NIL_UUID);
                return 1;
            }
            final Predicate<Entity> callback = t ->
            {
                if (!(t instanceof Player)) return false;
                PlayerDataHandler.getCustomDataTag(t.getStringUUID()).remove("prevPos");
                tptag.putLong("backDelay", time + Essentials.config.backReUseDelay);
                tag.put("tp", tptag);
                PlayerDataHandler.saveCustomData((Player) t);
                return true;
            };
            final Component teleMess = Essentials.config.getMessage("thutessentials.back.succeed");
            PlayerMover.setMove(player, Essentials.config.backActivateDelay, spot, teleMess, PlayerMover.INTERUPTED,
                    callback, false);
            return 0;
        }
        player.sendMessage(Essentials.config.getMessage("thutessentials.back.noback"), Util.NIL_UUID);
        return 1;
    }

    private static KGobalPos getBackSpot(final KGobalPos pos)
    {
        final KGobalPos spot = pos;
        if (pos == null) return null;
        final MinecraftServer server = Essentials.server;
        final ServerLevel world = server.getLevel(pos.getDimension());
        if (world == null) return null;
        final BlockPos check = spot.getPos();
        if (Back.valid(check, world)) return spot;
        final int r = Essentials.config.backRangeCheck;
        final Stream<BlockPos> stream = BlockPos.betweenClosedStream(check.getX() - r, check.getY() - r, check.getZ() - r, check
                .getX() + r, check.getY() + r, check.getZ() + r);
        final Optional<BlockPos> opt = stream.filter(p -> Back.valid(p, world)).min((p1, p2) ->
        {
            final double d1 = p1.distSqr(check);
            final double d2 = p2.distSqr(check);
            return Double.compare(d1, d2);
        });
        if (!opt.isPresent()) return null;
        return KGobalPos.getPosition(pos.getDimension(), opt.get().immutable());
    }

    static boolean valid(final BlockPos pos, final Level world)
    {
        final BlockState state1 = world.getBlockState(pos);
        final BlockState state2 = world.getBlockState(pos.above());
        final boolean valid1 = state1 == null || !state1.getMaterial().isSolid();
        final boolean valid2 = state2 == null || !state2.getMaterial().isSolid();
        return valid1 && valid2;
    }
}
