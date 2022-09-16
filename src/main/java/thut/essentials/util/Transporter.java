package thut.essentials.util;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.LevelTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import thut.essentials.Essentials;
import thut.essentials.land.LandManager.KGobalPos;

public class Transporter
{
    public static class Vector3
    {
        public static Vector3 readFromNBT(final CompoundTag nbt, final String tag)
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
            else if (o instanceof BlockEntity)
            {
                final BlockEntity te = (BlockEntity) o;
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
            else if (o instanceof Node)
            {
                final Node p = (Node) o;
                this.set(p.x, p.y, p.z);
            }
            else if (o instanceof Vec3)
            {
                final Vec3 p = (Vec3) o;
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
            return Mth.floor(this.x);
        }

        public int intY()
        {
            return Mth.floor(this.y);
        }

        public int intZ()
        {
            return Mth.floor(this.z);
        }

        public void writeToBuff(final ByteBuf data)
        {
            data.writeDouble(this.x);
            data.writeDouble(this.y);
            data.writeDouble(this.z);
        }

        public void writeToNBT(final CompoundTag nbt, final String tag)
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
        {}

        public TeleDest setLoc(final KGobalPos loc, final Vector3 subLoc)
        {
            this.loc = loc;
            this.subLoc = subLoc;
            this.name = loc.getPos().toString() + " " + loc.getDimension().location();
            return this;
        }

        public TeleDest setPos(final KGobalPos pos)
        {
            if (pos != null)
            {
                this.loc = pos;
                this.subLoc = new Vector3().set(this.loc.getPos().getX(), this.loc.getPos().getY(),
                        this.loc.getPos().getZ());
                this.name = this.loc.getPos().toString() + " " + this.loc.getDimension().location();
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

        public void writeToNBT(final CompoundTag nbt)
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
            if (other.loc.getDimension() == this.loc.getDimension())
                return other.loc.getPos().closerThan(this.loc.getPos(), dist);
            return false;
        }
    }

    private static class InvulnTicker
    {
        private final ServerLevel overworld;

        private final Entity entity;
        private final long start;

        public InvulnTicker(final Entity entity)
        {
            this.entity = entity;
            this.overworld = entity.getServer().getLevel(Level.OVERWORLD);
            this.start = this.overworld.getGameTime();
            MinecraftForge.EVENT_BUS.register(this);
        }

        @SubscribeEvent
        public void damage(final LivingHurtEvent event)
        {
            if (event.getEntity() != this.entity) return;
            final long time = this.overworld.getGameTime();
            if (time - this.start > Essentials.config.postTeleInvulDur)
            {
                MinecraftForge.EVENT_BUS.unregister(this);
                return;
            }
            event.setCanceled(true);
        }

    }

    private static class TransferTicker
    {
        private final Entity entity;
        private final ServerLevel destWorld;
        private final TeleDest dest;
        private final boolean sound;

        public TransferTicker(final ServerLevel destWorld, final Entity entity, final TeleDest dest,
                final boolean sound)
        {
            this.entity = entity;
            this.dest = dest;
            this.sound = sound;
            this.destWorld = destWorld;
            MinecraftForge.EVENT_BUS.register(this);
        }

        @SubscribeEvent
        public void TickEvent(final LevelTickEvent event)
        {
            if (event.level == this.entity.getCommandSenderWorld() && event.phase == Phase.END)
            {
                MinecraftForge.EVENT_BUS.unregister(this);
                Transporter.transferMob(this.destWorld, this.dest, this.entity);
                if (this.sound)
                {
                    this.destWorld.playLocalSound(this.dest.subLoc.x, this.dest.subLoc.y, this.dest.subLoc.z,
                            SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.0F, 1.0F, false);
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
        if (entity.getCommandSenderWorld() instanceof ServerLevel)
        {
            new InvulnTicker(entity);
            if (dest.loc.getDimension() == entity.level.dimension())
            {
                Transporter.moveMob(entity, dest);
                return;
            }
            final ServerLevel destWorld = entity.getServer().getLevel(dest.loc.getDimension());
            if (entity instanceof ServerPlayer)
            {
                final ServerPlayer player = (ServerPlayer) entity;
                player.isChangingDimension = true;
                player.teleportTo(destWorld, dest.subLoc.x, dest.subLoc.y, dest.subLoc.z, entity.getYRot(),
                        entity.getXRot());
                if (sound)
                {
                    destWorld.playLocalSound(dest.subLoc.x, dest.subLoc.y, dest.subLoc.z, SoundEvents.ENDERMAN_TELEPORT,
                            SoundSource.BLOCKS, 1.0F, 1.0F, false);
                    player.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
                }
                player.isChangingDimension = false;
            }
            else // Schedule the transfer for end of tick.
                new TransferTicker(destWorld, entity, dest, sound);
        }
    }

    private static void transferMob(final ServerLevel destWorld, final TeleDest dest, final Entity entity)
    {
        ServerPlayer player = null;
        if (entity instanceof ServerPlayer)
        {
            player = (ServerPlayer) entity;
            player.isChangingDimension = true;
        }
        final ServerLevel serverworld = (ServerLevel) entity.getCommandSenderWorld();
        Transporter.removeMob(serverworld, entity, true);
        entity.revive();
        entity.moveTo(dest.subLoc.x, dest.subLoc.y, dest.subLoc.z, entity.getYRot(), entity.getXRot());
        Transporter.addMob(destWorld, entity);
        if (player != null)
        {
            player.connection.resetPosition();
            player.connection.teleport(dest.subLoc.x, dest.subLoc.y, dest.subLoc.z, entity.getYRot(), entity.getXRot());
            player.isChangingDimension = false;
        }
    }

    private static void addMob(final ServerLevel world, final Entity entity)
    {
        if (net.minecraftforge.common.MinecraftForge.EVENT_BUS
                .post(new net.minecraftforge.event.entity.EntityJoinLevelEvent(entity, world)))
            return;
        final ChunkAccess ichunk = world.getChunk(Mth.floor(entity.getX() / 16.0D), Mth.floor(entity.getZ() / 16.0D),
                ChunkStatus.FULL, true);
        if (ichunk instanceof LevelChunk) ichunk.addEntity(entity);
        world.addDuringTeleport(entity);
    }

    private static void removeMob(final ServerLevel world, final Entity entity, final boolean keepData)
    {
        entity.remove(RemovalReason.CHANGED_DIMENSION);
    }

    private static void moveMob(final Entity entity, final TeleDest dest)
    {
        if (entity instanceof ServerPlayer)
        {
            final ServerPlayer player = (ServerPlayer) entity;
            player.isChangingDimension = true;
            ((ServerPlayer) entity).connection.teleport(dest.subLoc.x, dest.subLoc.y, dest.subLoc.z, entity.getYRot(),
                    entity.getXRot());
            ((ServerPlayer) entity).connection.resetPosition();
            player.isChangingDimension = false;
        }
        else entity.moveTo(dest.subLoc.x, dest.subLoc.y, dest.subLoc.z, entity.getYRot(), entity.getXRot());
    }
}
