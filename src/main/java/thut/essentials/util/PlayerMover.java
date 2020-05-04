package thut.essentials.util;

import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import com.google.common.collect.Maps;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import thut.essentials.Essentials;
import thut.essentials.events.MoveEvent;
import thut.essentials.util.Transporter.Vector4;

public class PlayerMover

{
    public static ITextComponent INTERUPTED = new TranslationTextComponent("thutessentials.tp.standstill");

    private static Vector4 offset = new Vector4(0.5, 0.5, 0.5, 0);

    private static class Mover
    {
        final long              moveTime;
        final PlayerEntity      player;
        final int               dimension;
        final BlockPos          moveTo;
        final Vector4           start;
        final ITextComponent    message;
        final ITextComponent    failMess;
        final boolean           event;
        final Predicate<Entity> callback;

        public Mover(final PlayerEntity player, final long moveTime, final int dimension, final BlockPos moveTo,
                final ITextComponent message, final ITextComponent failMess, final Predicate<Entity> callback,
                final boolean event)
        {
            this.player = player;
            this.dimension = dimension;
            this.moveTime = moveTime;
            this.moveTo = moveTo;
            this.message = message;
            this.failMess = failMess;
            this.event = event;
            this.callback = callback;
            this.start = new Vector4(player.posX, player.posY, player.posZ, dimension);
        }

        private void move()
        {
            if (this.event) MinecraftForge.EVENT_BUS.post(new MoveEvent(this.player));
            final Vector4 dest = new Vector4(this.moveTo, this.dimension);
            dest.add(PlayerMover.offset);
            if (Essentials.config.log_teleports) Essentials.LOGGER.trace("TP: " + this.player.getUniqueID() + " "
                    + this.player.getName() + " from: " + this.start + " to " + this.moveTo);
            final Entity player1 = Transporter.transferTo(this.player, dest);
            if (this.callback != null) this.callback.test(player1);
            if (this.message != null) player1.sendMessage(this.message);
        }
    }

    public static void setMove(final PlayerEntity player, final int moveTime, final int dimension,
            final BlockPos moveTo, final ITextComponent message, final ITextComponent failMess)
    {
        PlayerMover.setMove(player, moveTime, dimension, moveTo, message, failMess, true);
    }

    public static void setMove(final PlayerEntity player, final int moveTime, final int dimension,
            final BlockPos moveTo, final ITextComponent message, final ITextComponent failMess, final boolean event)
    {
        PlayerMover.setMove(player, moveTime, dimension, moveTo, message, failMess, null, event);
    }

    public static void setMove(final PlayerEntity player, final int moveTime, final int dimension,
            final BlockPos moveTo, final ITextComponent message, final ITextComponent failMess,
            final Predicate<Entity> callback, final boolean event)
    {
        if (player.getRidingEntity() != null || player.isBeingRidden())
        {
            player.sendMessage(Essentials.config.getMessage("thutessentials.tp.dismount"));
            return;
        }
        player.getServer().runImmediately(() ->
        {
            if (!PlayerMover.toMove.containsKey(player.getUniqueID()))
            {
                long time = moveTime;
                if (time > 0)
                {
                    player.sendMessage(Essentials.config.getMessage("thutessentials.tp.tele_init"));
                    time += player.getEntityWorld().getGameTime();
                }
                PlayerMover.toMove.put(player.getUniqueID(), new Mover(player, time, dimension, moveTo, message,
                        failMess, callback, event));
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
            final Vector4 loc = new Vector4(mover.player.posX, mover.player.posY, mover.player.posZ,
                    mover.player.dimension.getId());
            final Vector4 diff = new Vector4(mover.start.x, mover.start.y, mover.start.z, mover.player.dimension
                    .getId());
            diff.sub(loc);
            if (tick.getEntity().getEntityWorld().getGameTime() > mover.moveTime)
            {
                mover.move();
                PlayerMover.toMove.remove(tick.getEntity().getUniqueID());
            }
            else if (diff.lengthSquared() > 0.0 && mover.moveTime > 0)
            {
                if (mover.failMess != null) tick.getEntity().sendMessage(mover.failMess);
                PlayerMover.toMove.remove(tick.getEntity().getUniqueID());
                return;
            }
        }
    }

}
