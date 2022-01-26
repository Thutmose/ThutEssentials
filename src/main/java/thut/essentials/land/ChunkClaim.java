package thut.essentials.land;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import thut.essentials.Essentials;
import thut.essentials.land.ClaimedCapability.ClaimInfo;
import thut.essentials.land.ClaimedCapability.ClaimSegment;
import thut.essentials.land.ClaimedCapability.IClaimed;

public class ChunkClaim implements IClaimed, ICapabilitySerializable<CompoundTag>
{
    private final LazyOptional<IClaimed> holder = LazyOptional.of(() -> this);

    private final ClaimInfo info = new ClaimInfo();

    private final Int2ObjectArrayMap<ClaimSegment> claims = new Int2ObjectArrayMap<>(24);

    public ChunkClaim()
    {}

    @Override
    public ClaimSegment getSegment(final int yIndex)
    {
        ClaimSegment claim = this.claims.get(yIndex);
        if (claim == null) this.claims.put(yIndex, claim = new ClaimSegment());
        return claim;
    }

    @Override
    public ClaimInfo getInfo()
    {
        return this.info;
    }

    @Override
    public <T> LazyOptional<T> getCapability(final Capability<T> cap, final Direction side)
    {
        return ClaimedCapability.CAPABILITY.orEmpty(cap, this.holder);
    }

    @Override
    public CompoundTag serializeNBT()
    {
        final CompoundTag tag = new CompoundTag();
        tag.put("info", this.info.serializeNBT());
        for (final Entry<ClaimSegment> s : this.claims.int2ObjectEntrySet())
        {
            final ClaimSegment claim = s.getValue();
            if (!LandManager.isWild(LandManager.getInstance().getTeamForLand(claim.owner)))
                tag.put("seg_" + s.getIntKey(), claim.serializeNBT());
        }
        return tag;
    }

    @Override
    public void deserializeNBT(final CompoundTag nbt)
    {
        this.info.deserializeNBT(nbt.getCompound("info"));
        for (final String key : nbt.getAllKeys()) if (key.startsWith("seg_")) try
        {
            final int i = Integer.parseInt(key.replace("seg_", ""));
            final ClaimSegment claim = new ClaimSegment();
            claim.deserializeNBT((IntArrayTag) nbt.get(key));
            if (!LandManager.isWild(LandManager.getInstance().getTeamForLand(claim.owner))) this.claims.put(i, claim);
        }
        catch (final Exception e)
        {
            Essentials.LOGGER.error(e);
        }
    }
}