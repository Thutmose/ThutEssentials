package thut.essentials.defuzz;

import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Sets;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.stats.Stats;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import thut.essentials.Essentials;
import thut.essentials.land.LandManager.KGobalPos;
import thut.essentials.util.PlayerDataHandler;
import thut.essentials.util.PlayerMover;

public class SpawnDefuzzer
{

    public static int DEFUZZSENS = 500;

    final static Set<UUID> logins = Sets.newHashSet();

    private static boolean shouldDefuz(final ServerPlayer player, final boolean respawn)
    {
        if (!Essentials.config.defuzzKey.isEmpty())
        {
            final CompoundTag tag = PlayerDataHandler.getCustomDataTag(player);
            if (!tag.getString("__defuzz_key__").equals(Essentials.config.defuzzKey)) return true;
        }
        if (!respawn)
        {
            final int num = player.getStats().getValue(Stats.CUSTOM.get(Stats.WALK_ONE_CM)) + player.getStats()
                    .getValue(Stats.CUSTOM.get(Stats.FALL_ONE_CM)) + player.getStats().getValue(Stats.CUSTOM.get(
                            Stats.SWIM_ONE_CM));
            if (num > SpawnDefuzzer.DEFUZZSENS) return false;
        }
        return player.getRespawnPosition() == null;
    }

    @SubscribeEvent
    public static void deFuzzRespawn(final PlayerRespawnEvent event)
    {
        if (!Essentials.config.defuzz) return;
        if (!(event.getPlayer() instanceof ServerPlayer)) return;
        final ServerLevel world = event.getPlayer().getServer().getLevel(Essentials.config.spawnDimension);
        final ServerPlayer player = (ServerPlayer) event.getPlayer();
        final BlockPos worldSpawn = world.getSharedSpawnPos();
        if (SpawnDefuzzer.shouldDefuz(player, true))
        {
            PlayerDataHandler.getCustomDataTag(player).putString("__defuzz_key__", Essentials.config.defuzzKey);
            PlayerDataHandler.saveCustomData(player);
            PlayerMover.setMove(player, 0, KGobalPos.getPosition(Essentials.config.spawnDimension, worldSpawn), null,
                    null, false);
        }
    }

    @SubscribeEvent
    /**
     * This is to handle the initial connection of the player to the server.
     *
     * @param evt
     */
    public static void EntityUpdate(final LivingUpdateEvent evt)
    {
        if (!Essentials.config.defuzz) return;
        if (SpawnDefuzzer.logins.contains(evt.getEntity().getUUID()) && evt
                .getEntity() instanceof ServerPlayer)
        {
            final ServerPlayer player = (ServerPlayer) evt.getEntity();
            SpawnDefuzzer.logins.remove(evt.getEntity().getUUID());
            final ServerLevel world = player.getServer().getLevel(Essentials.config.spawnDimension);
            final BlockPos worldSpawn = world.getSharedSpawnPos();
            if (SpawnDefuzzer.shouldDefuz(player, false))
            {
                PlayerDataHandler.getCustomDataTag(player).putString("__defuzz_key__", Essentials.config.defuzzKey);
                PlayerDataHandler.saveCustomData(player);
                PlayerMover.setMove(player, 0, KGobalPos.getPosition(Essentials.config.spawnDimension, worldSpawn),
                        null, null, false);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void deFuzzSpawn(final PlayerLoggedInEvent event)
    {
        if (!Essentials.config.defuzz) return;
        if (event.getPlayer() instanceof ServerPlayer)
        {
            final ServerPlayer player = (ServerPlayer) event.getPlayer();
            Essentials.LOGGER.info("Login detected, adding player to logins for defuzz.");
            SpawnDefuzzer.logins.add(player.getUUID());
        }
    }
}
