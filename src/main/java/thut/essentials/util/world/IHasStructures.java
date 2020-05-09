package thut.essentials.util.world;

import java.util.Collection;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MutableBoundingBox;

public interface IHasStructures
{
    void putStructure(ResourceLocation key, MutableBoundingBox box);

    Collection<ResourceLocation> getStructures(BlockPos pos);

    void removeStructures(ResourceLocation key, MutableBoundingBox box);
}
