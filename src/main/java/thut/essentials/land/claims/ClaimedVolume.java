package thut.essentials.land.claims;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.INBTSerializable;
import thut.essentials.api.level.NamedVolumes;
import thut.essentials.api.level.NamedVolumes.INamedVolume;

import java.util.List;
import java.util.Objects;

public class ClaimedVolume implements INamedVolume, INBTSerializable<CompoundTag>
{
    AABB aabb;
    private BoundingBox bounds;
    public final ClaimInfo info;
    public String extraKey = "";

    public ClaimedVolume()
    {
        this.info = new ClaimInfo();
    }

    public ClaimedVolume(BoundingBox bounds, ClaimInfo info)
    {
        this.info = info;
        this.setBounds(bounds);
    }

    public ClaimedVolume(ChunkPos chunk, Level level, ClaimInfo info)
    {
        this.info = info;
        var bounds = new BoundingBox(chunk.getMinBlockX(), level.getMinBuildHeight(), chunk.getMinBlockZ(),
                chunk.getMaxBlockX(), level.getMaxBuildHeight(), chunk.getMaxBlockZ());
        this.setBounds(bounds);
    }

    public BoundingBox shouldMerge(ClaimedVolume other)
    {
        if (other == this) return null;
        if (!other.info.owner.equals(this.info.owner)) return null;
        var otherBounds = other.getTotalBounds();
        var vO = other.computeVolume();
        var vU = this.computeVolume();
        var boundList = List.of(otherBounds, bounds);
        var tBounds = BoundingBox.encapsulatingBoxes(boundList);
        if (tBounds.isPresent())
        {
            otherBounds = tBounds.get();
            var vT1 = NamedVolumes.computeVolume(AABB.of(otherBounds));
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
        this.setBounds(BoundingBox.CODEC.decode(NbtOps.INSTANCE, nbt.get("bounds")).result().get().getFirst());
        this.info.deserializeNBT(provider, nbt.getCompound("info"));
    }

    private void setBounds(BoundingBox box)
    {
        this.bounds = box;
        this.aabb = AABB.of(box);
    }

    @Override
    public long computeVolume()
    {
        return (long) NamedVolumes.computeVolume(aabb);
    }

    public int getColour()
    {
        return info.colour;
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof ClaimedVolume that)) return false;
        return Objects.equals(bounds, that.bounds) && Objects.equals(info, that.info);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(bounds, info);
    }
}
