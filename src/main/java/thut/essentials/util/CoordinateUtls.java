package thut.essentials.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import thut.essentials.Essentials;
import thut.essentials.events.TeleLoadEvent;
import thut.essentials.util.teleporting.TeleDest;

public class CoordinateUtls
{
    public static GlobalPos forMob(final Entity mob)
    {
        return GlobalPos.of(mob.getCommandSenderWorld().dimension(), mob.blockPosition());
    }

    public static GlobalPos chunkPos(final GlobalPos blockPos)
    {
        final BlockPos pos = new BlockPos(blockPos.pos().getX() >> 4, blockPos.pos().getY() >> 4,
                blockPos.pos().getZ() >> 4);
        return GlobalPos.of(blockPos.dimension(), pos);
    }

    public static GlobalPos fromNBT(final CompoundTag tag)
    {
        if (tag.contains("_v_"))
        {
            final CompoundTag nbt = tag;
            final Vec3 loc = TeleDest.readVec3FromNBT(nbt, "v");
            final String name = nbt.getString("name");
            final int index = nbt.getInt("i");
            final int version = nbt.getInt("_v_");
            final GlobalPos pos = CoordinateUtls.fromNBT(nbt.getCompound("pos"));
            if (pos == null) return null;
            final TeleDest dest = new TeleDest().setLoc(pos, loc).setPos(pos).setName(name).setIndex(index)
                    .setVersion(version);
            final TeleLoadEvent event = new TeleLoadEvent(dest);
            // This returns true if the event is cancelled.
            if (NeoForge.EVENT_BUS.post(event).isCanceled()) return null;
            // The event can override the destination, it defaults to dest.
            return event.getOverride().loc;
        }
        try
        {
            return GlobalPos.CODEC.decode(NbtOps.INSTANCE, tag).result().get().getFirst();
        }
        catch (final Exception e)
        {
            // Essentials.LOGGER.error("Error reading from nbt!");
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Tag> T toNBT(final GlobalPos pos)
    {
        return (T) GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, pos).result().get();
    }

    public static CompoundTag toNBT(final GlobalPos pos, final String name)
    {
        final TeleDest dest = new TeleDest().setName(name).setPos(pos).setVersion(Essentials.config.dim_verison);
        final CompoundTag nbt = new CompoundTag();
        dest.writeToNBT(nbt);
        return nbt;
    }

    public static GlobalPos fromString(String string)
    {
        if (string.contains("->")) string = string.split("->")[1];
        final String[] args = string.split(",");
        if (args.length != 4) return null;
        try
        {
            final BlockPos pos = new BlockPos(Integer.parseInt(args[0]), Integer.parseInt(args[1]),
                    Integer.parseInt(args[2]));
            final ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(args[3]));
            return GlobalPos.of(dim, pos);
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
        return pos.pos().getX() + "," + pos.pos().getY() + "," + pos.pos().getZ() + "," + pos.dimension()
                .location();
    }
}
