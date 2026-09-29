package thut.essentials.land.claims;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import thut.essentials.land.claims.NamedVolumes.INamedVolume;

public class StructureManager
{
    private static final ReentrantLock SET_ADD_LOCK = new ReentrantLock();
    /**
     * This is a cache of loaded chunks, it is used to prevent thread lock contention when trying to look up a chunk, as
     * it seems that world.chunkExists returning true does not mean that you can just go and ask for the chunk...
     */
    private static final Map<GlobalChunkPos, Map<GlobalChunkPos, Set<INamedVolume>>> map_by_rpos = new ConcurrentHashMap<>();

    private static Map<GlobalChunkPos, Map<GlobalChunkPos, Set<INamedVolume>>> map()
    {
        return map_by_rpos;
    }

    public static List<INamedVolume> getFor(final ResourceKey<Level> dim, final BlockPos loc)
    {
        List<INamedVolume> forPos = getFor(dim, new ChunkPos(loc));
        forPos.removeIf(v -> v == null || !v.isIn(loc));
        return forPos;
    }

    public static List<INamedVolume> getFor(final ResourceKey<Level> dim, final ChunkPos loc)
    {
        final GlobalChunkPos pos = new GlobalChunkPos(dim, loc);
        var rPos = new GlobalChunkPos(pos.world, new ChunkPos(pos.pos.getRegionX(), pos.pos.getRegionZ()));
        final Set<INamedVolume> forPos = StructureManager.map().getOrDefault(rPos, Collections.emptyMap())
                .getOrDefault(pos, Collections.emptySet());
        List<INamedVolume> list;
        SET_ADD_LOCK.lock();
        list = new ArrayList<>(forPos);
        SET_ADD_LOCK.unlock();
        return list;
    }

    public static List<INamedVolume> getColliding(ResourceKey<Level> dim, INamedVolume volume)
    {
        var here = forVolume(volume, dim);
        var opts = new HashSet<INamedVolume>();
        var ourB = volume.getTotalBounds();
        AABB aabbUs = AABB.of(ourB);
        here.forEach(p -> opts.addAll(getFor(dim, p.pos)));
        var ret = opts.stream().filter(b -> {
            var otherB = b.getTotalBounds();
            AABB otherBB = AABB.of(otherB);
            if (!aabbUs.intersects(otherBB)) return false;
            var aabbI = aabbUs.intersect(otherBB);
            return NamedVolumes.computeVolume(aabbI) == 0;
        });
        return ret.toList();
    }

    public static List<GlobalChunkPos> forVolume(INamedVolume volume, ResourceKey<Level> level)
    {
        List<GlobalChunkPos> list = new ArrayList<>();
        var bounds = volume.getTotalBounds();
        bounds.intersectingChunks().forEach(pos -> {
            list.add(new GlobalChunkPos(level, pos));
        });
        return list;
    }

    protected static void addVolume(INamedVolume volume, Level level)
    {
        List<GlobalChunkPos> list = StructureManager.forVolume(volume, level.dimension());
        list.forEach(pos -> {
            var rPos = new GlobalChunkPos(pos.world, new ChunkPos(pos.pos.getRegionX(), pos.pos.getRegionZ()));
            var map = map().computeIfAbsent(rPos, k -> new ConcurrentHashMap<>());
            var set = map.computeIfAbsent(pos, k -> new HashSet<>());
            SET_ADD_LOCK.lock();
            set.add(volume);
            SET_ADD_LOCK.unlock();
        });
    }

    protected static void removeVolume(INamedVolume volume, Level level)
    {
        List<GlobalChunkPos> list = forVolume(volume, level.dimension());
        list.forEach(pos-> {
            var rPos = new GlobalChunkPos(pos.world, new ChunkPos(pos.pos.getRegionX(), pos.pos.getRegionZ()));
            if (map().containsKey(rPos))
            {
                var map = map().get(rPos);
                if(map.containsKey(pos))
                {
                    var set = map.get(pos);
                    SET_ADD_LOCK.lock();
                    set.remove(volume);
                    if (set.isEmpty()) map.remove(pos);
                    SET_ADD_LOCK.unlock();
                }
                if(map.isEmpty()) map().remove(rPos);
            }
        });
    }

    public static boolean hasVolumes(ResourceKey<Level> dimension, int regionX, int regionZ)
    {
        final GlobalChunkPos gpos = new GlobalChunkPos(dimension, new ChunkPos(regionX, regionZ));
        return map().containsKey(gpos);
    }

    @SubscribeEvent
    public static void onChunkLoad(final ChunkEvent.Load evt)
    {
        // TODO use a tree lookup or such in the namedVolumes to load them as needed
    }

    @SubscribeEvent
    public static void onChunkUnload(final ChunkEvent.Unload evt)
    {
        // TODO use a tree lookup or such in the namedVolumes to remove them as needed
    }

    public static void clear()
    {
        map_by_rpos.clear();
    }
}