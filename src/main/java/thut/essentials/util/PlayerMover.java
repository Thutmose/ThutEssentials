package thut.essentials.util;

import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import com.google.common.collect.Maps;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Util;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import thut.essentials.Essentials;
import thut.essentials.events.MoveEvent;
import thut.essentials.land.LandManager.KGobalPos;
import thut.essentials.util.Transporter.TeleDest;
import thut.essentials.util.Transporter.Vector3;

public class PlayerMover

{
    public static ITextComponent INTERUPTED;

    private static Vector3 offset = new Vector3().set(0.5, 0.5, 0.5);

    private static class Mover
    {
        final long              moveTime;
        final PlayerEntity      player;
        final KGobalPos         moveTo;
        final KGobalPos         start;
        final ITextComponent    message;
        final ITextComponent    failMess;
        final boolean           event;
        final Predicate<Entity> callback;

        public Mover(final PlayerEntity player, final long moveTime, final KGobalPos moveTo,
                final ITextComponent message, final ITextComponent failMess, final Predicate<Entity> callback,
                final boolean event)
        {
            this.player = player;
            this.moveTime = moveTime;
            this.moveTo = moveTo;
            this.message = message;
            this.failMess = failMess;
            this.event = event;
            this.callback = callback;
            this.start = CoordinateUtls.forMob(player);
        }

        private void move()
        {
            if (!this.moveTo.isValid()) return;
            if (!this.start.isValid()) return;
            if (this.player == null || !this.player.inChunk || !this.player.isAddedToWorld()) return;
            if (this.event) MinecraftForge.EVENT_BUS.post(new MoveEvent(this.player));
            if (Essentials.config.log_teleports) InventoryLogger.log("Teleport from {} {} to {} {} for {} {}",
                    CoordinateUtls.chunkPos(this.start), this.start.getDimension().location(), this.start.getPos(),
                    this.moveTo.getDimension().location(), this.moveTo.getPos(), this.player.getUUID(),
                    this.player.getName().getString());
            final TeleDest dest = new TeleDest();
            dest.setLoc(this.moveTo, new Vector3().set(this.moveTo.getPos()).add(PlayerMover.offset));
            try
            {
                Transporter.transferTo(this.player, dest);
            }
            catch (final Exception e)
            {
                Essentials.LOGGER.error("Error teleporting player!");
                Essentials.LOGGER.error(e);
            }
            if (this.callback != null) this.callback.test(this.player);
            if (this.message != null) this.player.sendMessage(this.message, Util.NIL_UUID);
        }
    }

    public static void setMove(final PlayerEntity player, final int moveTime, final KGobalPos moveTo,
            final ITextComponent message, final ITextComponent failMess)
    {
        PlayerMover.setMove(player, moveTime, moveTo, message, failMess, true);
    }

    public static void setMove(final PlayerEntity player, final int moveTime, final KGobalPos moveTo,
            final ITextComponent message, final ITextComponent failMess, final boolean event)
    {
        PlayerMover.setMove(player, moveTime, moveTo, message, failMess, null, event);
    }

    public static void setMove(final PlayerEntity player, final int moveTime, final KGobalPos moveTo,
            final ITextComponent message, final ITextComponent failMess, final Predicate<Entity> callback,
            final boolean event)
    {
        if (player.getVehicle() != null || player.isVehicle())
        {
            player.sendMessage(Essentials.config.getMessage("thutessentials.tp.dismount"), Util.NIL_UUID);
            return;
        }
        player.getServer().executeBlocking(() ->
        {
            if (!PlayerMover.toMove.containsKey(player.getUUID()))
            {
                long time = moveTime;
                if (time > 0)
                {
                    player.sendMessage(Essentials.config.getMessage("thutessentials.tp.tele_init"), Util.NIL_UUID);
                    time += player.getCommandSenderWorld().getGameTime();
                }
                PlayerMover.toMove.put(player.getUUID(), new Mover(player, time, moveTo, message, failMess,
                        callback, event));
            }
        });
    }

    static Map<UUID, Mover> toMove = Maps.newHashMap();

    @SubscribeEvent
    public void playerTick(final LivingUpdateEvent tick)
    {
        if (!(tick.getEntity().level instanceof ServerWorld)) return;
        if (PlayerMover.toMove.containsKey(tick.getEntity().getUUID()))
        {
            final Mover mover = PlayerMover.toMove.get(tick.getEntity().getUUID());
            final KGobalPos playerPos = CoordinateUtls.forMob(mover.player);
            final Vector3i diff = playerPos.getPos().subtract(mover.start.getPos());
            if (tick.getEntity().getCommandSenderWorld().getGameTime() > mover.moveTime)
            {
                mover.move();
                PlayerMover.toMove.remove(tick.getEntity().getUUID());
            }
            else if (diff.distSqr(Vector3i.ZERO) > 1 && mover.moveTime > 0)
            {
                System.out.println(diff.distSqr(Vector3i.ZERO));
                if (mover.failMess != null) tick.getEntity().sendMessage(mover.failMess, Util.NIL_UUID);
                PlayerMover.toMove.remove(tick.getEntity().getUUID());
                return;
            }
        }
    }

}
