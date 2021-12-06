package thut.essentials.economy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.commons.io.FileUtils;

import com.google.common.collect.Lists;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import thut.essentials.Essentials;
import thut.essentials.economy.EconomyManager.Account;
import thut.essentials.economy.EconomyManager.Shop;

public class EconomySaveHandler
{
    static ExclusionStrategy exclusion = new ExclusionStrategy()
    {
        @Override
        public boolean shouldSkipField(final FieldAttributes f)
        {
            final String name = f.getName();
            return name.startsWith("_");
        }

        @Override
        public boolean shouldSkipClass(final Class<?> clazz)
        {
            return false;
        }
    };

    public static File getGlobalFolder()
    {
        final MinecraftServer server = Essentials.server;
        final Path path = server.getWorldPath(new LevelResource("economy"));
        final File file = path.toFile();
        if (!file.exists()) file.mkdirs();
        return file;
    }

    public static void saveGlobalData()
    {
        final MinecraftServer server = Essentials.server;
        if (server == null) return;
        final Gson gson = new GsonBuilder().addSerializationExclusionStrategy(EconomySaveHandler.exclusion)
                .setPrettyPrinting().create();
        EconomyManager.getInstance().version = EconomyManager.VERSION;
        final String json = gson.toJson(EconomyManager.getInstance());
        final File teamsFile = new File(EconomySaveHandler.getGlobalFolder(), "economy.json");
        try
        {
            FileUtils.writeStringToFile(teamsFile, json, "UTF-8");
        }
        catch (final IOException e)
        {
            e.printStackTrace();
        }
    }

    public static void loadGlobalData()
    {
        final MinecraftServer server = Essentials.server;
        if (server != null)
        {
            final File teamsFile = new File(EconomySaveHandler.getGlobalFolder(), "economy.json");
            if (teamsFile.exists()) try
            {
                final Gson gson = new GsonBuilder().addDeserializationExclusionStrategy(EconomySaveHandler.exclusion)
                        .setPrettyPrinting().create();
                final String json = FileUtils.readFileToString(teamsFile, "UTF-8");
                EconomyManager.instance = gson.fromJson(json, EconomyManager.class);
                for (final Entry<UUID, Account> entry : EconomyManager.instance.bank.entrySet())
                {
                    final Account account = entry.getValue();
                    final UUID id = entry.getKey();
                    account._id = id;
                    EconomyManager.instance._revBank.put(account, id);
                    final List<Shop> shops = Lists.newArrayList(account.shops);
                    for (final Shop shop : shops)
                    {
                        if (shop.location == null)
                        {
                            account.shops.remove(shop);
                            Essentials.LOGGER.error("Removing corrupted shop!");
                            continue;
                        }
                        account._shopMap.put(shop.location, shop);
                        EconomyManager.instance._shopMap.put(shop.location, account);
                    }
                }
                return;
            }
            catch (final Exception e)
            {
                e.printStackTrace();
            }
        }
        if (EconomyManager.instance == null) EconomyManager.instance = new EconomyManager();
        EconomySaveHandler.saveGlobalData();
    }
}