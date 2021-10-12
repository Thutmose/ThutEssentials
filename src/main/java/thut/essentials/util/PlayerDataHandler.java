package thut.essentials.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fmllegacy.LogicalSidedProvider;

public class PlayerDataHandler
{
    private static interface IPlayerData
    {
        String getIdentifier();

        String dataFileName();

        boolean shouldSync();

        void writeToNBT(CompoundTag tag);

        void readFromNBT(CompoundTag tag);

        void readSync(ByteBuf data);

        void writeSync(ByteBuf data);
    }

    public static abstract class PlayerData implements IPlayerData
    {
        @Override
        public void readSync(final ByteBuf data)
        {
        }

        @Override
        public void writeSync(final ByteBuf data)
        {
        }
    }

    /**
     * Generic data to store for each player, this gives another place besides
     * in the player's entity data to store information.
     */
    public static class PlayerCustomData extends PlayerData
    {
        public CompoundTag tag = new CompoundTag();

        public PlayerCustomData()
        {
        }

        @Override
        public String getIdentifier()
        {
            return "thutessentials";
        }

        @Override
        public String dataFileName()
        {
            return "thutEssentials";
        }

        @Override
        public boolean shouldSync()
        {
            return false;
        }

        @Override
        public void writeToNBT(final CompoundTag tag)
        {
            tag.put("data", this.tag);
        }

        @Override
        public void readFromNBT(final CompoundTag tag)
        {
            this.tag = tag.getCompound("data");
        }
    }

    public static class PlayerDataManager
    {
        Map<Class<? extends PlayerData>, PlayerData> data  = Maps.newHashMap();
        Map<String, PlayerData>                      idMap = Maps.newHashMap();
        final String                                 uuid;

        public PlayerDataManager(final String uuid)
        {
            this.uuid = uuid;
            for (final Class<? extends PlayerData> type : PlayerDataHandler.dataMap)
                try
                {
                    final PlayerData toAdd = type.newInstance();
                    this.data.put(type, toAdd);
                    this.idMap.put(toAdd.getIdentifier(), toAdd);
                }
                catch (final InstantiationException e)
                {
                    e.printStackTrace();
                }
                catch (final IllegalAccessException e)
                {
                    e.printStackTrace();
                }
        }

        @SuppressWarnings("unchecked")
        public <T extends PlayerData> T getData(final Class<T> type)
        {
            return (T) this.data.get(type);
        }

        public PlayerData getData(final String dataType)
        {
            return this.idMap.get(dataType);
        }
    }

    public static Set<Class<? extends PlayerData>> dataMap = Sets.newHashSet();

    static
    {
        PlayerDataHandler.dataMap.add(PlayerCustomData.class);
    }
    private static PlayerDataHandler INSTANCESERVER;

    public static PlayerDataHandler getInstance()
    {
        return PlayerDataHandler.INSTANCESERVER != null ? PlayerDataHandler.INSTANCESERVER
                : (PlayerDataHandler.INSTANCESERVER = new PlayerDataHandler());
    }

    public static void clear()
    {
        if (PlayerDataHandler.INSTANCESERVER != null) MinecraftForge.EVENT_BUS.unregister(
                PlayerDataHandler.INSTANCESERVER);
        PlayerDataHandler.INSTANCESERVER = null;
    }

    public static void saveAll()
    {

    }

    public static CompoundTag getCustomDataTag(final Player player)
    {
        final PlayerDataManager manager = PlayerDataHandler.getInstance().getPlayerData(player);
        final PlayerCustomData data = manager.getData(PlayerCustomData.class);
        return data.tag;
    }

    public static CompoundTag getCustomDataTag(final String player)
    {
        final PlayerDataManager manager = PlayerDataHandler.getInstance().getPlayerData(player);
        final PlayerCustomData data = manager.getData(PlayerCustomData.class);
        return data.tag;
    }

    public static void saveCustomData(final Player player)
    {
        PlayerDataHandler.saveCustomData(player.getStringUUID());
    }

    public static void saveCustomData(final String cachedUniqueIdString)
    {
        PlayerDataHandler.getInstance().save(cachedUniqueIdString, "thutessentials");
    }

    public static File getFileForUUID(final String uuid, final String fileName)
    {
        final MinecraftServer server = LogicalSidedProvider.INSTANCE.get(LogicalSide.SERVER);
        Path path = server.getWorldPath(new LevelResource("thutessentials"));
        // This is to the uuid specific folder
        path = path.resolve(uuid);
        final File dir = path.toFile();
        // and this if the file itself
        path = path.resolve(fileName + ".dat");
        final File file = path.toFile();
        if (!file.exists()) dir.mkdirs();
        return file;
    }

    private final Map<String, PlayerDataManager> data = Maps.newHashMap();

    public PlayerDataHandler()
    {
        MinecraftForge.EVENT_BUS.register(this);
    }

    public PlayerDataManager getPlayerData(final Player player)
    {
        return this.getPlayerData(player.getStringUUID());
    }

    public PlayerDataManager getPlayerData(final String uuid)
    {
        PlayerDataManager manager = this.data.get(uuid);
        if (manager == null) manager = this.load(uuid);
        return manager;
    }

    public PlayerDataManager getPlayerData(final UUID uniqueID)
    {
        return this.getPlayerData(uniqueID.toString());
    }

    @SubscribeEvent
    public void cleanupOfflineData(final WorldEvent.Save event)
    {
        // Whenever overworld saves, check player list for any that are not
        // online, and remove them. This is done here, and not on logoff, as
        // something may have requested the manager for an offline player, which
        // would have loaded it.
        final Set<String> toUnload = Sets.newHashSet();
        final MinecraftServer server = LogicalSidedProvider.INSTANCE.get(LogicalSide.SERVER);
        for (final String uuid : this.data.keySet())
        {
            final ServerPlayer player = server.getPlayerList().getPlayer(UUID.fromString(uuid));
            if (player == null) toUnload.add(uuid);
        }
        for (final String s : toUnload)
        {
            this.save(s);
            this.data.remove(s);
        }
    }

    public PlayerDataManager load(final String uuid)
    {
        final PlayerDataManager manager = new PlayerDataManager(uuid);
        for (final PlayerData data : manager.data.values())
        {
            final String fileName = data.dataFileName();
            File file = null;
            try
            {
                file = PlayerDataHandler.getFileForUUID(uuid, fileName);
            }
            catch (final Exception e)
            {

            }
            if (file != null && file.exists()) try
            {
                final FileInputStream fileinputstream = new FileInputStream(file);
                final CompoundTag nbttagcompound = NbtIo.readCompressed(fileinputstream);
                fileinputstream.close();
                data.readFromNBT(nbttagcompound.getCompound("Data"));
            }
            catch (final IOException e)
            {
                e.printStackTrace();
            }
        }
        this.data.put(uuid, manager);
        return manager;
    }

    public void save(final String uuid, final String dataType)
    {
        final PlayerDataManager manager = this.data.get(uuid);
        if (manager != null) for (final PlayerData data : manager.data.values())
        {
            if (!data.getIdentifier().equals(dataType)) continue;
            final String fileName = data.dataFileName();
            final File file = PlayerDataHandler.getFileForUUID(uuid, fileName);
            if (file != null)
            {
                final CompoundTag nbttagcompound = new CompoundTag();
                data.writeToNBT(nbttagcompound);
                final CompoundTag nbttagcompound1 = new CompoundTag();
                nbttagcompound1.put("Data", nbttagcompound);
                try
                {
                    final FileOutputStream fileoutputstream = new FileOutputStream(file);
                    NbtIo.writeCompressed(nbttagcompound1, fileoutputstream);
                    fileoutputstream.close();
                }
                catch (final IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    public void save(final String uuid)
    {
        final PlayerDataManager manager = this.data.get(uuid);
        if (manager != null) for (final PlayerData data : manager.data.values())
        {
            final String fileName = data.dataFileName();
            final File file = PlayerDataHandler.getFileForUUID(uuid, fileName);
            if (file != null)
            {
                final CompoundTag nbttagcompound = new CompoundTag();
                data.writeToNBT(nbttagcompound);
                final CompoundTag nbttagcompound1 = new CompoundTag();
                nbttagcompound1.put("Data", nbttagcompound);
                try
                {
                    final FileOutputStream fileoutputstream = new FileOutputStream(file);
                    NbtIo.writeCompressed(nbttagcompound1, fileoutputstream);
                    fileoutputstream.close();
                }
                catch (final IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }
}