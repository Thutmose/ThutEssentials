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
import thut.essentials.land.LandManager.KGobalPos;

public class CoordinateUtls
{
    public static KGobalPos forMob(final Entity mob)
    {
        return KGobalPos.getPosition(mob.getEntityWorld().getDimensionKey(), mob.getPosition());
    }

    public static KGobalPos chunkPos(final KGobalPos blockPos)
    {
        final BlockPos pos = new BlockPos(blockPos.getPos().getX() >> 4, blockPos.getPos().getY() >> 4, blockPos
                .getPos().getZ() >> 4);
        return KGobalPos.getPosition(blockPos.getDimension(), pos);
    }

    public static KGobalPos fromNBT(final CompoundNBT tag)
    {
        try
        {
            final GlobalPos pos = GlobalPos.CODEC.decode(NBTDynamicOps.INSTANCE, tag).result().get().getFirst();
            return new KGobalPos(pos);
        }
        catch (final Exception e)
        {
            Essentials.LOGGER.error("Error reading from nbt!");
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends INBT> T toNBT(final KGobalPos pos)
    {
        return (T) GlobalPos.CODEC.encodeStart(NBTDynamicOps.INSTANCE, pos.pos).get().left().get();
    }

    public static KGobalPos fromString(String string)
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
            return KGobalPos.getPosition(dim, pos);
        }
        catch (final NumberFormatException e)
        {
            Essentials.LOGGER.error("Error loading warp for {}", string);
            e.printStackTrace();
        }
        return null;
    }

    public static String toString(final KGobalPos pos)
    {
        return pos.getPos().getX() + "," + pos.getPos().getY() + "," + pos.getPos().getZ() + "," + pos.getDimension()
                .getLocation();
    }
}
