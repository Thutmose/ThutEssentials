package thut.essentials.util.world;

import java.util.List;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.server.level.ServerLevel;

public class WorldGenRegionWrapper extends WorldGenRegion
{
    public final ServerLevel world;

    public WorldGenRegionWrapper(final ServerLevel world, final List<ChunkAccess> iChunks)
    {
        super(world, iChunks, ChunkStatus.FULL, 5);
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
