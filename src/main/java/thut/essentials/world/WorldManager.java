package thut.essentials.world;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.logging.Level;

import javax.xml.namespace.QName;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import thut.essentials.ThutEssentials;

public class WorldManager
{
    public static final Gson gson;
    static
    {
        gson = new GsonBuilder().registerTypeAdapter(QName.class, new TypeAdapter<QName>()
        {
            @Override
            public void write(JsonWriter out, QName value) throws IOException
            {
                out.value(value.toString());
            }

            @Override
            public QName read(JsonReader in) throws IOException
            {
                return new QName(in.nextString());
            }
        }).setPrettyPrinting().create();
    }

    public static String     DEFAULTPATH = "./config";
    public static CustomDims dims;

    public static class CustomDims
    {
        public List<CustomDim> dims = Lists.newArrayList();
    }

    public static class CustomDim
    {
        public int    dimid;
        public String world_name;
        public String dim_type;
        public String world_type;
        public String generator_options;
        public Long   seed;

        @Override
        public String toString()
        {
            return dimid + " " + world_name + " " + dim_type + " " + world_type + " " + generator_options + " " + seed;
        }
    }

    public static void loadCustomDims(String dimFile)
    {
        File file = new File(DEFAULTPATH, dimFile);
        if (!file.exists())
        {
            ThutEssentials.logger.log(Level.FINER, "No Custom Dimensions file found: " + file
                    + " If you make one, it will allow specifying custom dimensions and worldgen.");
            return;
        }

        try
        {
            FileInputStream stream = new FileInputStream(file);
            InputStreamReader reader = new InputStreamReader(stream);
            dims = gson.fromJson(reader, CustomDims.class);
            ThutEssentials.logger.log(Level.FINER, "Loaded Dims: " + dims.dims);
        }
        catch (Exception e)
        {
            ThutEssentials.logger.log(Level.WARNING, "Error loading custom Dims from: " + file, e);
        }

    }

    public static void onServerStart(FMLServerStartingEvent event) throws IOException
    {
        loadCustomDims("thutessentials_dimensions.json");
        if (dims != null)
        {
            ThutEssentials.logger.log(Level.FINER, "Starting server, Thut Essentials Registering Dimensions");
            for (CustomDim dim : dims.dims)
            {
                DimensionType type = DimensionType.OVERWORLD;
                if (dim.dim_type != null)
                {
                    try
                    {
                        type = DimensionType.byName(dim.dim_type);
                    }
                    catch (Exception e)
                    {
                        ThutEssentials.logger.log(Level.WARNING, "Error with dim_type: " + dim.dim_type, e);
                        type = DimensionType.OVERWORLD;
                    }
                }
                initDimension(dim.dimid, dim.world_name, dim.world_type, dim.generator_options, type, dim.seed);
            }
        }
    }

    public static WorldServer initDimension(int dim, String worldName, String worldType, String generatorOptions,
            Long seed)
    {
        return initDimension(dim, worldName, worldType, generatorOptions, DimensionType.OVERWORLD, seed);
    }

    public static WorldServer initDimension(int dim, String worldName, String worldType, String generatorOptions,
            DimensionType dimType, Long seed)
    {
        World overworld = DimensionManager.getWorld(0);
        if (dimType == null)
        {
            dimType = DimensionType.OVERWORLD;
            ThutEssentials.logger.log(Level.WARNING, "Dimtype should not be null!");
        }
        if (!DimensionManager.isDimensionRegistered(dim))
        {
            DimensionManager.registerDimension(dim, dimType);
        }
        else
        {
            ThutEssentials.logger.log(Level.FINER, DimensionManager.getProviderType(dim) + "");
        }
        World oldWorld = DimensionManager.getWorld(dim);
        if (generatorOptions != null && generatorOptions.isEmpty()) generatorOptions = null;
        WorldInfo old = overworld.getWorldInfo();
        WorldSettings settings = new WorldSettings(seed == null ? overworld.getSeed() : seed, old.getGameType(),
                old.isMapFeaturesEnabled(), old.isHardcoreModeEnabled(), WorldType.parseWorldType(worldType));
        settings.setGeneratorOptions(generatorOptions);
        WorldServer newWorld = WorldManager.initDimension(dim, settings, worldName);
        DimensionManager.setWorld(dim, newWorld, FMLCommonHandler.instance().getMinecraftServerInstance());
        if (oldWorld != null && newWorld != null)
        {
            ThutEssentials.logger.log(Level.FINER, "Replaced " + oldWorld + " with " + newWorld);
        }
        else if (newWorld != null)
        {
            ThutEssentials.logger.log(Level.FINER, "Set World " + newWorld);
        }
        else
        {
            ThutEssentials.logger.log(Level.WARNING,
                    "Unable to create world " + dim + " " + worldName + " " + worldType + " " + generatorOptions);
        }
        return newWorld;
    }

    public static WorldServer initDimension(int dim, WorldSettings settings, String name)
    {
        DimensionManager.initDimension(dim);
        WorldServer overworld = DimensionManager.getWorld(0);
        WorldInfo info = new WorldInfo(settings, name);
        WorldServerMulti newWorld = new WorldServerMulti(overworld.getMinecraftServer(), overworld.getSaveHandler(),
                info, dim, overworld.profiler);
        return newWorld;
    }

}
