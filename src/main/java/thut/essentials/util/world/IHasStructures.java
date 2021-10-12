package thut.essentials.util.world;

import java.util.Collection;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public interface IHasStructures
{
    void putStructure(ResourceLocation key, BoundingBox box);

    Collection<ResourceLocation> getStructures(BlockPos pos);

    void removeStructures(ResourceLocation key, BoundingBox box);
}
