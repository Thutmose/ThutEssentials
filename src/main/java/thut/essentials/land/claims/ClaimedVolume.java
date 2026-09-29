package thut.essentials.land.claims;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.common.util.INBTSerializable;
import thut.essentials.land.claims.NamedVolumes.INamedVolume;

import java.util.List;

public class ClaimedVolume implements INamedVolume, INBTSerializable<CompoundTag>
{
    BoundingBox bounds;
    public final ClaimInfo info;
    public String extraKey = "";

    public ClaimedVolume()
    {
        this.info = new ClaimInfo();
    }

    public ClaimedVolume(BoundingBox bounds, ClaimInfo info)
    {
        this.info = info;
        this.bounds = bounds;
    }

    public ClaimedVolume(ChunkPos chunk, int minY, int maxY)
    {
        var lower = new BlockPos(chunk.getMinBlockX(), minY, chunk.getMinBlockZ());
        var upper = new BlockPos(chunk.getMaxBlockX() + 1, maxY, chunk.getMaxBlockZ() + 1);
        this.bounds = BoundingBox.fromCorners(upper, lower);
        this.info = new ClaimInfo();
    }

    public BoundingBox shouldMerge(ClaimedVolume other)
    {
        if (other == this) return null;
        if (!other.info.owner.equals(this.info.owner)) return null;
        var otherBounds = other.getTotalBounds();
        var vO = NamedVolumes.computeVolume(otherBounds);
        var vU = NamedVolumes.computeVolume(bounds);
        var boundList = List.of(otherBounds, bounds);
        var tBounds = BoundingBox.encapsulatingBoxes(boundList);
        if (tBounds.isPresent())
        {
            otherBounds = tBounds.get();
            var vT1 = NamedVolumes.computeVolume(otherBounds);
            return vT1 == vO + vU ? otherBounds : null;
        }
        return null;
    }

    @Override
    public String getName()
    {
        if(info.name.isBlank()) info.name = "claim";
        return info.name;
    }

    @Override
    public String getKey()
    {
        return "thutessentials:claim";
    }

    @Override
    public List<NamedVolumes.INamedPart> getParts()
    {
        return List.of();
    }

    @Override
    public boolean isIn(BlockPos pos)
    {
        return this.getTotalBounds().isInside(pos);
    }

    @Override
    public BoundingBox getTotalBounds()
    {
        return bounds;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider)
    {
        CompoundTag tag = new CompoundTag();
        tag.put("info", this.info.serializeNBT(provider));
        tag.put("bounds", BoundingBox.CODEC.encodeStart(NbtOps.INSTANCE, this.bounds).getOrThrow());
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt)
    {
        bounds = BoundingBox.CODEC.decode(NbtOps.INSTANCE, nbt.get("bounds")).result().get().getFirst();
        this.info.deserializeNBT(provider, nbt.getCompound("info"));
    }

    public int getColour()
    {
        return info.colour;
    }
}
