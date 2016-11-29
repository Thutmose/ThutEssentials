package thut.essentials.defuzz;

import com.mojang.authlib.GameProfile;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ServerConnectionFromClientEvent;
import thut.essentials.commands.misc.Spawn.PlayerMover;

public class SpawnDefuzzer
{

    @SubscribeEvent
    public void deFuzzRespawn(PlayerRespawnEvent event)
    {
        if (event.player.getEntityWorld().isRemote) return;
        BlockPos worldSpawn = event.player.getEntityWorld().getSpawnPoint();
        BlockPos playerSpawn = event.player.getBedLocation();
        if (playerSpawn == null)
        {
            PlayerMover.setMove(event.player, event.player.getEntityWorld().provider.getDimension(), worldSpawn, null);
        }
    }

    @SubscribeEvent
    public void deFuzzSpawn(ServerConnectionFromClientEvent event)
    {
        if (event.getHandler() instanceof NetHandlerPlayServer)
        {
            MinecraftServer mcServer = FMLCommonHandler.instance().getMinecraftServerInstance();
            EntityPlayer player = ((NetHandlerPlayServer) event.getHandler()).playerEntity;
            BlockPos worldSpawn = null;
            World playerWorld = mcServer.worldServerForDimension(player.dimension);
            GameProfile gameprofile = player.getGameProfile();
            PlayerProfileCache playerprofilecache = mcServer.getPlayerProfileCache();
            GameProfile gameprofile1 = playerprofilecache.getProfileByUUID(gameprofile.getId());
            if (gameprofile1 == null && playerWorld != null)
            {
                worldSpawn = playerWorld.provider.getSpawnPoint();
            }

            if (worldSpawn != null)
            {
                PlayerMover.setMove(player, player.getEntityWorld().provider.getDimension(), worldSpawn, null);
            }
        }
    }
}
