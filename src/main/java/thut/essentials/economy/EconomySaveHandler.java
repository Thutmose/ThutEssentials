package thut.essentials.economy;

import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.commons.io.FileUtils;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraftforge.fml.common.FMLCommonHandler;
import thut.essentials.economy.EconomyManager.Account;
import thut.essentials.economy.EconomyManager.Shop;

public class EconomySaveHandler
{
    static ExclusionStrategy exclusion = new ExclusionStrategy()
    {
        @Override
        public boolean shouldSkipField(FieldAttributes f)
        {
            String name = f.getName();
            return name.startsWith("_");
        }

        @Override
        public boolean shouldSkipClass(Class<?> clazz)
        {
            return false;
        }
    };

    public static File getGlobalFolder()
    {
        String folder = FMLCommonHandler.instance().getMinecraftServerInstance().getFolderName();
        File file = FMLCommonHandler.instance().getSavesDirectory();
        File saveFolder = new File(file, folder);
        File teamsFolder = new File(saveFolder, "economy");
        if (!teamsFolder.exists()) teamsFolder.mkdirs();
        return teamsFolder;
    }

    public static void saveGlobalData()
    {
        if (FMLCommonHandler.instance().getMinecraftServerInstance() == null) return;
        Gson gson = new GsonBuilder().addSerializationExclusionStrategy(exclusion).setPrettyPrinting().create();
        EconomyManager.getInstance().version = EconomyManager.VERSION;
        String json = gson.toJson(EconomyManager.getInstance());
        File teamsFile = new File(getGlobalFolder(), "economy.json");
        try
        {
            FileUtils.writeStringToFile(teamsFile, json, "UTF-8");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public static void loadGlobalData()
    {
        if (FMLCommonHandler.instance().getMinecraftServerInstance() == null) return;
        File teamsFile = new File(getGlobalFolder(), "economy.json");
        if (teamsFile.exists())
        {
            try
            {
                Gson gson = new GsonBuilder().addDeserializationExclusionStrategy(exclusion).setPrettyPrinting()
                        .create();
                String json = FileUtils.readFileToString(teamsFile, "UTF-8");
                EconomyManager.instance = gson.fromJson(json, EconomyManager.class);
                for (Entry<UUID, Account> entry : EconomyManager.instance.bank.entrySet())
                {
                    Account account = entry.getValue();
                    UUID id = entry.getKey();
                    EconomyManager.instance._revBank.put(account, id);
                    for (Shop shop : account.shops)
                    {
                        account._shopMap.put(shop.location, shop);
                        EconomyManager.instance._shopMap.put(shop.location, account);
                    }
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            if (EconomyManager.instance == null) EconomyManager.instance = new EconomyManager();
            saveGlobalData();
        }
    }
}
