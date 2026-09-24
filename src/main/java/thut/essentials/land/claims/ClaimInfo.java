package thut.essentials.land.claims;

import com.google.common.collect.Sets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.Set;
import java.util.UUID;

public class ClaimInfo implements INBTSerializable<CompoundTag>
{
    public UUID owner;
    public Set<UUID> publicMobs = Sets.newHashSet();
    public Set<UUID> protectedMobs = Sets.newHashSet();
    public Set<BlockPos> publicBlocks = Sets.newHashSet();

    public void mergeFrom(ClaimInfo other)
    {
        this.protectedMobs.addAll(other.protectedMobs);
        this.publicBlocks.addAll(other.publicBlocks);
        this.publicMobs.addAll(other.publicMobs);
    }

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
        if (owner != null) tag.put("owner", NbtUtils.createUUID(owner));
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider var1, final CompoundTag nbt)
    {
        if (nbt.contains("owner")) owner = NbtUtils.loadUUID(nbt.get("owner"));
        if (nbt.contains("public_mobs"))
        {
            ListTag list = (ListTag) nbt.get("public_mobs");
            list.forEach(tag -> publicMobs.add(NbtUtils.loadUUID(tag)));
        }
        if (nbt.contains("protected_mobs"))
        {
            ListTag list = (ListTag) nbt.get("protected_mobs");
            list.forEach(tag -> protectedMobs.add(NbtUtils.loadUUID(tag)));
        }
        if (nbt.contains("public_blocks"))
        {
            ListTag list = (ListTag) nbt.get("public_blocks");
            list.forEach(tag -> {
                var posTag = new CompoundTag();
                posTag.put("p", tag);
                NbtUtils.readBlockPos(posTag, "p").ifPresent(publicBlocks::add);
            });
        }
    }
}