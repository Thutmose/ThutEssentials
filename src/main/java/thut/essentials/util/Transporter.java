package thut.essentials.util;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.WorldTickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import thut.essentials.land.LandManager.KGobalPos;

public class Transporter
{
    public static class Vector3
    {
        public static Vector3 readFromNBT(final CompoundNBT nbt, final String tag)
        {
            if (!nbt.contains(tag + "x")) return null;

            final Vector3 ret = new Vector3();
            ret.x = nbt.getDouble(tag + "x");
            ret.y = nbt.getDouble(tag + "y");
            ret.z = nbt.getDouble(tag + "z");
            return ret;
        }

        public double x;
        public double y;
        public double z;

        public Vector3 set(final Direction dir)
        {
            this.x = dir.getStepX();
            this.y = dir.getStepY();
            this.z = dir.getStepZ();
            return this;
        }

        public Vector3 set(final double x, final double y, final double z)
        {
            this.x = x;
            this.y = y;
            this.z = z;
            return this;
        }

        public Vector3 set(final double[] vec)
        {
            this.x = vec[0];
            this.y = vec[1];
            this.z = vec[2];
            return this;
        }

        public Vector3 set(final Entity e, final boolean b)
        {
            if (e != null && b)
            {
                this.x = e.getX();
                this.y = e.getY() + e.getBbHeight() / 2;
                this.z = e.getZ();
            }
            else if (e != null)
            {
                this.x = e.getX();
                this.y = e.getY() + e.getEyeHeight();
                this.z = e.getZ();
            }
            return this;
        }

        public void set(final int i, final double j)
        {
            if (i == 0) this.x = j;
            else if (i == 1) this.y = j;
            else if (i == 2) this.z = j;
        }

        public Vector3 set(final Object o)
        {
            if (o instanceof Entity)
            {
                final Entity e = (Entity) o;
                this.set(e.getX(), e.getY(), e.getZ());
            }
            else if (o instanceof TileEntity)
            {
                final TileEntity te = (TileEntity) o;
                this.set(te.getBlockPos());
            }
            else if (o instanceof double[])
            {
                final double[] d = (double[]) o;
                this.set(d[0], d[1], d[2]);
            }
            else if (o instanceof Direction)
            {
                final Direction side = (Direction) o;
                this.set(side.getStepX(), side.getStepY(), side.getStepZ());
            }
            else if (o instanceof Vector3) this.set((Vector3) o);
            else if (o instanceof BlockPos)
            {
                final BlockPos c = (BlockPos) o;
                this.set(c.getX(), c.getY(), c.getZ());
            }
            else if (o instanceof PathPoint)
            {
                final PathPoint p = (PathPoint) o;
                this.set(p.x, p.y, p.z);
            }
            else if (o instanceof Vector3d)
            {
                final Vector3d p = (Vector3d) o;
                this.set(p.x, p.y, p.z);
            }
            else if (o instanceof int[])
            {
                final int[] p = (int[]) o;
                this.set(p[0], p[1], p[2]);
            }
            else if (o instanceof byte[])
            {
                final byte[] p = (byte[]) o;
                this.set(p[0], p[1], p[2]);
            }
            else if (o instanceof short[])
            {
                final short[] p = (short[]) o;
                this.set(p[0], p[1], p[2]);
            }
            else if (o instanceof Double) this.x = this.y = this.z = (double) o;
            return this;
        }

        public Vector3 set(final Vector3 vec)
        {
            if (vec != null)
            {
                this.x = vec.x;
                this.y = vec.y;
                this.z = vec.z;
            }
            else
            {

            }
            return this;
        }

        public int intX()
        {
            return MathHelper.floor(this.x);
        }

        public int intY()
        {
            return MathHelper.floor(this.y);
        }

        public int intZ()
        {
            return MathHelper.floor(this.z);
        }

        public void writeToBuff(final ByteBuf data)
        {
            data.writeDouble(this.x);
            data.writeDouble(this.y);
            data.writeDouble(this.z);
        }

        public void writeToNBT(final CompoundNBT nbt, final String tag)
        {
            nbt.putDouble(tag + "x", this.x);
            nbt.putDouble(tag + "y", this.y);
            nbt.putDouble(tag + "z", this.z);
        }

        public Vector3 add(final Vector3 offset)
        {
            this.x += offset.x;
            this.y += offset.y;
            this.z += offset.z;
            return this;
        }
    }

    public static class TeleDest
    {
        public KGobalPos loc;

        private Vector3 subLoc;

        private String name;

        public int index;

        // This can be used for tracking things like if worlds update and
        // teledests need resetting, etc.
        public int version = 0;

        public TeleDest()
        {
        }

        public TeleDest setLoc(final KGobalPos loc, final Vector3 subLoc)
        {
            this.loc = loc;
            this.subLoc = subLoc;
            this.name = loc.getPos().toString() + " " + loc.getDimension().getRegistryName();
            return this;
        }

        public TeleDest setPos(final KGobalPos pos)
        {
            if (pos != null)
            {
                this.loc = pos;
                this.subLoc = new Vector3().set(this.loc.getPos().getX(), this.loc.getPos().getY(), this.loc.getPos()
                        .getZ());
                this.name = this.loc.getPos().toString() + " " + this.loc.getDimension().getRegistryName();
            }
            return this;
        }

        public TeleDest setVersion(final int version)
        {
            this.version = version;
            return this;
        }

        public KGobalPos getPos()
        {
            return this.loc;
        }

        public Vector3 getLoc()
        {
            return this.subLoc;
        }

        public String getName()
        {
            return this.name;
        }

        public TeleDest setIndex(final int index)
        {
            this.index = index;
            return this;
        }

        public TeleDest setName(final String name)
        {
            this.name = name;
            return this;
        }

        public void writeToNBT(final CompoundNBT nbt)
        {
            this.subLoc.writeToNBT(nbt, "v");
            nbt.put("pos", CoordinateUtls.toNBT(this.loc));
            nbt.putString("name", this.name);
            nbt.putInt("i", this.index);
            nbt.putInt("_v_", this.version);
        }

        public void shift(final double dx, final int dy, final double dz)
        {
            this.subLoc.x += dx;
            this.subLoc.y += dy;
            this.subLoc.z += dz;
        }

        public boolean withinDist(final TeleDest other, final double dist)
        {
            if (other.loc.getDimension() == this.loc.getDimension()) return other.loc.getPos().closerThan(this.loc
                    .getPos(), dist);
            return false;
        }
    }

    private static class InvulnTicker
    {
        private final ServerWorld overworld;

        private final Entity entity;
        private final long   start;

        public InvulnTicker(final Entity entity)
        {
            this.entity = entity;
            this.overworld = entity.getServer().getLevel(World.OVERWORLD);
            this.start = this.overworld.getGameTime();
            MinecraftForge.EVENT_BUS.register(this);
        }

        @SubscribeEvent
        public void damage(final LivingHurtEvent event)
        {
            if (event.getEntity() != this.entity) return;
            final long time = this.overworld.getGameTime();
            if (time - this.start > 20)
            {
                MinecraftForge.EVENT_BUS.unregister(this);
                return;
            }
            event.setCanceled(true);
        }

    }

    private static class TransferTicker
    {
        private final Entity      entity;
        private final ServerWorld destWorld;
        private final TeleDest    dest;
        private final boolean     sound;

        public TransferTicker(final ServerWorld destWorld, final Entity entity, final TeleDest dest,
                final boolean sound)
        {
            this.entity = entity;
            this.dest = dest;
            this.sound = sound;
            this.destWorld = destWorld;
            MinecraftForge.EVENT_BUS.register(this);
        }

        @SubscribeEvent
        public void TickEvent(final WorldTickEvent event)
        {
            if (event.world == this.entity.getCommandSenderWorld() && event.phase == Phase.END)
            {
                MinecraftForge.EVENT_BUS.unregister(this);
                Transporter.transferMob(this.destWorld, this.dest, this.entity);
                if (this.sound)
                {
                    this.destWorld.playLocalSound(this.dest.subLoc.x, this.dest.subLoc.y,
                            this.dest.subLoc.z,
                            SoundEvents.ENDERMAN_TELEPORT, SoundCategory.BLOCKS, 1.0F, 1.0F, false);
                    this.entity.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
                }

            }
        }
    }

    public static void transferTo(final Entity entity, final TeleDest dest)
    {
        Transporter.transferTo(entity, dest, false);
    }

    public static void transferTo(final Entity entity, final TeleDest dest, final boolean sound)
    {
        if (entity.getCommandSenderWorld() instanceof ServerWorld)
        {
            new InvulnTicker(entity);
            if (dest.loc.getDimension() == entity.level.dimension())
            {
                Transporter.moveMob(entity, dest);
                return;
            }
            final ServerWorld destWorld = entity.getServer().getLevel(dest.loc.getDimension());
            if (entity instanceof ServerPlayerEntity)
            {
                final ServerPlayerEntity player = (ServerPlayerEntity) entity;
                player.isChangingDimension = true;
                player.teleportTo(destWorld, dest.subLoc.x, dest.subLoc.y, dest.subLoc.z, entity.yRot,
                        entity.xRot);
                if (sound)
                {
                    destWorld.playLocalSound(dest.subLoc.x, dest.subLoc.y, dest.subLoc.z,
                            SoundEvents.ENDERMAN_TELEPORT, SoundCategory.BLOCKS, 1.0F, 1.0F, false);
                    player.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
                }
                player.isChangingDimension = false;
            }
            else // Schedule the transfer for end of tick.
                new TransferTicker(destWorld, entity, dest, sound);
        }
    }

    private static void transferMob(final ServerWorld destWorld, final TeleDest dest, final Entity entity)
    {
        ServerPlayerEntity player = null;
        if (entity instanceof ServerPlayerEntity)
        {
            player = (ServerPlayerEntity) entity;
            player.isChangingDimension = true;
        }
        final ServerWorld serverworld = (ServerWorld) entity.getCommandSenderWorld();
        // TODO did we need to update the mob for what dim it was in?
        Transporter.removeMob(serverworld, entity, true);
        entity.revive();
        entity.moveTo(dest.subLoc.x, dest.subLoc.y, dest.subLoc.z, entity.yRot,
                entity.xRot);
        entity.setLevel(destWorld);
        Transporter.addMob(destWorld, entity);
        if (player != null)
        {
            player.isChangingDimension = false;
            player.connection.resetPosition();
            player.connection.teleport(dest.subLoc.x, dest.subLoc.y, dest.subLoc.z, entity.yRot,
                    entity.xRot);
        }
    }

    private static void addMob(final ServerWorld world, final Entity entity)
    {
        if (net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                new net.minecraftforge.event.entity.EntityJoinWorldEvent(entity, world))) return;
        final IChunk ichunk = world.getChunk(MathHelper.floor(entity.getX() / 16.0D), MathHelper.floor(entity
                .getZ() / 16.0D), ChunkStatus.FULL, true);
        if (ichunk instanceof Chunk) ichunk.addEntity(entity);
        world.loadFromChunk(entity);
    }

    private static void removeMob(final ServerWorld world, final Entity entity, final boolean keepData)
    {
        entity.remove(keepData);
        world.removeEntity(entity, keepData);
    }

    private static void moveMob(final Entity entity, final TeleDest dest)
    {
        if (entity instanceof ServerPlayerEntity)
        {
            final ServerPlayerEntity player = (ServerPlayerEntity) entity;
            player.isChangingDimension = true;
            ((ServerPlayerEntity) entity).connection.teleport(dest.subLoc.x, dest.subLoc.y, dest.subLoc.z,
                    entity.yRot, entity.xRot);
            ((ServerPlayerEntity) entity).connection.resetPosition();
            player.isChangingDimension = false;
        }
        else entity.moveTo(dest.subLoc.x, dest.subLoc.y, dest.subLoc.z, entity.yRot,
                entity.xRot);
    }
}
