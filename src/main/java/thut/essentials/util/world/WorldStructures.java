package thut.essentials.util.world;

import com.google.common.collect.Lists;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.common.util.INBTSerializable;
import thut.essentials.Essentials;

import java.util.Collection;
import java.util.List;

public class WorldStructures extends SavedData implements IHasStructures, INBTSerializable<ListTag>
{
    // Create new instance of saved data
    public static WorldStructures create()
    {
        return new WorldStructures();
    }

    // Load existing instance of saved data
    public static WorldStructures load(CompoundTag tag, HolderLookup.Provider lookupProvider)
    {
        WorldStructures data = WorldStructures.create();
        // Load saved data
        if (tag.contains("L")) data.deserializeNBT(lookupProvider, tag.getList("L", Tag.TAG_COMPOUND));
        return data;
    }

    public static WorldStructures getForLevel(ServerLevel level)
    {
        return level.getDataStorage()
                .get(new SavedData.Factory<>(WorldStructures::create, WorldStructures::load), "te_structures");
    }

    @Override
    public CompoundTag save(CompoundTag compoundTag, HolderLookup.Provider provider)
    {
        compoundTag.put("L", this.serializeNBT(provider));
        return compoundTag;
    }

    public static class StructInfo implements INBTSerializable<CompoundTag>
    {
        ResourceLocation key;
        BoundingBox box;

        public StructInfo(final ResourceLocation key, final BoundingBox box)
        {
            this.key = key;
            this.box = box;
        }

        public StructInfo(final CompoundTag tag)
        {
            this.deserializeNBT(Essentials.server.registryAccess(), tag);
        }

        @Override
        public CompoundTag serializeNBT(HolderLookup.Provider var1)
        {
            final CompoundTag tag = new CompoundTag();
            tag.putString("key", this.key.toString());
            tag.put("box", this.write(this.box));
            return tag;
        }

        @Override
        public void deserializeNBT(HolderLookup.Provider var1, final CompoundTag nbt)
        {
            this.key = ResourceLocation.parse(nbt.getString("key"));
            this.box = this.read(nbt.getIntArray("box"));
        }

        private IntArrayTag write(final BoundingBox box)
        {
            return new IntArrayTag(
                    new int[] { box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ() });
        }

        private BoundingBox read(final int[] arr)
        {
            return new BoundingBox(arr[0], arr[1], arr[2], arr[3], arr[4], arr[5]);
        }
    }

    public static void setup()
    {
    }

    final List<StructInfo> structs = Lists.newArrayList();

    @Override
    public void putStructure(final ResourceLocation key, final BoundingBox box)
    {
        synchronized (this.structs)
        {
            this.structs.add(new StructInfo(key,
                    new BoundingBox(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())));
        }
    }

    @Override
    public Collection<ResourceLocation> getStructures(final BlockPos pos)
    {
        final List<ResourceLocation> ret = Lists.newArrayList();
        synchronized (this.structs)
        {
            for (final StructInfo i : this.structs)
                if (i.box.isInside(pos)) ret.add(i.key);
        }
        return ret;
    }

    @Override
    public void removeStructures(final ResourceLocation key, final BoundingBox box)
    {
        synchronized (this.structs)
        {
            this.structs.removeIf(i -> (key == null || i.key.equals(key)) && i.box.intersects(box));
        }
    }

    @Override
    public ListTag serializeNBT(HolderLookup.Provider var1)
    {
        final ListTag list = new ListTag();
        for (final StructInfo i : this.structs)
            list.add(i.serializeNBT(var1));
        return list;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider var1, final ListTag nbt)
    {
        this.structs.clear();
        for (final Tag tag : nbt)
            if (tag instanceof CompoundTag) this.structs.add(new StructInfo((CompoundTag) tag));
    }

}
