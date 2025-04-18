package thut.essentials.util.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.levelgen.Heightmap.Types;

public class WorldGenRegionWrapper extends WorldGenRegion
{
    public final ServerLevel world;

    public WorldGenRegionWrapper(ServerLevel world, StaticCache2D<GenerationChunkHolder> pCache,
            ChunkStep pGeneratingStep, ChunkAccess pCenter)
    {
        super(world, pCache, pGeneratingStep, pCenter);
        this.world = world;
    }

    @Override
    public boolean setBlock(final BlockPos pos, final BlockState newState, final int flags)
    {
        return this.world.setBlock(pos, newState, flags);
    }

    @Override
    public ChunkAccess getChunk(final int x, final int z, final ChunkStatus requiredStatus, final boolean nonnull)
    {
        return this.world.getChunk(x, z, requiredStatus, nonnull);
    }

    @Override
    public int getHeight(final Types heightmapType, final int x, final int z)
    {
        return super.getHeight(heightmapType, x, z);
    }

    @Override
    public boolean addFreshEntity(final Entity entityIn)
    {
        return this.world.addFreshEntity(entityIn);
    }

}
