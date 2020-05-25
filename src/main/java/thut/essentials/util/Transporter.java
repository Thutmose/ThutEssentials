package thut.essentials.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.WorldTickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class Transporter
{
    public static class Vector4
    {
        public float x, y, z, w;

        public Vector4()
        {
            this.y = this.z = this.w = 0;
            this.x = 1;
        }

        public Vector4(final BlockPos pos, final DimensionType dim)
        {
            this();
            this.x = pos.getX();
            this.y = pos.getY();
            this.z = pos.getZ();
            this.w = dim.getId();
        }

        public Vector4(final CompoundNBT nbt)
        {
            this();
            this.x = nbt.getFloat("x");
            this.y = nbt.getFloat("y");
            this.z = nbt.getFloat("z");
            this.w = nbt.getFloat("w");
        }

        public Vector4(final double posX, final double posY, final double posZ, final float w)
        {
            this.x = (float) posX;
            this.y = (float) posY;
            this.z = (float) posZ;
            this.w = w;
        }

        public Vector4(final Entity e)
        {
            this(e.getPosX(), e.getPosY(), e.getPosZ(), e.dimension.getId());
        }

        public Vector4(final String toParse)
        {
            final String[] vals = toParse.split(" ");
            if (vals.length == 4)
            {
                this.x = Float.parseFloat(vals[0]);
                this.y = Float.parseFloat(vals[1]);
                this.z = Float.parseFloat(vals[2]);
                this.w = Float.parseFloat(vals[3]);
            }
        }

        public Vector4(final BlockPos moveTo, final int dimension)
        {
            this(moveTo.getX(), moveTo.getY(), moveTo.getZ(), dimension);
        }

        public Vector4 copy()
        {
            return new Vector4(this.x, this.y, this.z, this.w);
        }

        @Override
        public boolean equals(final Object o)
        {
            if (o instanceof Vector4)
            {
                final Vector4 v = (Vector4) o;
                return v.x == this.x && v.y == this.y && v.z == this.z && v.w == this.w;
            }

            return super.equals(o);
        }

        @Override
        public String toString()
        {
            return "x:" + this.x + " y:" + this.y + " z:" + this.z + " w:" + this.w;
        }

        public boolean withinDistance(final float distance, final Vector4 toCheck)
        {
            if ((int) this.w == (int) toCheck.w && toCheck.x >= this.x - distance && toCheck.z >= this.z - distance
                    && toCheck.y >= this.y - distance && toCheck.y <= this.y + distance && toCheck.x <= this.x
                            + distance && toCheck.z <= this.z + distance) return true;

            return false;
        }

        public void writeToNBT(final CompoundNBT nbt)
        {
            nbt.putFloat("x", this.x);
            nbt.putFloat("y", this.y);
            nbt.putFloat("z", this.z);
            nbt.putFloat("w", this.w);
        }

        public void add(final Vector4 offset)
        {
            this.x += offset.x;
            this.y += offset.y;
            this.z += offset.z;
        }

        public void sub(final Vector4 offset)
        {
            this.x -= offset.x;
            this.y -= offset.y;
            this.z -= offset.z;
        }

        public double lengthSquared()
        {
            return this.x * this.x + this.y * this.y + this.z * this.z;
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
            this.overworld = entity.getServer().getWorld(DimensionType.OVERWORLD);
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
        private final Vector4     dest;
        private final boolean     sound;

        public TransferTicker(final ServerWorld destWorld, final Entity entity, final Vector4 dest, final boolean sound)
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
            if (event.world == this.entity.getEntityWorld() && event.phase == Phase.END)
            {
                MinecraftForge.EVENT_BUS.unregister(this);
                Transporter.transferMob(this.destWorld, this.dest, this.entity);
                if (this.sound)
                {
                    this.destWorld.playSound(this.dest.x, this.dest.y, this.dest.z,
                            SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.BLOCKS, 1.0F, 1.0F, false);
                    this.entity.playSound(SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
                }

            }
        }
    }

    public static Entity transferTo(final Entity entity, final Vector4 dest)
    {
        return Transporter.transferTo(entity, dest, false);
    }

    public static Entity transferTo(final Entity entity, final Vector4 dest, final boolean sound)
    {
        if (entity.getEntityWorld() instanceof ServerWorld)
        {
            new InvulnTicker(entity);
            if (dest.w == entity.dimension.getId()) return Transporter.moveMob(entity, dest);
            final ServerWorld destWorld = entity.getServer().getWorld(DimensionType.getById((int) dest.w));
            if (entity instanceof ServerPlayerEntity)
            {

                final ServerPlayerEntity player = (ServerPlayerEntity) entity;
                player.invulnerableDimensionChange = true;
                player.teleport(destWorld, dest.x, dest.y, dest.z, entity.rotationYaw, entity.rotationPitch);
                if (sound)
                {
                    destWorld.playSound(dest.x, dest.y, dest.z, SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                            SoundCategory.BLOCKS, 1.0F, 1.0F, false);
                    player.playSound(SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
                }
                player.invulnerableDimensionChange = false;
                return player;
            }
            else // Schedule the transfer for end of tick.
                new TransferTicker(destWorld, entity, dest, sound);
            return entity;
        }
        return entity;
    }

    private static void transferMob(final ServerWorld destWorld, final Vector4 dest, final Entity entity)
    {
        ServerPlayerEntity player = null;
        if (entity instanceof ServerPlayerEntity)
        {
            player = (ServerPlayerEntity) entity;
            player.invulnerableDimensionChange = true;
        }
        final ServerWorld serverworld = (ServerWorld) entity.getEntityWorld();
        entity.dimension = destWorld.dimension.getType();
        Transporter.removeMob(serverworld, entity, true);
        entity.revive();
        entity.setLocationAndAngles(dest.x, dest.y, dest.z, entity.rotationYaw, entity.rotationPitch);
        entity.setWorld(destWorld);
        Transporter.addMob(destWorld, entity);
        if (player != null) player.invulnerableDimensionChange = false;
    }

    private static void addMob(final ServerWorld world, final Entity entity)
    {
        if (net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                new net.minecraftforge.event.entity.EntityJoinWorldEvent(entity, world))) return;
        final IChunk ichunk = world.getChunk(MathHelper.floor(entity.getPosX() / 16.0D), MathHelper.floor(entity
                .getPosZ() / 16.0D), ChunkStatus.FULL, true);
        if (ichunk instanceof Chunk) ichunk.addEntity(entity);
        world.addEntityIfNotDuplicate(entity);
    }

    private static void removeMob(final ServerWorld world, final Entity entity, final boolean keepData)
    {
        entity.remove(keepData);
        world.removeEntity(entity, keepData);
    }

    private static Entity moveMob(final Entity entity, final Vector4 dest)
    {
        if (entity instanceof ServerPlayerEntity)
        {
            ((ServerPlayerEntity) entity).connection.setPlayerLocation(dest.x, dest.y, dest.z, entity.rotationYaw,
                    entity.rotationPitch);
            ((ServerPlayerEntity) entity).connection.captureCurrentPosition();
        }
        else entity.setLocationAndAngles(dest.x, dest.y, dest.z, entity.rotationYaw, entity.rotationPitch);
        return entity;
    }
}
