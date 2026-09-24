package thut.essentials.land.claims;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class NamedVolumes
{
    public static interface INamedPart
    {
        /**
         * @return The name of this part
         */
        String getName();
        /**
         * @return A key for the deserialiser for this part for loading it from nbt, etc.
         */
        String getKey();

        BoundingBox getBounds();

        default boolean is(String name)
        {
            return name.equals(this.getName());
        }
    }

    public static interface INamedVolume
    {
        /**
         * @return The name of this volume, does not need to be unique or registered anywhere
         */
        String getName();

        /**
         * @return A key for the deserialiser for this volume for loading it from nbt, etc.
         */
        String getKey();

        default boolean is(String name)
        {
            return name.equals(this.getName());
        }

        List<INamedPart> getParts();

        BoundingBox getTotalBounds();

        default boolean isIn(final BlockPos pos)
        {
            if (this.getParts().isEmpty()) return false;
            if (!this.getTotalBounds().isInside(pos)) return false;
            synchronized (this.getParts())
            {
                for (var p1 : this.getParts()) if (insideBox(p1.getBounds(), pos)) return true;
            }
            return false;
        }

        default boolean isNear(final BlockPos pos, final int distance)
        {
            if (this.getParts().isEmpty()) return false;
            if (!inflate(this.getTotalBounds(), distance).isInside(pos)) return false;
            synchronized (this.getParts())
            {
                for (var p1 : this.getParts())
                    if (insideBox(inflate(p1.getBounds(), distance), pos)) return true;
            }
            return false;
        }
    }

    private static BoundingBox inflate(final BoundingBox other, final int amt)
    {
        return new BoundingBox(other.minX(), other.minY(), other.minZ(), other.maxX(), other.maxY(),
                other.maxZ()).inflatedBy(amt);
    }

    private static boolean insideBox(final BoundingBox b, BlockPos pos)
    {
        return b.isInside(pos);
    }

    public static Map<String, Supplier<INamedVolume>> VOLUMES_FACTORY_REGISTRY = new HashMap<>();
    public static Map<String, Supplier<INamedPart>> PART_FACTORY_REGISTRY = new HashMap<>();

    public static INamedPart loadPart(HolderLookup.Provider registries, CompoundTag comp)
    {
        var key = comp.getString("key");
        var data = comp.getCompound("tag");
        if (NamedVolumes.PART_FACTORY_REGISTRY.containsKey(key))
        {
            var factory = NamedVolumes.PART_FACTORY_REGISTRY.get(key);
            var obj = factory.get();
            if (obj instanceof INBTSerializable<?> sera)
            {
                try
                {
                    @SuppressWarnings("unchecked")
                    var ser = (INBTSerializable<CompoundTag>) sera;
                    ser.deserializeNBT(registries, data);
                    return obj;
                }
                catch (Exception ignored)
                {

                }
            }
        }
        return null;
    }

    public static INamedVolume loadVolume(HolderLookup.Provider registries, CompoundTag comp)
    {
        var key = comp.getString("key");
        var data = comp.getCompound("tag");
        if(NamedVolumes.VOLUMES_FACTORY_REGISTRY.containsKey(key))
        {
            var factory = NamedVolumes.VOLUMES_FACTORY_REGISTRY.get(key);
            var obj = factory.get();
            if(obj instanceof INBTSerializable<?> sera)
            {
                try
                {
                    @SuppressWarnings("unchecked")
                    var ser = (INBTSerializable<CompoundTag>) sera;
                    ser.deserializeNBT(registries, data);
                    return obj;
                }
                catch (Exception ignored)
                {

                }
            }
        }
        return null;
    }

    public static CompoundTag saveVolumeOrPart(HolderLookup.Provider registries, Object volume)
    {
        CompoundTag tag = new CompoundTag();
        if (volume instanceof INamedVolume vol)
        {
            tag.putString("key", vol.getKey());
            if (vol instanceof INBTSerializable<?> ser)
            {
                tag.put("tag", ser.serializeNBT(registries));
            }
        }
        else if(volume instanceof INamedPart part)
        {
            tag.putString("key", part.getKey());
            if (part instanceof INBTSerializable<?> ser)
            {
                tag.put("tag", ser.serializeNBT(registries));
            }
        }
        return tag;
    }
}
