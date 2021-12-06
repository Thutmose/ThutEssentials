package thut.essentials.util.world;

import java.io.File;

import net.minecraft.Util;
import net.minecraft.core.Direction;
import net.minecraft.nbt.IntTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
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

    public static class VersionHolder implements IVersioned, ICapabilitySerializable<IntTag>
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
        public IntTag serializeNBT()
        {
            return IntTag.valueOf(this.vers);
        }

        @Override
        public void deserializeNBT(final IntTag nbt)
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

    public static void init()
    {
        MinecraftForge.EVENT_BUS.addListener(DimVersionManager::registerCapabilities);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, DimVersionManager::handleTeleLoading);
        MinecraftForge.EVENT_BUS.addGenericListener(Level.class, DimVersionManager::attach);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, DimVersionManager::handleWorldLoad);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, DimVersionManager::handleWarnPlayer);
    }

    private static final ResourceLocation CAPTAG = new ResourceLocation(Essentials.MODID, "version");

    public static final Capability<IVersioned> CAPABILITY = CapabilityManager.get(new CapabilityToken<>(){});

    private static void registerCapabilities(final RegisterCapabilitiesEvent event)
    {
        event.register(IVersioned.class);
    }

    private static void attach(final AttachCapabilitiesEvent<Level> event)
    {
        if (!(event.getObject() instanceof ServerLevel)) return;
        final ServerLevel world = (ServerLevel) event.getObject();
        if (!Essentials.config.versioned_dim_keys.contains(world.dimension().location())) return;
        if (event.getCapabilities().containsKey(DimVersionManager.CAPTAG)) return;
        event.addCapability(DimVersionManager.CAPTAG, new VersionHolder(Essentials.config.dim_verison));
    }

    private static void handleTeleLoading(final TeleLoadEvent event)
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

    private static void handleWorldLoad(final WorldEvent.Load event)
    {
        if (!(event.getWorld() instanceof ServerLevel)) return;
        final ServerLevel world = (ServerLevel) event.getWorld();
        final IVersioned vers = world.getCapability(DimVersionManager.CAPABILITY).orElse(null);
        // Not all worlds will have this, only ones to track!
        if (vers == null) return;
        if (vers.getVersion() < Essentials.config.dim_verison)
        {
            final LevelStorageAccess var = world.getServer().storageSource;
            final File file = var.getDimensionPath(world.dimension()).toFile();
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

    private static void handleWarnPlayer(final EntityJoinWorldEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer)) return;
        if (!Essentials.config.versioned_dim_warning) return;
        final ServerLevel world = (ServerLevel) event.getWorld();
        if (!Essentials.config.versioned_dim_keys.contains(world.dimension().location())) return;
        event.getEntity().sendMessage(Essentials.config.getMessage("thutessentials.dimversions.warning"),
                Util.NIL_UUID);
    }

}
