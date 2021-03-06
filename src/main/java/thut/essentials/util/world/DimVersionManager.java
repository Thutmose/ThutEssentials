package thut.essentials.util.world;

import java.io.File;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.IntNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.SaveFormat.LevelSave;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import thut.essentials.Essentials;
import thut.essentials.events.TeleLoadEvent;
import thut.essentials.util.Transporter.TeleDest;

public class DimVersionManager
{
    public interface IVersioned
    {
        int getVersion();

        void setVersion(int vers);
    }

    public static class VersionHolder implements IVersioned, ICapabilitySerializable<IntNBT>
    {
        private final LazyOptional<IVersioned> holder = LazyOptional.of(() -> this);

        int vers = 0;

        public VersionHolder()
        {
        }

        public VersionHolder(final int vers)
        {
            this.vers = vers;
        }

        @Override
        public IntNBT serializeNBT()
        {
            return IntNBT.valueOf(this.vers);
        }

        @Override
        public void deserializeNBT(final IntNBT nbt)
        {
            this.vers = nbt.getAsInt();
        }

        @Override
        public int getVersion()
        {
            return this.vers;
        }

        @Override
        public void setVersion(final int vers)
        {
            this.vers = vers;
        }

        @Override
        public <T> LazyOptional<T> getCapability(final Capability<T> cap, final Direction side)
        {
            return DimVersionManager.CAPABILITY.orEmpty(cap, this.holder);
        }
    }

    public static class Storage implements Capability.IStorage<IVersioned>
    {
        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Override
        public void readNBT(final Capability<IVersioned> capability, final IVersioned instance, final Direction side,
                final INBT nbt)
        {
            if (instance instanceof ICapabilitySerializable) ((ICapabilitySerializable) instance).deserializeNBT(nbt);
        }

        @Override
        public INBT writeNBT(final Capability<IVersioned> capability, final IVersioned instance, final Direction side)
        {
            if (instance instanceof ICapabilitySerializable<?>) return ((ICapabilitySerializable<?>) instance)
                    .serializeNBT();
            return null;
        }
    }

    public static void init()
    {
        CapabilityManager.INSTANCE.register(IVersioned.class, new Storage(), VersionHolder::new);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, DimVersionManager::handleTeleLoading);
        MinecraftForge.EVENT_BUS.addGenericListener(World.class, DimVersionManager::attach);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, DimVersionManager::handleWorldLoad);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, DimVersionManager::handleWarnPlayer);
    }

    private static final ResourceLocation CAPTAG = new ResourceLocation(Essentials.MODID, "version");

    @CapabilityInject(IVersioned.class)
    public static final Capability<IVersioned> CAPABILITY = null;

    public static void attach(final AttachCapabilitiesEvent<World> event)
    {
        if (!(event.getObject() instanceof ServerWorld)) return;
        final ServerWorld world = (ServerWorld) event.getObject();
        if (!Essentials.config.versioned_dim_keys.contains(world.dimension().location())) return;
        if (event.getCapabilities().containsKey(DimVersionManager.CAPTAG)) return;
        event.addCapability(DimVersionManager.CAPTAG, new VersionHolder(Essentials.config.dim_verison));
    }

    public static void handleTeleLoading(final TeleLoadEvent event)
    {
        final TeleDest dest = event.getOverride();
        if (dest == null) return;
        if (dest.version != Essentials.config.dim_verison)
        {
            if (!Essentials.config.versioned_dim_keys.contains(dest.getPos().getDimension().location())) return;
            Essentials.LOGGER.info("Invalidating stale teledest {} ({})", dest.getName(), dest.getPos());
            event.setCanceled(true);
            event.setOverride(null);
        }
    }

    public static void handleWorldLoad(final WorldEvent.Load event)
    {
        if (!(event.getWorld() instanceof ServerWorld)) return;
        final ServerWorld world = (ServerWorld) event.getWorld();
        final IVersioned vers = world.getCapability(DimVersionManager.CAPABILITY).orElse(null);
        // Not all worlds will have this, only ones to track!
        if (vers == null) return;
        if (vers.getVersion() < Essentials.config.dim_verison)
        {
            final LevelSave var = world.getServer().storageSource;
            final File file = var.getDimensionPath(world.dimension());
            int i = 0;
            File named_file = new File(file.getParent(), file.getName() + "_" + i++);
            while (named_file.exists())
                named_file = new File(file.getParent(), file.getName() + "_" + i);
            if (file.exists())
            {
                final File prev = new File(file.getParent(), file.getName());
                prev.mkdirs();
                new File(prev, "data").mkdirs();
                file.renameTo(named_file);
            }
        }
        world.getChunkSource().getGenerator().strongholdSeed = world.getSeed();
        vers.setVersion(Essentials.config.dim_verison);
    }

    public static void handleWarnPlayer(final EntityJoinWorldEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayerEntity)) return;
        if (!Essentials.config.versioned_dim_warning) return;
        final ServerWorld world = (ServerWorld) event.getWorld();
        if (!Essentials.config.versioned_dim_keys.contains(world.dimension().location())) return;
        event.getEntity().sendMessage(Essentials.config.getMessage("thutessentials.dimversions.warning"),
                Util.NIL_UUID);
    }

}
