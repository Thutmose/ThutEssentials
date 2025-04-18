package thut.essentials.land;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import thut.essentials.Essentials;
import thut.essentials.land.ClaimedCapability.ClaimInfo;
import thut.essentials.land.ClaimedCapability.ClaimSegment;
import thut.essentials.land.ClaimedCapability.IClaimed;

public class ChunkClaim implements IClaimed
{
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
    public CompoundTag serializeNBT(HolderLookup.Provider var1)
    {
        final CompoundTag tag = new CompoundTag();
        tag.put("info", this.info.serializeNBT(var1));
        for (final Entry<ClaimSegment> s : this.claims.int2ObjectEntrySet())
        {
            final ClaimSegment claim = s.getValue();
            if (!LandManager.isWild(LandManager.getInstance().getTeamForLand(claim.owner)))
                tag.put("seg_" + s.getIntKey(), claim.serializeNBT(var1));
        }
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider var1, final CompoundTag nbt)
    {
        this.info.deserializeNBT(var1, nbt.getCompound("info"));
        for (final String key : nbt.getAllKeys())
            if (key.startsWith("seg_")) try
            {
                final int i = Integer.parseInt(key.replace("seg_", ""));
                final ClaimSegment claim = new ClaimSegment();
                claim.deserializeNBT(var1, (IntArrayTag) nbt.get(key));
                if (!LandManager.isWild(LandManager.getInstance().getTeamForLand(claim.owner)))
                    this.claims.put(i, claim);
            }
            catch (final Exception e)
            {
                Essentials.LOGGER.error(e);
            }
    }
}