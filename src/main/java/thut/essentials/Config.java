package thut.essentials;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import net.minecraft.command.CommandSource;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.dimension.DimensionType;
import thut.essentials.config.Config.ConfigData;
import thut.essentials.config.Configure;
import thut.essentials.land.LandEventsHandler;
import thut.essentials.util.HomeManager;
import thut.essentials.util.KitManager;
import thut.essentials.util.WarpManager;

public class Config extends ConfigData
{
    public static final String LAND = "land";
    public static final String MISC = "misc";
    public static final String HOME = "homes";
    public static final String WARP = "warps";
    public static final String KITS = "kits";
    public static final String BACK = "back";
    public static final String ECON = "economy";

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

    @Configure(category = Config.ECON)
    public boolean shopsEnabled = true;

    @Configure(category = Config.MISC)
    public boolean      log_interactions    = true;
    @Configure(category = Config.MISC)
    public List<String> itemUseWhitelist    = Lists.newArrayList();
    @Configure(category = Config.MISC)
    public List<String> blockUseWhitelist   = Lists.newArrayList();
    @Configure(category = Config.MISC)
    public List<String> blockBreakWhitelist = Lists.newArrayList();
    @Configure(category = Config.MISC)
    public List<String> blockPlaceWhitelist = Lists.newArrayList();
    @Configure(category = Config.MISC)
    public List<String> commandBlacklist    = Lists.newArrayList();
    @Configure(category = Config.MISC)
    public List<String> rules               = Lists.newArrayList();
    @Configure(category = Config.MISC)
    public boolean      debug               = false;
    @Configure(category = Config.MISC)
    public boolean      defuzz              = true;
    @Configure(category = Config.MISC)
    public boolean      comandDisableSpam   = true;
    @Configure(category = Config.MISC)
    public boolean      log_teleports       = true;
    @Configure(category = Config.MISC)
    public int          spawnDim            = 0;
    @Configure(category = Config.MISC)
    public int          spawnActivateDelay  = 50;
    @Configure(category = Config.MISC)
    public long         spawnReUseDelay     = 100;
    @Configure(category = Config.MISC)
    public int          tpaActivateDelay    = 50;

    @Configure(category = Config.MISC)
    public List<String> lang_overrides = Lists.newArrayList();

    public DimensionType spawnDimension = DimensionType.OVERWORLD;

    public Config()
    {
        super(Essentials.MODID);
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
        for (final String s : this.lang_overrides)
        {
            final String[] args = s.split(":");
            if (args.length < 2) Essentials.LOGGER.warn(
                    "Invalid lang override: {}, it must be of form \"<key>:<value>\"");
            else
            {
                String value = args[1];
                for (int i = 2; i < args.length; i++)
                    value = value + ":" + args[i];
                this.lang_overrides_map.put(args[0], value);
            }
        }
        HomeManager.registerPerms();
        WarpManager.init();
        KitManager.init();
        if (this.landEnabled) LandEventsHandler.init();
    }

}
