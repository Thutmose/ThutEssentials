package thut.essentials;

import java.util.List;

import thut.essentials.config.Config.ConfigData;

public class Config extends ConfigData
{

    public boolean      defaultMessages;
    public boolean      denyExplosions;
    public boolean      chunkLoading;
    public boolean      landEnabled;
    public boolean      shopsEnabled;
    public boolean      log_interactions;
    public List<String> itemUseWhitelist;
    public List<String> blockUseWhitelist;
    public List<String> blockBreakWhitelist;
    public List<String> blockPlaceWhitelist;
    public String       defaultTeamName;
    public boolean      wildernessTeam;
    public String       wildernessTeamName;
    public boolean      debug;
    public List<String> commandBlacklist;
    public boolean      comandDisableSpam;
    public List<String> rules;

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
