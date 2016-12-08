package thut.essentials.defuzz;

import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Sets;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.stats.StatList;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ServerConnectionFromClientEvent;
import thut.essentials.commands.misc.Spawn.PlayerMover;

public class SpawnDefuzzer
{
    public static int      DEFUZZSENS = 10000;

    final static Set<UUID> logins     = Sets.newHashSet();

    @SubscribeEvent
    public void deFuzzRespawn(PlayerRespawnEvent event)
    {
        if (event.player.getEntityWorld().isRemote) return;
        BlockPos worldSpawn = event.player.getEntityWorld().getSpawnPoint();
        BlockPos playerSpawn = event.player.getBedLocation();
        if (playerSpawn == null)
        {
            PlayerMover.setMove(event.player, 0, event.player.getEntityWorld().provider.getDimension(), worldSpawn,
                    null, null);
        }
    }

    @SubscribeEvent
    public void EntityUpdate(LivingUpdateEvent evt)
    {
        if (logins.contains(evt.getEntity().getUniqueID()) && evt.getEntity() instanceof EntityPlayerMP)
        {
            EntityPlayerMP player = (EntityPlayerMP) evt.getEntity();
            if (player.getEntityWorld().isRemote) return;
            int num = player.getStatFile().readStat(StatList.WALK_ONE_CM)
                    + player.getStatFile().readStat(StatList.FALL_ONE_CM)
                    + player.getStatFile().readStat(StatList.SWIM_ONE_CM);
            logins.remove(evt.getEntity().getUniqueID());
            BlockPos worldSpawn = player.getEntityWorld().getSpawnPoint();
            BlockPos playerSpawn = player.getBedLocation();
            if (num > DEFUZZSENS) return;
            if (playerSpawn == null)
            {
                PlayerMover.setMove(player, 0, player.getEntityWorld().provider.getDimension(), worldSpawn, null, null);
            }
        }
    }

    @SubscribeEvent
    public void deFuzzSpawn(ServerConnectionFromClientEvent event)
    {
        if (event.getHandler() instanceof NetHandlerPlayServer)
        {
            EntityPlayerMP player = ((NetHandlerPlayServer) event.getHandler()).playerEntity;
            logins.add(player.getUniqueID());
        }
    }
}
