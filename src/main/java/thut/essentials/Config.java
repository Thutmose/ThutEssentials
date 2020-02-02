package thut.essentials;

import java.util.List;

import com.google.common.collect.Lists;

import net.minecraft.world.dimension.DimensionType;
import thut.essentials.config.Config.ConfigData;

public class Config extends ConfigData
{

    public boolean       defaultMessages     = true;
    public boolean       denyExplosions      = true;
    public boolean       chunkLoading        = true;
    public boolean       landEnabled         = true;
    public boolean       shopsEnabled        = true;
    public boolean       log_interactions    = true;
    public List<String>  itemUseWhitelist    = Lists.newArrayList();
    public List<String>  blockUseWhitelist   = Lists.newArrayList();
    public List<String>  blockBreakWhitelist = Lists.newArrayList();
    public List<String>  blockPlaceWhitelist = Lists.newArrayList();
    public String        defaultTeamName     = "Plebs";
    public boolean       wildernessTeam      = false;
    public String        wildernessTeamName  = "Wilderness";
    public boolean       debug               = false;
    public List<String>  commandBlacklist    = Lists.newArrayList();
    public boolean       comandDisableSpam   = true;
    public List<String>  rules               = Lists.newArrayList();
    public boolean       logTeamChat         = true;
    public int           homeActivateDelay   = 50;
    public int           homeReUseDelay      = 100;
    public boolean       log_teleports       = true;
    public DimensionType spawnDimension      = DimensionType.OVERWORLD;
    public int           spawnActivateDelay  = 50;
    public long          spawnReUseDelay     = 100;
    public int           backRangeCheck      = 5;
    public int           backReUseDelay      = 100;
    public int           backActivateDelay   = 50;
    public int           tpaActivateDelay    = 50;
    public int           teamLandPerPlayer   = 125;

    public Config()
    {
        super(Reference.MODID);
    }

    @Override
    public void onUpdated()
    {
        // TODO Auto-generated method stub

    }

}
