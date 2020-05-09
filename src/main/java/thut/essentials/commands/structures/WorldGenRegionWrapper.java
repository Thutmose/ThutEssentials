package thut.essentials.commands.structures;

import java.util.List;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.Heightmap.Type;
import net.minecraft.world.gen.WorldGenRegion;
import net.minecraft.world.server.ServerWorld;

public class WorldGenRegionWrapper extends WorldGenRegion
{
    public final ServerWorld world;

    public WorldGenRegionWrapper(final ServerWorld world, final List<IChunk> iChunks)
    {
        super(world, iChunks);
        this.world = world;
    }

    @Override
    public boolean setBlockState(final BlockPos pos, final BlockState newState, final int flags)
    {
        return this.world.setBlockState(pos, newState, flags);
    }

    @Override
    public IChunk getChunk(final int x, final int z, final ChunkStatus requiredStatus, final boolean nonnull)
    {
        return this.world.getChunk(x, z, requiredStatus, nonnull);
    }

    @Override
    public int getHeight(final Type heightmapType, final int x, final int z)
    {
        return super.getHeight(heightmapType, x, z);
    }

    @Override
    public boolean addEntity(final Entity entityIn)
    {
        return this.world.addEntity(entityIn);
    }

}
