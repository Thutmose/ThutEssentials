package thut.essentials.util;

import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import com.google.common.collect.Maps;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Util;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import thut.essentials.Essentials;
import thut.essentials.events.MoveEvent;
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
        final GlobalPos         moveTo;
        final GlobalPos         start;
        final ITextComponent    message;
        final ITextComponent    failMess;
        final boolean           event;
        final Predicate<Entity> callback;

        public Mover(final PlayerEntity player, final long moveTime, final GlobalPos moveTo,
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
            if (this.event) MinecraftForge.EVENT_BUS.post(new MoveEvent(this.player));
            if (Essentials.config.log_teleports) Essentials.LOGGER.trace("TP: " + this.player.getUniqueID() + " "
                    + this.player.getName() + " from: " + this.start + " to " + this.moveTo);
            final TeleDest dest = new TeleDest();
            dest.setLoc(this.moveTo, new Vector3().set(this.moveTo.getPos()).add(PlayerMover.offset));
            Transporter.transferTo(this.player, dest);
            if (this.callback != null) this.callback.test(this.player);
            if (this.message != null) this.player.sendMessage(this.message, Util.DUMMY_UUID);
        }
    }

    public static void setMove(final PlayerEntity player, final int moveTime, final GlobalPos moveTo,
            final ITextComponent message, final ITextComponent failMess)
    {
        PlayerMover.setMove(player, moveTime, moveTo, message, failMess, true);
    }

    public static void setMove(final PlayerEntity player, final int moveTime, final GlobalPos moveTo,
            final ITextComponent message, final ITextComponent failMess, final boolean event)
    {
        PlayerMover.setMove(player, moveTime, moveTo, message, failMess, null, event);
    }

    public static void setMove(final PlayerEntity player, final int moveTime, final GlobalPos moveTo,
            final ITextComponent message, final ITextComponent failMess, final Predicate<Entity> callback,
            final boolean event)
    {
        if (player.getRidingEntity() != null || player.isBeingRidden())
        {
            player.sendMessage(Essentials.config.getMessage("thutessentials.tp.dismount"), Util.DUMMY_UUID);
            return;
        }
        player.getServer().runImmediately(() ->
        {
            if (!PlayerMover.toMove.containsKey(player.getUniqueID()))
            {
                long time = moveTime;
                if (time > 0)
                {
                    player.sendMessage(Essentials.config.getMessage("thutessentials.tp.tele_init"), Util.DUMMY_UUID);
                    time += player.getEntityWorld().getGameTime();
                }
                PlayerMover.toMove.put(player.getUniqueID(), new Mover(player, time, moveTo, message, failMess,
                        callback, event));
            }
        });
    }

    static Map<UUID, Mover> toMove = Maps.newHashMap();

    @SubscribeEvent
    public void playerTick(final LivingUpdateEvent tick)
    {
        if (!(tick.getEntity().world instanceof ServerWorld)) return;
        if (PlayerMover.toMove.containsKey(tick.getEntity().getUniqueID()))
        {
            final Mover mover = PlayerMover.toMove.get(tick.getEntity().getUniqueID());
            final GlobalPos playerPos = CoordinateUtls.forMob(mover.player);
            final Vector3i diff = playerPos.getPos().subtract(mover.start.getPos());
            if (tick.getEntity().getEntityWorld().getGameTime() > mover.moveTime)
            {
                mover.move();
                PlayerMover.toMove.remove(tick.getEntity().getUniqueID());
            }
            else if (diff.distanceSq(Vector3i.NULL_VECTOR) > 1 && mover.moveTime > 0)
            {
                System.out.println(diff.distanceSq(Vector3i.NULL_VECTOR));
                if (mover.failMess != null) tick.getEntity().sendMessage(mover.failMess, Util.DUMMY_UUID);
                PlayerMover.toMove.remove(tick.getEntity().getUniqueID());
                return;
            }
        }
    }

}
