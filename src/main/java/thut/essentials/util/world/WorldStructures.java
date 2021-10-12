package thut.essentials.util.world;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import thut.essentials.Essentials;

public class WorldStructures implements IHasStructures, ICapabilitySerializable<ListTag>
{
    public static class StructInfo implements INBTSerializable<CompoundTag>
    {
        ResourceLocation key;
        BoundingBox      box;

        public StructInfo(final ResourceLocation key, final BoundingBox box)
        {
            this.key = key;
            this.box = box;
        }

        public StructInfo(final CompoundTag tag)
        {
            this.deserializeNBT(tag);
        }

        @Override
        public CompoundTag serializeNBT()
        {
            final CompoundTag tag = new CompoundTag();
            tag.putString("key", this.key.toString());
            tag.put("box", this.write(this.box));
            return tag;
        }

        @Override
        public void deserializeNBT(final CompoundTag nbt)
        {
            this.key = new ResourceLocation(nbt.getString("key"));
            this.box = this.read(nbt.getIntArray("box"));
        }

        private IntArrayTag write(final BoundingBox box)
        {
            return new IntArrayTag(new int[] { box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box
                    .maxZ() });
        }

        private BoundingBox read(final int[] arr)
        {
            return new BoundingBox(arr[0], arr[1], arr[2], arr[3], arr[4], arr[5]);
        }
    }

    public static void setup()
    {
        CapabilityManager.INSTANCE.register(IHasStructures.class);
        MinecraftForge.EVENT_BUS.register(WorldStructures.class);
    }

    @SubscribeEvent
    public static void attach(final AttachCapabilitiesEvent<Level> event)
    {
        if (!(event.getObject() instanceof ServerLevel)) return;
        if (event.getCapabilities().containsKey(WorldStructures.CAPTAG)) return;
        event.addCapability(WorldStructures.CAPTAG, new WorldStructures());
    }

    private static final ResourceLocation CAPTAG = new ResourceLocation(Essentials.MODID, "genned_structures");

    @CapabilityInject(IHasStructures.class)
    public static final Capability<IHasStructures> CAPABILITY = null;

    private final LazyOptional<IHasStructures> holder = LazyOptional.of(() -> this);

    List<StructInfo> structs = Lists.newArrayList();

    @Override
    public void putStructure(final ResourceLocation key, final BoundingBox box)
    {
        synchronized (this.structs)
        {
            this.structs.add(new StructInfo(key, new BoundingBox(box.minX(), box.minY(), box.minZ(), box.maxX(), box
                    .maxY(), box.maxZ())));
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
    public <T> LazyOptional<T> getCapability(final Capability<T> cap, final Direction side)
    {
        return WorldStructures.CAPABILITY.orEmpty(cap, this.holder);
    }

    @Override
    public ListTag serializeNBT()
    {
        final ListTag list = new ListTag();
        for (final StructInfo i : this.structs)
            list.add(i.serializeNBT());
        return list;
    }

    @Override
    public void deserializeNBT(final ListTag nbt)
    {
        this.structs.clear();
        for (final Tag tag : nbt)
            if (tag instanceof CompoundTag) this.structs.add(new StructInfo((CompoundTag) tag));
    }

}
