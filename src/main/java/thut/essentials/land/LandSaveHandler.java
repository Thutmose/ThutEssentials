package thut.essentials.land;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import com.google.common.collect.Lists;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.FolderName;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import thut.essentials.Essentials;
import thut.essentials.land.LandManager.Coordinate;
import thut.essentials.land.LandManager.KGobalPos;
import thut.essentials.land.LandManager.LandTeam;

public class LandSaveHandler
{
    public static Gson SAVE_GSON = null;
    public static Gson LOAD_GSON = null;

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

    public static void removeEmptyTeams()
    {
        final Set<String> toRemove = new HashSet<>();
        final Map<String, LandTeam> teamMap = LandManager.getInstance()._teamMap;
        for (final String s : teamMap.keySet())
        {
            final LandTeam team = teamMap.get(s);
            if (team.member.size() == 0 && !team.reserved && team != LandManager.getDefaultTeam()) toRemove.add(s);
        }
        for (final String s : toRemove)
            LandManager.getInstance().removeTeam(s);
    }

    public static File getGlobalFolder()
    {
        final MinecraftServer server = LogicalSidedProvider.INSTANCE.get(LogicalSide.SERVER);
        final Path path = server.getWorldPath(new FolderName("land"));
        final File file = path.toFile();
        if (!file.exists()) file.mkdirs();
        return file;
    }

    public static File getTeamFolder()
    {
        final File teamFolder = new File(LandSaveHandler.getGlobalFolder(), "teams");
        if (!teamFolder.exists()) teamFolder.mkdirs();
        return teamFolder;
    }

    public static void saveGlobalData()
    {
        if (LandSaveHandler.SAVE_GSON == null) LandSaveHandler.SAVE_GSON = new GsonBuilder()
                .addSerializationExclusionStrategy(LandSaveHandler.exclusion).setPrettyPrinting().create();
        LandManager.getInstance().version = LandManager.VERSION;
        final String json = LandSaveHandler.SAVE_GSON.toJson(LandManager.getInstance());
        final File teamsFile = new File(LandSaveHandler.getGlobalFolder(), "landData.json");
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
        if (LandSaveHandler.LOAD_GSON == null) LandSaveHandler.LOAD_GSON = new GsonBuilder()
                .addDeserializationExclusionStrategy(LandSaveHandler.exclusion).setPrettyPrinting().create();
        final File teamsFile = new File(LandSaveHandler.getGlobalFolder(), "landData.json");
        if (Essentials.config.debug) Essentials.LOGGER.info("Starting Loading Land");
        if (teamsFile.exists())
        {
            try
            {
                final String json = FileUtils.readFileToString(teamsFile, "UTF-8");
                LandManager.instance = LandSaveHandler.LOAD_GSON.fromJson(json, LandManager.class);
            }
            catch (final Exception e)
            {
                e.printStackTrace();
            }
            if (LandManager.instance == null) LandManager.instance = new LandManager();
            LandSaveHandler.loadTeams();
        }
        else
        {
            if (LandManager.instance == null) LandManager.instance = new LandManager();
            LandSaveHandler.saveGlobalData();
        }
        if (Essentials.config.debug) Essentials.LOGGER.info("Finished Loading Land and Teams");
        // Set default as reservred to prevent it from getting cleaned up.
        LandManager.getDefaultTeam().reserved = true;
    }

    private static void loadTeams()
    {
        if (LandSaveHandler.LOAD_GSON == null) LandSaveHandler.LOAD_GSON = new GsonBuilder()
                .addDeserializationExclusionStrategy(LandSaveHandler.exclusion).setPrettyPrinting().create();
        final File folder = LandSaveHandler.getTeamFolder();
        if (Essentials.config.debug) Essentials.LOGGER.info("Starting Loading Teams");
        final MinecraftServer server = LogicalSidedProvider.INSTANCE.get(LogicalSide.SERVER);
        for (final File file : folder.listFiles())
            try
            {
                final String json = FileUtils.readFileToString(file, "UTF-8");
                final LandTeam team = LandSaveHandler.LOAD_GSON.fromJson(json, LandTeam.class);
                LandManager.getInstance()._teamMap.put(team.teamName, team);
                team.init(server);

                // Here we convert over the old land to the new format.
                final List<KGobalPos> toAdd = Lists.newArrayList(team.land.claims);
                final List<Coordinate> oldList = Lists.newArrayList(team.land.land);
                for (final Coordinate old : oldList)
                {
                    final BlockPos b = new BlockPos(old.x, old.y, old.z);
                    final RegistryKey<World> dim = Coordinate.fromOld(old.dim);
                    if (dim != null)
                    {
                        toAdd.add(KGobalPos.getPosition(dim, b));
                        team.land.land.remove(old);
                    }
                    else Essentials.LOGGER.debug("Did not find claim! " + old.dim);
                }
                if (Essentials.config.debug) Essentials.LOGGER.info("Processing " + team.teamName);
                for (final KGobalPos land : toAdd)
                    LandManager.getInstance().addTeamLand(team.teamName, land, false);
            }
            catch (final Exception e)
            {
                e.printStackTrace();
            }
        if (Essentials.config.debug) Essentials.LOGGER.info("Cleaning Up Teams");
        // Remove any teams that were loaded with no members, and not reserved.
        LandSaveHandler.removeEmptyTeams();
    }

    public static void saveTeam(final String team)
    {
        if (LandSaveHandler.SAVE_GSON == null) LandSaveHandler.SAVE_GSON = new GsonBuilder()
                .addSerializationExclusionStrategy(LandSaveHandler.exclusion).setPrettyPrinting().create();
        final File folder = LandSaveHandler.getTeamFolder();
        final File teamFile = new File(folder, team + ".json");
        LandTeam land;
        if ((land = LandManager.getInstance().getTeam(team, false)) != null)
        {
            if (land == LandManager.getDefaultTeam()) land.member.clear();
            final String json = LandSaveHandler.SAVE_GSON.toJson(land);
            try
            {
                FileUtils.writeStringToFile(teamFile, json, "UTF-8");
            }
            catch (final IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    public static void deleteTeam(final String team)
    {
        final File folder = LandSaveHandler.getTeamFolder();
        final File teamFile = new File(folder, team + ".json");
        if (teamFile.exists()) teamFile.delete();
    }

}
