package thut.essentials.util;

import java.util.List;

import javax.annotation.Nullable;
import javax.vecmath.Vector3f;

import com.google.common.collect.Lists;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityTracker;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Teleporter;
import net.minecraft.world.ServerWorld;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class Transporter
{
    @SuppressWarnings("serial")
    public static class Vector3 extends Vector3f
    {

        public Vector3(double x, double y, double z)
        {
            super((float) x, (float) y, (float) z);
        }

        public Vector3(BlockPos moveTo)
        {
            this(moveTo.getX(), moveTo.getY(), moveTo.getZ());
        }

        public int intY()
        {
            return MathHelper.floor(y);
        }

        public int intX()
        {
            return MathHelper.floor(x);
        }

        public int intZ()
        {
            return MathHelper.floor(z);
        }

        public AxisAlignedBB getAABB()
        {
            return new AxisAlignedBB(x, y, z, x, y, z);
        }
    }

    // From RFTools.
    public static class TTeleporter extends Teleporter
    {
        private final ServerWorld worldServerInstance;
        private boolean           move = true;
        private double            x;
        private double            y;
        private double            z;

        public TTeleporter(ServerWorld world, double x, double y, double z)
        {
            super(world);
            this.worldServerInstance = world;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public TTeleporter(ServerWorld worldServerForDimension)
        {
            super(worldServerForDimension);
            this.worldServerInstance = worldServerForDimension;
            move = false;
        }

        @Override
        public void placeInPortal(Entity pEntity, float rotationYaw)
        {
            if (!move) return;
            this.worldServerInstance.getBlockState(new BlockPos((int) this.x, (int) this.y, (int) this.z));
            doMoveEntity(pEntity, this.x, this.y, this.z, pEntity.rotationYaw, pEntity.rotationPitch);
            pEntity.motionX = 0.0f;
            pEntity.motionY = 0.0f;
            pEntity.motionZ = 0.0f;
        }

        @Override
        public void removeStalePortalLocations(long par1)
        {
        }

        @Override
        public boolean makePortal(Entity p_85188_1_)
        {
            return true;
        }
    }

    public static class DeSticker
    {
        final ServerPlayerEntity player;
        final float          x0;
        final float          y0;
        final float          z0;
        final float          yaw;
        final int            dimension;
        int                  tick = 0;

        public DeSticker(ServerPlayerEntity player)
        {
            this.player = player;
            this.dimension = player.dimension;
            this.x0 = (float) player.posX;
            this.y0 = (float) player.posY;
            this.z0 = (float) player.posZ;
            this.yaw = player.rotationYaw;
        }

        @SubscribeEvent
        public void logout(PlayerLoggedOutEvent evt)
        {
            if (evt.player.getUniqueID().equals(player.getUniqueID()))
            {
                MinecraftForge.EVENT_BUS.unregister(this);
            }
        }

        @SubscribeEvent
        public void tick(TickEvent.ServerTickEvent evt)
        {
            if (evt.phase == Phase.START) return;
            boolean done = dimension != player.dimension;
            float dx = (float) player.posX - x0;
            float dz = (float) player.posZ - z0;
            float dyaw = player.rotationYaw - yaw;
            done = done || dx != 0 || dz != 0 || dyaw != 0;
            if (done)
            {
                EntityTracker tracker = player.getServerWorld().getEntityTracker();
                tracker.updateVisibility(player);
                MinecraftForge.EVENT_BUS.unregister(this);
            }
            else if (tick % 20 == 0)
            {
                player.connection.setPlayerLocation(player.posX, y0 + 0.5, player.posZ, player.rotationYaw,
                        player.rotationPitch);
            }
            tick++;
        }
    }

    public static class ReMounter
    {
        final Entity[] riders;
        final Entity   theMount;
        final int      dim;
        final long     time;

        public ReMounter(Entity mount, int dim, Entity... entities)
        {
            riders = entities;
            theMount = mount;
            time = mount.getEntityWorld().getGameTime();
            this.dim = dim;
        }

        @SubscribeEvent
        public void tick(TickEvent.ServerTickEvent evt)
        {
            if (evt.phase != TickEvent.Phase.END) return;
            if (theMount.isDead) MinecraftForge.EVENT_BUS.unregister(this);
            boolean doneAll = theMount.getEntityWorld().getGameTime() > time;
            for (int i = riders.length - 1; i >= 0; i--)
            {
                Entity theEntity = riders[i];
                if (theEntity == null) continue;
                if (dim != theEntity.dimension)
                {
                    doneAll = false;
                    if (theEntity instanceof ServerPlayerEntity)
                    {
                        ReflectionHelper.setPrivateValue(ServerPlayerEntity.class, (ServerPlayerEntity) theEntity, true,
                                "invulnerableDimensionChange", "field_184851_cj", "ck");
                        theEntity.getServer().getPlayerList().transferPlayerToDimension((ServerPlayerEntity) theEntity, dim,
                                new TTeleporter(theEntity.getServer().getWorld(dim)));
                    }
                    else
                    {
                        // Handle moving non players.
                    }
                }
                else
                {

                }
            }
            int num = 0;
            if (doneAll) for (int i = riders.length - 1; i >= 0; i--)
            {
                Entity theEntity = riders[i];
                if (theEntity == null)
                {
                    num++;
                    continue;
                }
                doMoveEntity(theEntity, theMount.posX, theMount.posY, theMount.posZ, theEntity.rotationYaw,
                        theEntity.rotationPitch);
                boolean mounted = theEntity.startRiding(theMount);
                doneAll = doneAll && mounted;
                if (mounted)
                {
                    riders[i] = null;
                    num++;
                }
            }
            if (doneAll || riders.length == num) MinecraftForge.EVENT_BUS.unregister(this);
        }
    }

    private static void doMoveEntity(Entity theEntity, double x, double y, double z, float yaw, float pitch)
    {
        if (theEntity instanceof ServerPlayerEntity)
        {
            theEntity.stopRiding();
            ((ServerPlayerEntity) theEntity).connection.setPlayerLocation(x, y, z, yaw, pitch);
            MinecraftForge.EVENT_BUS.register(new DeSticker((ServerPlayerEntity) theEntity));
        }
        else theEntity.setLocationAndAngles(x, y, z, yaw, pitch);
    }

    public static Entity teleportEntity(Entity entity, Vector3 t2, int dimension)
    {
        if (!DimensionManager.isDimensionRegistered(dimension)) return entity;
        if (entity.isPassenger())
        {
            Entity mount = entity.getRidingEntity();
            mount = teleportEntity(mount, t2, dimension);
            return entity;
        }
        if (dimension != entity.dimension)
        {
            entity = transferToDimension(entity, t2, dimension);
            for (Entity e : entity.getRecursivePassengers())
            {
                e = transferToDimension(e, t2, dimension);
            }
        }
        int x = t2.intX() >> 4;
        int z = t2.intZ() >> 4;
        for (int i = x - 1; i <= x + 1; i++)
            for (int j = z - 1; j <= z + 1; j++)
            {
                entity.world.getChunk(x, z);
            }
        doMoveEntity(entity, t2.x, t2.y, t2.z, entity.rotationYaw, entity.rotationPitch);
        List<Entity> passengers = Lists.newArrayList(entity.getPassengers());
        for (Entity e : passengers)
        {
            e.stopRiding();
            doMoveEntity(e, t2.x, t2.y, t2.z, e.rotationYaw, e.rotationPitch);
        }
        if (!passengers.isEmpty()) MinecraftForge.EVENT_BUS
                .register(new ReMounter(entity, dimension, passengers.toArray(new Entity[passengers.size()])));
        ServerWorld world = entity.getServer().getWorld(dimension);
        EntityTracker tracker = world.getEntityTracker();
        tracker.untrack(entity);
        tracker.track(entity);
        return entity;
    }

    // From RFTools.
    private static Entity transferToDimension(Entity entityIn, Vector3 t2, int dimension)
    {
        int oldDimension = entityIn.world.dimension.getDimension();
        if (oldDimension == dimension) return entityIn;
        if (!(entityIn instanceof ServerPlayerEntity)) { return changeDimension(entityIn, t2, dimension); }
        MinecraftServer server = entityIn.world.getMinecraftServer();
        ServerWorld worldServer = server.getWorld(dimension);
        Teleporter teleporter = new TTeleporter(worldServer, t2.x, t2.y, t2.z);
        ServerPlayerEntity playerIn = (ServerPlayerEntity) entityIn;
        // Prevents death due to say world border size differences.
        ReflectionHelper.setPrivateValue(ServerPlayerEntity.class, playerIn, true, "invulnerableDimensionChange",
                "field_184851_cj", "ck");
        // Use player list to actually do the transfer.
        worldServer.getMinecraftServer().getPlayerList().transferPlayerToDimension(playerIn, dimension, teleporter);
        // Re-Sync exp bar.
        playerIn.addExperienceLevel(0);
        return playerIn;
    }

    @Nullable
    // From Advanced Rocketry
    public static Entity changeDimension(Entity entityIn, Vector3 t2, int dimensionIn)
    {
        if (entityIn.dimension == dimensionIn) return entityIn;
        if (!entityIn.world.isRemote && !entityIn.isDead)
        {
            List<Entity> passengers = entityIn.getPassengers();

            if (!net.minecraftforge.common.ForgeHooks.onTravelToDimension(entityIn, dimensionIn)) return null;
            entityIn.world.profiler.startSection("changeDimension");
            MinecraftServer minecraftserver = entityIn.getServer();
            int i = entityIn.dimension;
            ServerWorld worldserver = minecraftserver.getWorld(i);
            ServerWorld worldserver1 = minecraftserver.getWorld(dimensionIn);
            entityIn.dimension = dimensionIn;

            if (i == 1 && dimensionIn == 1)
            {
                worldserver1 = minecraftserver.getWorld(0);
                entityIn.dimension = 0;
            }
            CompoundNBT tag = new CompoundNBT();
            entityIn.writeToNBT(tag);
            entityIn.world.removeEntity(entityIn);
            entityIn.readFromNBT(tag);
            entityIn.isDead = false;
            entityIn.world.profiler.startSection("reposition");

            double d0 = entityIn.posX;
            double d1 = entityIn.posZ;
            d0 = MathHelper.clamp((int) d0, -29999872, 29999872);
            d1 = MathHelper.clamp((int) d1, -29999872, 29999872);
            d0 = MathHelper.clamp(d0, worldserver1.getWorldBorder().minX(), worldserver1.getWorldBorder().maxX());
            d1 = MathHelper.clamp(d1, worldserver1.getWorldBorder().minZ(), worldserver1.getWorldBorder().maxZ());
            float f = entityIn.rotationYaw;
            doMoveEntity(entityIn, d0, entityIn.posY, d1, 90.0F, 0.0F);
            Teleporter teleporter = new TTeleporter(worldserver1, t2.x, t2.y, t2.z);
            teleporter.placeInExistingPortal(entityIn, f);
            worldserver.updateEntityWithOptionalForce(entityIn, false);
            entityIn.world.profiler.endStartSection("reloading");
            Entity entity = CompatWrapper.createEntity(worldserver1, entityIn);
            if (entity != null)
            {
                entity.copyDataFromOld(entityIn);
                entity.forceSpawn = true;
                worldserver1.spawnEntity(entity);
                worldserver1.updateEntityWithOptionalForce(entity, true);
                // Fix that darn random crash?
                worldserver.resetUpdateEntityTick();
                worldserver1.resetUpdateEntityTick();
                // Transfer the player if applicable
                // Need to handle our own removal to avoid race condition
                // where player is mounted on client on the old entity but
                // is already mounted to the new one on server
                MinecraftForge.EVENT_BUS.register(
                        new ReMounter(entity, dimensionIn, passengers.toArray(new Entity[passengers.size()])));
            }
            entityIn.isDead = true;
            entityIn.world.profiler.endSection();
            worldserver.resetUpdateEntityTick();
            worldserver1.resetUpdateEntityTick();
            entityIn.world.profiler.endSection();
            return entity;
        }
        return null;
    }
}
