package thut.essentials.util.world;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import thut.essentials.Essentials;
import thut.essentials.events.TeleLoadEvent;
import thut.essentials.util.ChatHelper;
import thut.essentials.util.teleporting.TeleDest;

import java.io.File;

public class DimVersionManager
{
    public interface IVersioned
    {
        int getVersion();

        void setVersion(int vers);
    }

    public static class VersionHolder extends SavedData implements IVersioned
    {
        // Create new instance of saved data
        public static VersionHolder create()
        {
            return new VersionHolder();
        }

        // Load existing instance of saved data
        public static VersionHolder load(CompoundTag tag, HolderLookup.Provider lookupProvider)
        {
            VersionHolder data = VersionHolder.create();
            // Load saved data
            if (tag.contains("V")) data.vers = tag.getInt("V");
            return data;
        }

        int vers = 0;

        public VersionHolder()
        {
            vers = Essentials.config.dim_verison;
        }

        @Override
        public CompoundTag save(CompoundTag compoundTag, HolderLookup.Provider provider)
        {
            compoundTag.putInt("V", this.getVersion());
            return compoundTag;
        }

        public VersionHolder(final int vers)
        {
            this.vers = vers;
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
    }

    public static void init()
    {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, DimVersionManager::handleTeleLoading);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, DimVersionManager::handleWorldLoad);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, DimVersionManager::handleWarnPlayer);
    }

    private static void handleTeleLoading(final TeleLoadEvent event)
    {
        final TeleDest dest = event.getOverride();
        if (dest == null) return;
        if (dest.version != Essentials.config.dim_verison)
        {
            if (!Essentials.config.versioned_dim_keys.contains(dest.getPos().dimension().location())) return;
            Essentials.LOGGER.info("Invalidating stale teledest {} ({})", dest.getName(), dest.getPos());
            event.setCanceled(true);
            event.setOverride(null);
        }
    }

    private static void handleWorldLoad(final LevelEvent.Load event)
    {
        if (!(event.getLevel() instanceof ServerLevel world)) return;
        final IVersioned vers = world.getDataStorage().get(new SavedData.Factory<>(VersionHolder::create, VersionHolder::load), "te_version");
        // Not all worlds will have this, only ones to track!
        if (vers == null) return;
        if (vers.getVersion() < Essentials.config.dim_verison)
        {
            final LevelStorageAccess var = world.getServer().storageSource;
            final File file = var.getDimensionPath(world.dimension()).toFile();
            int i = 0;
            File named_file = new File(file.getParent(), file.getName() + "_" + i++);
            while (named_file.exists()) named_file = new File(file.getParent(), file.getName() + "_" + i);
            if (file.exists())
            {
                final File prev = new File(file.getParent(), file.getName());
                prev.mkdirs();
                new File(prev, "data").mkdirs();
                file.renameTo(named_file);
            }
        }
        vers.setVersion(Essentials.config.dim_verison);
    }

    private static void handleWarnPlayer(final EntityJoinLevelEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!Essentials.config.versioned_dim_warning) return;
        final ServerLevel world = (ServerLevel) event.getLevel();
        if (!Essentials.config.versioned_dim_keys.contains(world.dimension().location())) return;
        ChatHelper.sendSystemMessage(player, Essentials.config.getMessage("thutessentials.dimversions.warning"));
    }

}
