package thut.essentials.land;

import com.google.common.collect.Sets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.registries.DeferredRegister;
import thut.essentials.Essentials;

import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public class ClaimedCapability
{
    public static class ClaimInfo implements INBTSerializable<CompoundTag>
    {
        public Set<UUID> publicMobs = Sets.newHashSet();
        public Set<UUID> protectedMobs = Sets.newHashSet();

        public Set<BlockPos> publicBlocks = Sets.newHashSet();

        @Override
        public CompoundTag serializeNBT(HolderLookup.Provider var1)
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
        public void deserializeNBT(HolderLookup.Provider var1, final CompoundTag nbt)
        {

        }
    }

    public static class ClaimSegment implements INBTSerializable<IntArrayTag>
    {
        /**
         * This is the UUID of the TeamLand associated with this claim, not the Team, or the Player, this allows
         * resetting team land without loading the chunks. When this is first initialized, it will check to see if any
         * teams actually own here, via legacy means, and if so, will set owner to that. Otherwise, owner will be set to
         * TeamLand._WILDUUID_
         */
        public UUID owner = null;

        @Override
        public IntArrayTag serializeNBT(HolderLookup.Provider var1)
        {
            return NbtUtils.createUUID(this.owner);
        }

        @Override
        public void deserializeNBT(HolderLookup.Provider var1,final IntArrayTag nbt)
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

    public static interface IClaimed extends INBTSerializable<CompoundTag>
    {
        ClaimSegment getSegment(int yIndex);

        ClaimInfo getInfo();
    }

    public static IClaimed makeProvider(final IAttachmentHolder in)
    {
        if (!(in instanceof ChunkAccess chunk)) return null;
        return new ChunkClaim();
    }

    private static final ResourceLocation CAPTAG = ResourceLocation.fromNamespaceAndPath(Essentials.MODID, "claims");

    public static Supplier<AttachmentType<IClaimed>> TYPE;

    public static void setup(DeferredRegister<AttachmentType<?>> registry)
    {
        TYPE = registry.register(CAPTAG.getPath(),
                () -> AttachmentType.serializable(ClaimedCapability::makeProvider).build());
    }
}
