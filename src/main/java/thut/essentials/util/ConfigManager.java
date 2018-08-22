package thut.essentials.util;

import java.io.File;

import net.minecraftforge.common.MinecraftForge;
import thut.essentials.ThutEssentials;
import thut.essentials.economy.EconomyManager;
import thut.essentials.itemcontrol.ItemControl;
import thut.essentials.land.LandEventsHandler;
import thut.essentials.land.LandManager;

public class ConfigManager extends ConfigBase
{
    private static final String SPAWN                     = "spawn";
    private static final String RULES                     = "rules";
    private static final String WARPS                     = "warps";
    private static final String STAFF                     = "staff";
    private static final String NAMES                     = "names";
    private static final String COMMANDS                  = "commands";
    private static final String LAND                      = "land";
    private static final String ITEM                      = "itemcontrol";
    private static final String MISC                      = "misc";
    private static final String ECON                      = "economy";

    public static ConfigManager INSTANCE;

    @Configure(category = SPAWN)
    public int                  spawnDimension            = 0;
    @Configure(category = SPAWN)
    public boolean              spawnDefuzz               = false;

    @Configure(category = RULES)
    public String[]             rules                     = {};
    @Configure(category = RULES)
    public String               ruleHeader                = "List of Rules:";

    @Configure(category = WARPS)
    public String[]             warps                     = {};
    @Configure(category = WARPS)
    public int                  backReUseDelay            = 10;
    @Configure(category = WARPS)
    public int                  backRangeCheck            = 3;
    @Configure(category = WARPS)
    public int                  warpReUseDelay            = 10;
    @Configure(category = WARPS)
    public int                  spawnReUseDelay           = 10;
    @Configure(category = WARPS)
    public int                  homeReUseDelay            = 10;
    @Configure(category = WARPS)
    public int                  backActivateDelay         = 10;
    @Configure(category = WARPS)
    public int                  warpActivateDelay         = 10;
    @Configure(category = WARPS)
    public int                  spawnActivateDelay        = 10;
    @Configure(category = WARPS)
    public int                  homeActivateDelay         = 10;
    @Configure(category = WARPS)
    public int                  rtpActivateDelay          = 10;
    @Configure(category = WARPS)
    public int                  tpaActivateDelay          = 10;

    @Configure(category = STAFF)
    public String[]             staff                     = {};

    @Configure(category = ITEM)
    public String[]             itemBlacklist             = {};
    @Configure(category = ITEM)
    public boolean              itemControlEnabled        = false;
    @Configure(category = ITEM)
    public double               blacklistDamage           = 5;
    @Configure(category = ITEM)
    public int                  kitReuseDelay             = -1;
    @Configure(category = ITEM)
    public boolean              itemLifeTweak             = false;
    @Configure(category = ITEM)
    public int                  itemLifeSpan              = 6000;

    @Configure(category = MISC)
    public double               speedCap                  = 10;

    @Configure(category = MISC)
    public int                  maxHomes                  = 2;
    @Configure(category = MISC)
    public int                  rtpdistance               = 1000;

    @Configure(category = MISC)
    public String[]             disabledCommands          = {};

    @Configure(category = MISC)
    public String[]             alternateCommands         = { "gamemode:gm" };

    @Configure(category = MISC)
    public String[]             commandPermissionLevels   = { "heal:2" };

    @Configure(category = MISC)
    public String               motd                      = "";

    @Configure(category = MISC)
    public boolean              comandDisableSpam         = false;

    @Configure(category = MISC)
    public boolean              chatTweaks                = true;

    @Configure(category = MISC)
    public boolean              forceChatFormat           = false;
    @Configure(category = MISC)
    public String               chatFormat                = "<[name]> [message]";

    @Configure(category = MISC)
    public String[]             allThutEssentialsCommands = {};

    @Configure(category = MISC)
    public boolean              pauseWhenEmpty            = false;

    @Configure(category = MISC)
    public int                  pauseTime                 = 5000;

    @Configure(category = MISC)
    public boolean              debug                     = false;

    @Configure(category = ECON)
    public boolean              economyEnabled            = true;
    @Configure(category = ECON)
    public int                  maxLandViaSalesPerTeam    = 512;

    @Configure(category = NAMES)
    public boolean              name                      = true;
    @Configure(category = NAMES)
    public boolean              suffix                    = true;
    @Configure(category = NAMES)
    public boolean              prefix                    = true;

    @Configure(category = LAND)
    public boolean              landEnabled               = false;
    @Configure(category = LAND)
    public boolean              denyExplosions            = false;
    @Configure(category = LAND)
    public int                  teamLandPerPlayer         = 125;
    @Configure(category = LAND)
    public int                  playerLand                = 1;
    @Configure(category = LAND)
    public String               defaultTeamName           = "Plebs";
    @Configure(category = LAND)
    public boolean              logTeamChat               = false;
    @Configure(category = LAND)
    public int                  prefixLength              = 10;
    @Configure(category = LAND)
    public String[]             itemUseWhitelist          = {};
    @Configure(category = LAND)
    public String[]             blockUseWhitelist         = {};
    @Configure(category = LAND)
    public String[]             blockBreakWhitelist       = {};

    @Configure(category = COMMANDS)
    public String[]             commands                  = {};

    public ConfigManager()
    {
        super(null);
    }

    public ConfigManager(File configFile)
    {
        super(configFile, new ConfigManager());
        MinecraftForge.EVENT_BUS.register(this);
        INSTANCE = this;
        populateSettings();
        applySettings();
        save();
    }

    @Override
    public void applySettings()
    {
        WarpManager.init();
        if (itemControlEnabled) ItemControl.init();
        if (landEnabled) LandEventsHandler.init();
        else LandManager.clearInstance();
        if (economyEnabled && ThutEssentials.instance.loaded) EconomyManager.getInstance();
        else EconomyManager.clearInstance();
        if (spawnDefuzz) MinecraftForge.EVENT_BUS.register(ThutEssentials.instance.defuz);
        else MinecraftForge.EVENT_BUS.unregister(ThutEssentials.instance.defuz);
        BaseCommand.permsMap.clear();
        for (String s : commandPermissionLevels)
        {
            try
            {
                String[] args = s.split(":");
                String key = args[0];
                int value = Integer.parseInt(args[1]);
                BaseCommand.permsMap.put(key, value);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        if (pauseWhenEmpty) MinecraftForge.EVENT_BUS.register(ServerPauser.class);
        else MinecraftForge.EVENT_BUS.unregister(ServerPauser.class);
    }

}
