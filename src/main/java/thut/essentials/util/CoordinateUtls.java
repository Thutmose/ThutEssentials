package thut.essentials.util;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.NBTDynamicOps;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import thut.essentials.Essentials;

public class CoordinateUtls
{
    public static GlobalPos forMob(final Entity mob)
    {
        return GlobalPos.getPosition(mob.getEntityWorld().getDimensionKey(), mob.getPosition());
    }

    public static GlobalPos chunkPos(final GlobalPos blockPos)
    {
        final BlockPos pos = new BlockPos(blockPos.getPos().getX() >> 4, blockPos.getPos().getY() >> 4, blockPos
                .getPos().getZ() >> 4);
        return GlobalPos.getPosition(blockPos.getDimension(), pos);
    }

    public static GlobalPos fromNBT(final CompoundNBT tag)
    {
        return GlobalPos.CODEC.decode(NBTDynamicOps.INSTANCE, tag).result().get().getFirst();
    }

    @SuppressWarnings("unchecked")
    public static <T extends INBT> T toNBT(final GlobalPos pos)
    {
        return (T) GlobalPos.CODEC.encodeStart(NBTDynamicOps.INSTANCE, pos).get().left().get();
    }

    public static GlobalPos fromString(String string)
    {
        if (string.contains("->")) string = string.split("->")[1];
        final String[] args = string.split(",");
        if (args.length != 4) return null;
        try
        {
            final BlockPos pos = new BlockPos(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(
                    args[2]));
            final RegistryKey<World> dim = RegistryKey.getOrCreateKey(Registry.WORLD_KEY, new ResourceLocation(
                    args[3]));
            return GlobalPos.getPosition(dim, pos);
        }
        catch (final NumberFormatException e)
        {
            Essentials.LOGGER.error("Error loading warp for {}", string);
            e.printStackTrace();
        }
        return null;
    }

    public static String toString(final GlobalPos pos)
    {
        return pos.getPos().getX() + "," + pos.getPos().getY() + "," + pos.getPos().getZ() + "," + pos.getDimension()
                .getLocation();
    }
}
