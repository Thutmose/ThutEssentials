package thut.essentials.compat.xaeros;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import thut.essentials.land.claims.ClaimedVolume;
import thut.essentials.land.claims.NamedVolumes;
import thut.essentials.land.claims.StructureManager;
import xaero.map.highlight.ChunkHighlighter;

import java.util.List;
import java.util.Objects;

public class HighlightHandler extends ChunkHighlighter
{
    private Component cachedTooltip;
    private String cachedForCustomName;
    private int cachedForClaimsColor;
    private ResourceLocation cachedForDimensionId;
    Object vols_cache;

    public HighlightHandler()
    {
        super(true);
    }

    @Override
    public boolean regionHasHighlights(ResourceKey<Level> dimension, int regionX, int regionZ)
    {
        // For now, just show if it is in the chunk middle at sea level?
        return StructureManager.hasVolumes(dimension, regionX, regionZ);
    }

    private NamedVolumes.INamedVolume getClaim(ResourceKey<Level> dimension, int chunkX, int chunkZ)
    {
        int posY = 63;
        var player = Minecraft.getInstance().player;
        if (player != null && player.level().dimension() == dimension)
        {
            posY = (int) player.getY();
        }
        BlockPos pos = new ChunkPos(chunkX, chunkZ).getBlockAt(8, posY, 8);
        var vols = StructureManager.getFor(dimension, pos);
        if (vols.isEmpty()) return null;
        return vols.getFirst();
    }

    @Override
    protected int[] getColors(ResourceKey<Level> dimension, int chunkX, int chunkZ)
    {
        var vol = getClaim(dimension, chunkX, chunkZ);
        if (vol == null) return null;
        int colour = this.getClaimsColor(vol);
        int claimColorFormatted = (colour & 255) << 24 | (colour >> 8 & 255) << 16 | (colour >> 16 & 255) << 8;
        int fillOpacity = 0x0F;
        int borderOpacity = 0xF0;
        int centerColor = claimColorFormatted | fillOpacity;
        int sideColor = claimColorFormatted | borderOpacity;
        var topClaim = getClaim(dimension, chunkX, chunkZ - 1);
        var rightClaim = getClaim(dimension, chunkX + 1, chunkZ);
        var bottomClaim = getClaim(dimension, chunkX, chunkZ + 1);
        var leftClaim = getClaim(dimension, chunkX - 1, chunkZ);
        this.resultStore[0] = centerColor;
        this.resultStore[1] = sameClaimer(topClaim, vol) ? centerColor : sideColor;
        this.resultStore[2] = sameClaimer(rightClaim, vol) ? centerColor : sideColor;
        this.resultStore[3] = sameClaimer(bottomClaim, vol) ? centerColor : sideColor;
        this.resultStore[4] = sameClaimer(leftClaim, vol) ? centerColor : sideColor;
        return this.resultStore;
    }

    @Override
    public int calculateRegionHash(ResourceKey<Level> dimension, int regionX, int regionZ)
    {
        long accumulator = 0L;
        for (int i = 0; i < 32; ++i)
        {
            for (int j = 0; j < 32; ++j)
            {
                int x = regionX * 32 + i;
                int z = regionZ * 32 + j;
                var vol = getClaim(dimension, x, z);
                accumulator = this.accountClaim(accumulator, vol);
            }
        }
        return (int) (accumulator >> 32) * 37 + (int) (accumulator & -1L);
    }

    private long accountClaim(long accumulator, NamedVolumes.INamedVolume volume)
    {
        if (volume != null && volume instanceof ClaimedVolume vol && vol.info.owner != null)
        {
            var id = vol.info.owner;
            accumulator += id.getLeastSignificantBits();
            accumulator *= 37L;
            accumulator += id.getMostSignificantBits();
            accumulator *= 37L;
            accumulator += this.getClaimsColor(vol);
        }
        accumulator *= 37L;
        return accumulator;
    }

    @Override
    public boolean chunkIsHighlit(ResourceKey<Level> dimension, int chunkX, int chunkZ)
    {
        return this.getClaim(dimension, chunkX, chunkZ) != null;
    }

    @Override
    public Component getChunkHighlightSubtleTooltip(ResourceKey<Level> dimension, int chunkX, int chunkZ)
    {
        var vol = getClaim(dimension, chunkX, chunkZ);
        if (vol == null)
        {
            return null;
        }
        else
        {
            var dimid = dimension.location();
            String customName = this.getClaimsCustomName(vol);
            int actualClaimsColor = this.getClaimsColor(vol);
            int claimsColor = actualClaimsColor | 0xFF000000;
            var id = vol instanceof ClaimedVolume v ? v.info.owner : null;
            if (!Objects.equals(vols_cache, id) || this.cachedForClaimsColor != claimsColor || !Objects.equals(
                    customName, this.cachedForCustomName) || !dimid.equals(this.cachedForDimensionId))
            {
                this.cachedTooltip = Component.literal("□ ").withStyle((s) -> s.withColor(claimsColor));
                this.cachedTooltip.getSiblings().add(Component.literal(customName).withStyle(ChatFormatting.WHITE));
                this.cachedForCustomName = customName;
                this.cachedForClaimsColor = claimsColor;
                this.vols_cache = id;
                this.cachedForDimensionId = dimid;
            }
            return this.cachedTooltip;
        }
    }

    @Override
    public Component getChunkHighlightBluntTooltip(ResourceKey<Level> dimension, int chunkX, int chunkZ)
    {
        return null;
    }

    @Override
    public void addMinimapBlockHighlightTooltips(List<Component> list, ResourceKey<Level> dimension, int blockX,
            int blockZ, int width)
    {
    }

    private boolean sameClaimer(NamedVolumes.INamedVolume a, NamedVolumes.INamedVolume b)
    {
        if (a == null || b == null) return false;
        if (!(a instanceof ClaimedVolume v && b instanceof ClaimedVolume u)) return false;
        return v.info.owner.equals(u.info.owner);
    }

    private String getClaimsCustomName(NamedVolumes.INamedVolume volume)
    {
        return volume.getName();
    }

    private int getClaimsColor(NamedVolumes.INamedVolume volume)
    {
        if (volume instanceof ClaimedVolume v) return v.getColour();
        return volume.getName().hashCode() | 0xFF000000;
    }
}
