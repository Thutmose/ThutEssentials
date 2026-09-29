package thut.essentials.compat.xaeros;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import thut.essentials.land.claims.ClaimEvent;
import thut.essentials.land.claims.GlobalChunkPos;
import thut.essentials.land.claims.StructureManager;
import xaero.map.WorldMapSession;
import xaero.map.world.MapDimension;

import java.util.HashSet;
import java.util.Set;

public class ClaimSync
{
    public static void init()
    {
        NeoForge.EVENT_BUS.register(ClaimSync.class);
    }

    @SubscribeEvent
    public static void onClaim(ClaimEvent.Unclaim event)
    {
        _onClaim(event);
    }

    @SubscribeEvent
    public static void onClaim(ClaimEvent.Claim event)
    {
        _onClaim(event);
    }

    public static void _onClaim(ClaimEvent event)
    {
        if(event.level instanceof ServerLevel) return;
        // need to force a refresh of map.
        var list = StructureManager.forVolume(event.volume, event.level.dimension());
        WorldMapSession session = WorldMapSession.getCurrentSession();
        MapDimension mapDim = session.getMapProcessor().getMapWorld()
                .getDimension(ResourceKey.create(Registries.DIMENSION, event.level.dimension().location()));
        if (mapDim == null) return;
        Set<GlobalChunkPos> rSet = new HashSet<>();
        list.forEach(pos -> {
            var rPos = new GlobalChunkPos(pos.world, new ChunkPos(pos.pos.getRegionX(), pos.pos.getRegionZ()));
            if (rSet.add(rPos))
            {
                int regionX = rPos.pos.x;
                int regionZ = rPos.pos.z;
                for (int i = -1; i < 2; ++i)
                {
                    for (int j = -1; j < 2; ++j)
                    {
                        if (i == 0 && j == 0 || i * i != j * j)
                        {
                            mapDim.getHighlightHandler().clearCachedHash(regionX + i, regionZ + j);
                        }
                    }
                }
            }
        });
    }
}
