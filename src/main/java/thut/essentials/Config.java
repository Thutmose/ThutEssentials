package thut.essentials;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.command.CommandSource;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.fml.loading.FMLPaths;
import thut.essentials.config.Config.ConfigData;
import thut.essentials.config.Configure;
import thut.essentials.land.LandEventsHandler;
import thut.essentials.util.ChatManager;
import thut.essentials.util.HomeManager;
import thut.essentials.util.InventoryLogger;
import thut.essentials.util.KitManager;
import thut.essentials.util.MobManager;
import thut.essentials.util.PlayerMover;
import thut.essentials.util.PvPManager;
import thut.essentials.util.WarpManager;

public class Config extends ConfigData
{
    public static final String LAND  = "land";
    public static final String MISC  = "misc";
    public static final String HOME  = "homes";
    public static final String WARP  = "warps";
    public static final String KITS  = "kits";
    public static final String BACK  = "back";
    public static final String BED   = "bed";
    public static final String ECON  = "economy";
    public static final String STAFF = "staff";
    public static final String LOGS  = "logging";
    public static final String CHAT  = "chat";
    public static final String RTP   = "rtp";
    public static final String TPA   = "tpa";
    public static final String MOBS  = "mobs";

    @Configure(category = Config.LAND)
    public boolean defaultMessages    = true;
    @Configure(category = Config.LAND)
    public boolean denyExplosions     = true;
    @Configure(category = Config.LAND)
    public boolean chunkLoading       = true;
    @Configure(category = Config.LAND)
    public boolean landEnabled        = true;
    @Configure(category = Config.LAND)
    public String  defaultTeamName    = "Plebs";
    @Configure(category = Config.LAND)
    public boolean wildernessTeam     = false;
    @Configure(category = Config.LAND)
    public String  wildernessTeamName = "Wilderness";
    @Configure(category = Config.LAND)
    public boolean logTeamChat        = true;
    @Configure(category = Config.LAND)
    public int     teamLandPerPlayer  = 125;
    @Configure(category = Config.LAND)
    public int     prefixLength       = 12;
    @Configure(category = Config.LAND)
    public int     maxChunkloads      = 9;
    @Configure(category = Config.LAND)
    public boolean noMobGriefing      = true;

    @Configure(category = Config.LAND)
    public List<String> itemUseWhitelist    = Lists.newArrayList();
    @Configure(category = Config.LAND)
    public List<String> blockUseWhitelist   = Lists.newArrayList();
    @Configure(category = Config.LAND)
    public List<String> mobUseWhitelist     = Lists.newArrayList();
    @Configure(category = Config.LAND)
    public List<String> blockBreakWhitelist = Lists.newArrayList();
    @Configure(category = Config.LAND)
    public List<String> blockPlaceWhitelist = Lists.newArrayList();
    @Configure(category = Config.LAND)
    public boolean      foodWhitelisted     = true;

    @Configure(category = Config.HOME)
    public int maxHomes          = 2;
    @Configure(category = Config.HOME)
    public int homeActivateDelay = 50;
    @Configure(category = Config.HOME)
    public int homeReUseDelay    = 100;

    @Configure(category = Config.HOME)
    public int kitReuseDelay = -1;

    @Configure(category = Config.WARP)
    public List<String> warps             = Lists.newArrayList();
    @Configure(category = Config.WARP)
    public int          warpActivateDelay = 50;
    @Configure(category = Config.WARP)
    public int          warpReUseDelay    = 100;

    @Configure(category = Config.BACK)
    public int     backRangeCheck    = 5;
    @Configure(category = Config.BACK)
    public int     backReUseDelay    = 100;
    @Configure(category = Config.BACK)
    public int     backActivateDelay = 50;
    @Configure(category = Config.BACK)
    public boolean back_on_death     = true;
    @Configure(category = Config.BACK)
    public boolean back_on_tp        = true;

    @Configure(category = Config.BED)
    public int bedReUseDelay    = 100;
    @Configure(category = Config.BED)
    public int bedActivateDelay = 50;

    @Configure(category = Config.RTP)
    public int rtpReUseDelay    = 100;
    @Configure(category = Config.RTP)
    public int rtpActivateDelay = 50;

    @Configure(category = Config.RTP)
    public String rtpCentre = "0,0";

    @Configure(category = Config.RTP)
    public int     rtpDistance      = 5000;
    @Configure(category = Config.RTP)
    public boolean rtpSpawnCentred  = true;
    @Configure(category = Config.RTP)
    public boolean rtpPlayerCentred = false;

    @Configure(category = Config.ECON)
    public boolean      shopsEnabled = true;
    @Configure(category = Config.ECON)
    public List<String> sellTags     = Lists.newArrayList("Sell", "Sells", "Sale");
    @Configure(category = Config.ECON)
    public List<String> recycleTags  = Lists.newArrayList("Recycle");

    @Configure(category = Config.CHAT)
    public boolean useChatFormat = true;
    @Configure(category = Config.CHAT)
    public String  chatFormat    = "<%s> %s";

    @Configure(category = Config.LOGS)
    public List<String> inventory_log_blacklist = Lists.newArrayList();
    @Configure(category = Config.LOGS)
    public boolean      log_interactions        = true;
    @Configure(category = Config.LOGS)
    public boolean      log_teleports           = true;
    @Configure(category = Config.LOGS)
    public boolean      log_inventory_use       = true;

    @Configure(category = Config.TPA)
    public int tpaActivateDelay = 50;
    @Configure(category = Config.TPA)
    public int tpaReUseDelay    = 100;

    @Configure(category = Config.TPA)
    public boolean tpaCrossDim = true;

    @Configure(category = Config.MISC)
    public List<String> commandBlacklist = Lists.newArrayList();
    @Configure(category = Config.MISC)
    public List<String> rules            = Lists.newArrayList();
    @Configure(category = Config.MISC)
    public List<String> invulnMobs       = Lists.newArrayList();

    @Configure(category = Config.MISC)
    public boolean debug             = false;
    @Configure(category = Config.MISC)
    public boolean defuzz            = true;
    @Configure(category = Config.MISC)
    public boolean comandDisableSpam = true;

    @Configure(category = Config.MISC)
    public boolean pvpPerms = false;

    @Configure(category = Config.MISC)
    public int  spawnDim           = 0;
    @Configure(category = Config.MISC)
    public int  spawnActivateDelay = 50;
    @Configure(category = Config.MISC)
    public long spawnReUseDelay    = 100;

    @Configure(category = Config.MISC)
    public double maxSpeed = 10;

    @Configure(category = Config.STAFF)
    public List<String> staff = Lists.newArrayList();

    @Configure(category = Config.MISC)
    public String lang_file = "en_us.json";

    @Configure(category = Config.MOBS)
    public List<String> mobSpawnBlacklist = Lists.newArrayList();
    @Configure(category = Config.MOBS)
    public List<String> mobSpawnWhitelist = Lists.newArrayList();

    @Configure(category = Config.MOBS)
    public boolean mobSpawnUsesWhitelist = false;

    @Configure(category = Config.MOBS)
    public List<String> mobGriefAllowBlacklist = Lists.newArrayList();
    @Configure(category = Config.MOBS)
    public List<String> mobGriefAllowWhitelist = Lists.newArrayList(EntityType.VILLAGER.getRegistryName().toString());

    @Configure(category = Config.MOBS)
    public boolean mobGriefAllowUsesWhitelist = true;

    public DimensionType spawnDimension = DimensionType.OVERWORLD;

    private final Path configpath;

    public Config()
    {
        super(Essentials.MODID);
        this.configpath = FMLPaths.CONFIGDIR.get().resolve(Essentials.MODID);
    }

    private final Map<String, String> lang_overrides_map = Maps.newHashMap();

    public ITextComponent getMessage(final String key, final Object... args)
    {
        if (this.lang_overrides_map.containsKey(key)) return new StringTextComponent(String.format(
                this.lang_overrides_map.get(key), args));
        else return new TranslationTextComponent(key, args);
    }

    public void sendFeedback(final CommandSource target, final String key, final boolean log, final Object... args)
    {
        target.sendFeedback(this.getMessage(key, args), log);
    }

    public void sendError(final CommandSource target, final String key, final Object... args)
    {
        target.sendErrorMessage(this.getMessage(key, args));
    }

    @Override
    public void onUpdated()
    {
        this.spawnDimension = DimensionType.getById(this.spawnDim);

        final File file = this.configpath.resolve(this.lang_file).toFile();
        if (file.exists()) try
        {
            final BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
            final Gson gson = new GsonBuilder().create();
            final JsonObject o = gson.fromJson(in, JsonObject.class);
            for (final Entry<String, JsonElement> entry : o.entrySet())
                try
                {
                    final String key = entry.getKey();
                    final String value = entry.getValue().getAsString();
                    this.lang_overrides_map.put(key, value);
                }
                catch (final Exception e)
                {
                    Essentials.LOGGER.error("Error with keypair {}, {}", entry.getKey(), entry.getValue());
                }
        }
        catch (final Exception e)
        {
            Essentials.LOGGER.error("Error loading lang json from config!", e);
        }

        if (this.log_inventory_use) InventoryLogger.enable();
        else InventoryLogger.disable();

        if (this.rtpSpawnCentred) thut.essentials.commands.misc.RTP.centre = null;
        else try
        {
            final String[] args = this.rtpCentre.split(",");
            final int x = Integer.parseInt(args[0]);
            final int z = Integer.parseInt(args[1]);
            thut.essentials.commands.misc.RTP.centre = new BlockPos(x, 0, z);
        }
        catch (final Exception e)
        {
            Essentials.LOGGER.error("Error with value in rtpCentre, defaulting to spawn centred!");
            thut.essentials.commands.misc.RTP.centre = null;
        }

        HomeManager.registerPerms();
        WarpManager.init();
        KitManager.init();
        ChatManager.init();
        MobManager.init();
        PlayerMover.INTERUPTED = this.getMessage("thutessentials.tp.standstill");
        LandEventsHandler.init();
        PvPManager.init();
    }

}
