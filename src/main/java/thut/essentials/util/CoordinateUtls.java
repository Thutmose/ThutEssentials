package thut.essentials.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import thut.essentials.Essentials;
import thut.essentials.events.TeleLoadEvent;
import thut.essentials.land.LandManager.KGobalPos;
import thut.essentials.util.Transporter.TeleDest;
import thut.essentials.util.Transporter.Vector3;

public class CoordinateUtls
{
    public static KGobalPos forMob(final Entity mob)
    {
        return KGobalPos.getPosition(mob.getCommandSenderWorld().dimension(), mob.blockPosition());
    }

    public static KGobalPos chunkPos(final KGobalPos blockPos)
    {
        final BlockPos pos = new BlockPos(blockPos.getPos().getX() >> 4, blockPos.getPos().getY() >> 4, blockPos
                .getPos().getZ() >> 4);
        return KGobalPos.getPosition(blockPos.getDimension(), pos);
    }

    public static KGobalPos fromNBT(final CompoundTag tag)
    {
        if (tag.contains("_v_"))
        {
            final CompoundTag nbt = tag;
            final Vector3 loc = Vector3.readFromNBT(nbt, "v");
            final String name = nbt.getString("name");
            final int index = nbt.getInt("i");
            final int version = nbt.getInt("_v_");
            final KGobalPos pos = CoordinateUtls.fromNBT(nbt.getCompound("pos"));
            if (pos == null) return null;
            final TeleDest dest = new TeleDest().setLoc(pos, loc).setPos(pos).setName(name).setIndex(index).setVersion(
                    version);
            final TeleLoadEvent event = new TeleLoadEvent(dest);
            // This returns true if the event is cancelled.
            if (MinecraftForge.EVENT_BUS.post(event)) return null;
            // The event can override the destination, it defaults to dest.
            return event.getOverride().loc;
        }
        try
        {
            final GlobalPos pos = GlobalPos.CODEC.decode(NbtOps.INSTANCE, tag).result().get().getFirst();
            return new KGobalPos(pos);
        }
        catch (final Exception e)
        {
            // Essentials.LOGGER.error("Error reading from nbt!");
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Tag> T toNBT(final KGobalPos pos)
    {
        return (T) GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, pos.pos).get().left().get();
    }

    public static CompoundTag toNBT(final KGobalPos pos, final String name)
    {
        final TeleDest dest = new TeleDest().setName(name).setPos(pos).setVersion(Essentials.config.dim_verison);
        final CompoundTag nbt = new CompoundTag();
        dest.writeToNBT(nbt);
        return nbt;
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
            final ResourceKey<Level> dim = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(
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
                .location();
    }
}
