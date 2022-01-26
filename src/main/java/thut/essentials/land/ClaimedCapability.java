package thut.essentials.land;

import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Sets;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import thut.essentials.Essentials;

public class ClaimedCapability
{
    public static class ClaimInfo implements INBTSerializable<CompoundTag>
    {
        public Set<UUID> publicMobs = Sets.newHashSet();
        public Set<UUID> protectedMobs = Sets.newHashSet();

        public Set<BlockPos> publicBlocks = Sets.newHashSet();

        @Override
        public CompoundTag serializeNBT()
        {
            final CompoundTag tag = new CompoundTag();
            final ListTag mobListPub = new ListTag();
            this.publicMobs.forEach(uuid -> mobListPub.add(NbtUtils.createUUID(uuid)));
            tag.put("public_mobs", mobListPub);
            final ListTag mobListProt = new ListTag();
            this.protectedMobs.forEach(uuid -> mobListProt.add(NbtUtils.createUUID(uuid)));
            tag.put("protected_mobs", mobListProt);
            final ListTag pubBlocks = new ListTag();
            this.publicBlocks.forEach(b -> pubBlocks.add(NbtUtils.writeBlockPos(b)));
            tag.put("public_blocks", pubBlocks);
            return tag;
        }

        @Override
        public void deserializeNBT(final CompoundTag nbt)
        {

        }
    }

    public static class ClaimSegment implements INBTSerializable<IntArrayTag>
    {
        /**
         * This is the UUID of the TeamLand associated with this claim, not the
         * Team, or the Player, this allows resetting team land without loading
         * the chunks. When this is first initialized, it will check to see if
         * any teams actually own here, via legacy means, and if so, will set
         * owner to that. Otherwise, owner will be set to TeamLand._WILDUUID_
         */
        public UUID owner = null;

        @Override
        public IntArrayTag serializeNBT()
        {
            return NbtUtils.createUUID(this.owner);
        }

        @Override
        public void deserializeNBT(final IntArrayTag nbt)
        {
            try
            {
                this.owner = NbtUtils.loadUUID(nbt);
            }
            catch (final Exception e)
            {
                Essentials.LOGGER.catching(e);
            }
        }
    }

    public static interface IClaimed
    {
        ClaimSegment getSegment(int yIndex);

        ClaimInfo getInfo();
    }

    public static final Capability<IClaimed> CAPABILITY = CapabilityManager.get(new CapabilityToken<>()
    {
    });

    private static final ResourceLocation CAPTAG = new ResourceLocation(Essentials.MODID, "claims");

    public static void setup()
    {
        MinecraftForge.EVENT_BUS.addListener(ClaimedCapability::registerCapabilities);
        MinecraftForge.EVENT_BUS.addGenericListener(LevelChunk.class, ClaimedCapability::attach);
    }

    private static void registerCapabilities(final RegisterCapabilitiesEvent event)
    {
        event.register(IClaimed.class);
    }

    private static void attach(final AttachCapabilitiesEvent<LevelChunk> event)
    {
        if (event.getCapabilities().containsKey(ClaimedCapability.CAPTAG)) return;
        event.addCapability(ClaimedCapability.CAPTAG, new ChunkClaim());
    }
}
