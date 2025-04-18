package thut.essentials.util;

import com.google.common.collect.Maps;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import thut.essentials.Essentials;
import thut.essentials.events.MoveEvent;
import thut.essentials.util.teleporting.TeleDest;
import thut.essentials.util.teleporting.ThutTeleporter;

import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

public class PlayerMover

{
    public static Component INTERUPTED;

    private static class Mover
    {
        final long moveTime;
        final Player player;
        final GlobalPos moveTo;
        final GlobalPos start;
        final Component message;
        final Component failMess;
        final boolean event;
        final Predicate<Entity> callback;

        public Mover(final Player player, final long moveTime, final GlobalPos moveTo, final Component message,
                final Component failMess, final Predicate<Entity> callback, final boolean event)
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
            if (this.moveTo == null) return;
            if (this.start == null) return;
            if (this.player == null || !this.player.isAddedToLevel()) return;
            if (this.event) NeoForge.EVENT_BUS.post(new MoveEvent(this.player));
            if (Essentials.config.log_teleports)
                InventoryLogger.log("Teleport from {} {} to {} {} for {} {}", CoordinateUtls.chunkPos(this.start),
                        this.start.dimension().location(), this.start.pos(), this.moveTo.dimension().location(),
                        this.moveTo.pos(), this.player.getUUID(), this.player.getName().getString());
            final TeleDest dest = new TeleDest();
            dest.setLoc(this.moveTo, this.moveTo.pos().getCenter());
            try
            {
                ThutTeleporter.transferTo(this.player, dest);
            }
            catch (final Exception e)
            {
                Essentials.LOGGER.error("Error teleporting player!");
                Essentials.LOGGER.error(e);
            }
            if (this.callback != null) this.callback.test(this.player);
            if (this.message != null) ChatHelper.sendSystemMessage(player, this.message);
        }
    }

    public static void setMove(final Player player, final int moveTime, final GlobalPos moveTo, final Component message,
            final Component failMess)
    {
        PlayerMover.setMove(player, moveTime, moveTo, message, failMess, true);
    }

    public static void setMove(final Player player, final int moveTime, final GlobalPos moveTo, final Component message,
            final Component failMess, final boolean event)
    {
        PlayerMover.setMove(player, moveTime, moveTo, message, failMess, null, event);
    }

    public static void setMove(final Player player, final int moveTime, final GlobalPos moveTo, final Component message,
            final Component failMess, final Predicate<Entity> callback, final boolean event)
    {
        if (player.getVehicle() != null || player.isVehicle())
        {
            ChatHelper.sendSystemMessage(player, Essentials.config.getMessage("thutessentials.tp.dismount"));
            return;
        }
        player.getServer().executeBlocking(() -> {
            if (!PlayerMover.toMove.containsKey(player.getUUID()))
            {
                long time = moveTime;
                if (time > 0)
                {
                    ChatHelper.sendSystemMessage(player, Essentials.config.getMessage("thutessentials.tp.tele_init"));
                    time += player.getCommandSenderWorld().getGameTime();
                }
                PlayerMover.toMove.put(player.getUUID(),
                        new Mover(player, time, moveTo, message, failMess, callback, event));
            }
        });
    }

    static Map<UUID, Mover> toMove = Maps.newHashMap();

    @SubscribeEvent
    public void playerTick(final PlayerTickEvent.Post tick)
    {
        if (!(tick.getEntity().level() instanceof ServerLevel) || !(tick.getEntity() instanceof ServerPlayer player))
            return;
        if (PlayerMover.toMove.containsKey(player.getUUID()))
        {
            final Mover mover = PlayerMover.toMove.get(player.getUUID());
            final GlobalPos playerPos = CoordinateUtls.forMob(mover.player);
            final Vec3i diff = playerPos.pos().subtract(mover.start.pos());
            if (player.getCommandSenderWorld().getGameTime() > mover.moveTime)
            {
                mover.move();
                PlayerMover.toMove.remove(player.getUUID());
            }
            else if (diff.distSqr(Vec3i.ZERO) > 1 && mover.moveTime > 0)
            {
                if (mover.failMess != null) ChatHelper.sendSystemMessage(player, mover.failMess);
                PlayerMover.toMove.remove(player.getUUID());
                return;
            }
        }
    }

}
