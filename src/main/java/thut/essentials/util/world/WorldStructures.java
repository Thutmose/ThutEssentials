package thut.essentials.util.world;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
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

public class WorldStructures implements IHasStructures, ICapabilitySerializable<ListNBT>
{
    public static class StructInfo implements INBTSerializable<CompoundNBT>
    {
        ResourceLocation   key;
        MutableBoundingBox box;

        public StructInfo(final ResourceLocation key, final MutableBoundingBox box)
        {
            this.key = key;
            this.box = box;
        }

        public StructInfo(final CompoundNBT tag)
        {
            this.deserializeNBT(tag);
        }

        @Override
        public CompoundNBT serializeNBT()
        {
            final CompoundNBT tag = new CompoundNBT();
            tag.putString("key", this.key.toString());
            tag.put("box", this.box.toNBTTagIntArray());
            return tag;
        }

        @Override
        public void deserializeNBT(final CompoundNBT nbt)
        {
            this.key = new ResourceLocation(nbt.getString("key"));
            this.box = new MutableBoundingBox(nbt.getIntArray("box"));
        }
    }

    public static class Storage implements Capability.IStorage<IHasStructures>
    {
        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Override
        public void readNBT(final Capability<IHasStructures> capability, final IHasStructures instance,
                final Direction side, final INBT nbt)
        {
            if (instance instanceof ICapabilitySerializable) ((ICapabilitySerializable) instance).deserializeNBT(nbt);
        }

        @Override
        public INBT writeNBT(final Capability<IHasStructures> capability, final IHasStructures instance,
                final Direction side)
        {
            if (instance instanceof ICapabilitySerializable<?>) return ((ICapabilitySerializable<?>) instance)
                    .serializeNBT();
            return null;
        }
    }

    public static void setup()
    {
        CapabilityManager.INSTANCE.register(IHasStructures.class, new Storage(), WorldStructures::new);
        MinecraftForge.EVENT_BUS.register(WorldStructures.class);
    }

    @SubscribeEvent
    public static void attach(final AttachCapabilitiesEvent<World> event)
    {
        if (!(event.getObject() instanceof ServerWorld)) return;
        if (event.getCapabilities().containsKey(WorldStructures.CAPTAG)) return;
        event.addCapability(WorldStructures.CAPTAG, new WorldStructures());
    }

    private static final ResourceLocation CAPTAG = new ResourceLocation(Essentials.MODID, "genned_structures");

    @CapabilityInject(IHasStructures.class)
    public static final Capability<IHasStructures> CAPABILITY = null;

    private final LazyOptional<IHasStructures> holder = LazyOptional.of(() -> this);

    List<StructInfo> structs = Lists.newArrayList();

    @Override
    public void putStructure(final ResourceLocation key, final MutableBoundingBox box)
    {
        synchronized (this.structs)
        {
            this.structs.add(new StructInfo(key, new MutableBoundingBox(box)));
        }
    }

    @Override
    public Collection<ResourceLocation> getStructures(final BlockPos pos)
    {
        final List<ResourceLocation> ret = Lists.newArrayList();
        synchronized (this.structs)
        {
            for (final StructInfo i : this.structs)
                if (i.box.isVecInside(pos)) ret.add(i.key);
        }
        return ret;
    }

    @Override
    public void removeStructures(final ResourceLocation key, final MutableBoundingBox box)
    {
        synchronized (this.structs)
        {
            this.structs.removeIf(i -> (key == null || i.key.equals(key)) && i.box.intersectsWith(box));
        }
    }

    @Override
    public <T> LazyOptional<T> getCapability(final Capability<T> cap, final Direction side)
    {
        return WorldStructures.CAPABILITY.orEmpty(cap, this.holder);
    }

    @Override
    public ListNBT serializeNBT()
    {
        final ListNBT list = new ListNBT();
        for (final StructInfo i : this.structs)
            list.add(i.serializeNBT());
        return list;
    }

    @Override
    public void deserializeNBT(final ListNBT nbt)
    {
        this.structs.clear();
        for (final INBT tag : nbt)
            if (tag instanceof CompoundNBT) this.structs.add(new StructInfo((CompoundNBT) tag));
    }

}
