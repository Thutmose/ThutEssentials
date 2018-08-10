package thut.essentials.land;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import net.minecraft.command.CommandException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import thut.essentials.util.ConfigManager;
import thut.essentials.util.Coordinate;

public class LandManager
{

    public static class Invites
    {
        public Set<String> teams = Sets.newHashSet();
    }

    public static class LandTeam
    {
        public static final String EDITMESSAGES   = "editMessages";
        public static final String CLAIMPERM      = "claim";
        public static final String UNCLAIMPERM    = "unclaim";
        public static final String SETPREFIX      = "prefix";
        public static final String SETHOME        = "sethome";
        public static final String INVITE         = "invite";
        public static final String KICK           = "kick";

        public TeamLand            land           = new TeamLand();
        public String              teamName;
        public Set<UUID>           admin          = Sets.newHashSet();
        public Set<UUID>           member         = Sets.newHashSet();
        public Map<UUID, Rank>     _ranksMembers  = Maps.newHashMap();
        public Map<String, Rank>   rankMap        = Maps.newHashMap();
        public Set<Coordinate>     anyUse         = Sets.newHashSet();
        public Coordinate          home;
        public String              exitMessage    = "";
        public String              enterMessage   = "";
        public String              denyMessage    = "";
        public String              prefix         = "";
        public boolean             reserved       = false;
        public boolean             players        = false;
        public boolean             noPlayerDamage = false;
        public boolean             noMobSpawn     = false;
        public boolean             friendlyFire   = true;
        public boolean             noExplosions   = false;
        public boolean             allPublic      = false;

        // TODO figure out what I want to do with these two.
        public List<String>        allies         = Lists.newArrayList();
        public List<String>        enemies        = Lists.newArrayList();

        public LandTeam()
        {
        }

        public LandTeam(String name)
        {
            teamName = name;
        }

        public boolean isMember(UUID id)
        {
            return member.contains(id);
        }

        public boolean isMember(Entity player)
        {
            return isMember(player.getUniqueID());
        }

        public boolean isAdmin(UUID id)
        {
            return admin.contains(id);
        }

        public boolean isAdmin(Entity player)
        {
            return isAdmin(player.getUniqueID());
        }

        public boolean hasPerm(UUID player, String perm)
        {
            if (admin.contains(player)) return true;
            Rank rank = _ranksMembers.get(player);
            if (rank == null) return false;
            return rank.perms.contains(perm);
        }

        public void setPerm(String rankName, String perm)
        {
            Rank rank = rankMap.get(rankName);
            if (rank != null) rank.perms.add(perm);
        }

        public void unsetPerm(String rankName, String perm)
        {
            Rank rank = rankMap.get(rankName);
            if (rank != null) rank.perms.remove(perm);
        }

        public void init(MinecraftServer server)
        {
            Set<UUID> members = Sets.newHashSet(member);
            if (!teamName.equals(ConfigManager.INSTANCE.defaultTeamName))
            {
                for (UUID id : members)
                    LandManager.getInstance()._playerTeams.put(id, this);
                for (Coordinate c : anyUse)
                    LandManager.getInstance()._publicBlocks.put(c, this);
            }
        }

        @Override
        public boolean equals(Object o)
        {
            if (o instanceof LandTeam) { return ((LandTeam) o).teamName.equals(teamName); }
            return false;
        }

        @Override
        public int hashCode()
        {
            return teamName.hashCode();
        }

        public static class Rank
        {
            public Set<UUID>   members = Sets.newHashSet();
            public String      prefix;
            public Set<String> perms   = Sets.newHashSet();
        }
    }

    public static class TeamLand
    {
        public HashSet<Coordinate> land = Sets.newHashSet();

        public boolean addLand(Coordinate land)
        {
            return this.land.add(land);
        }

        public int countLand()
        {
            return land.size();
        }

        public boolean removeLand(Coordinate land)
        {
            return this.land.remove(land);
        }
    }

    static LandManager      instance;

    public static final int VERSION = 1;

    public static void clearInstance()
    {
        if (instance != null)
        {
            LandSaveHandler.saveGlobalData();
            for (String s : instance._teamMap.keySet())
                LandSaveHandler.saveTeam(s);
        }
        instance = null;
    }

    public static LandManager getInstance()
    {
        if (instance == null)
        {
            LandSaveHandler.loadGlobalData();
        }
        return instance;
    }

    public static LandTeam getTeam(UUID id)
    {
        LandTeam playerTeam = getInstance()._playerTeams.get(id);
        if (playerTeam == null)
        {
            for (LandTeam team : getInstance()._teamMap.values())
            {
                if (team.isMember(id))
                {
                    getInstance().addToTeam(id, team.teamName);
                    playerTeam = team;
                    break;
                }
            }
            if (playerTeam == null)
            {
                getInstance().addToTeam(id, ConfigManager.INSTANCE.defaultTeamName);
                playerTeam = getInstance().getTeam(ConfigManager.INSTANCE.defaultTeamName, false);
            }
        }
        return playerTeam;
    }

    public static LandTeam getTeam(Entity player)
    {
        return getTeam(player.getUniqueID());
    }

    public static LandTeam getDefaultTeam()
    {
        return getInstance().getTeam(ConfigManager.INSTANCE.defaultTeamName, true);
    }

    public static boolean owns(Entity player, Coordinate chunk)
    {
        return getTeam(player).equals(getInstance().getLandOwner(chunk));
    }

    public HashMap<String, LandTeam>        _teamMap      = Maps.newHashMap();
    protected HashMap<Coordinate, LandTeam> _landMap      = Maps.newHashMap();
    protected HashMap<UUID, LandTeam>       _playerTeams  = Maps.newHashMap();
    protected HashMap<UUID, Invites>        invites       = Maps.newHashMap();
    protected HashMap<Coordinate, LandTeam> _publicBlocks = Maps.newHashMap();
    public int                              version       = VERSION;

    LandManager()
    {
    }

    public void renameTeam(String oldName, String newName) throws CommandException
    {
        if (_teamMap.containsKey(newName)) throw new CommandException("Error, new team name already in use");
        LandTeam team = _teamMap.remove(oldName);
        if (team == null) throw new CommandException("Error, specified team not found");
        _teamMap.put(newName, team);
        for (Invites i : invites.values())
        {
            if (i.teams.remove(oldName))
            {
                i.teams.add(newName);
            }
        }
        team.teamName = newName;
        LandSaveHandler.saveTeam(newName);
        LandSaveHandler.deleteTeam(oldName);
    }

    public void removeTeam(String teamName)
    {
        LandTeam team = _teamMap.remove(teamName);
        HashSet<Coordinate> land = Sets.newHashSet(_landMap.keySet());
        for (Coordinate c : land)
        {
            if (_landMap.get(c).equals(team))
            {
                _landMap.remove(c);
            }
        }
        HashSet<UUID> ids = Sets.newHashSet(_playerTeams.keySet());
        for (UUID id : ids)
        {
            if (_playerTeams.get(id).equals(team))
            {
                _playerTeams.remove(id);
            }
        }
        for (Invites i : invites.values())
        {
            i.teams.remove(teamName);
        }
        LandSaveHandler.deleteTeam(teamName);
    }

    public void addTeamLand(String team, Coordinate land, boolean sync)
    {
        LandTeam t = _teamMap.get(team);
        if (t == null)
        {
            Thread.dumpStack();
            return;
        }
        t.land.addLand(land);
        _landMap.put(land, t);
        for (LandTeam t1 : _teamMap.values())
        {
            if (t != t1) t1.land.removeLand(land);
        }
        if (sync)
        {
            LandSaveHandler.saveTeam(team);
        }
    }

    public void addAdmin(UUID admin, String team)
    {
        LandTeam t = getTeam(team, true);
        t.admin.add(admin);
        LandSaveHandler.saveTeam(team);
    }

    public void addToTeam(UUID member, String team)
    {
        LandTeam t = getTeam(team, true);
        if (t.admin.isEmpty() && !t.teamName.equals(ConfigManager.INSTANCE.defaultTeamName))
        {
            t.admin.add(member);
        }
        if (_playerTeams.containsKey(member))
        {
            LandTeam old = _playerTeams.remove(member);
            old.member.remove(member);
            old.admin.remove(member);
            LandSaveHandler.saveTeam(old.teamName);
        }
        t.member.add(member);
        _playerTeams.put(member, t);
        Invites invite = invites.get(member);
        if (invite != null)
        {
            invite.teams.remove(team);
        }
        LandSaveHandler.saveTeam(team);
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        try
        {
            EntityPlayer player = server.getPlayerList().getPlayerByUUID(member);
            if (player != null)
            {
                player.refreshDisplayName();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public int countLand(String team)
    {
        LandTeam t = _teamMap.get(team);
        if (t != null) { return t.land.countLand(); }
        return 0;
    }

    public void createTeam(UUID member, String team) throws CommandException
    {
        if (_teamMap.containsKey(team)) throw new CommandException(team + " already exists!");
        getTeam(team, true);
        addToTeam(member, team);
        addAdmin(member, team);
    }

    public List<String> getInvites(UUID member)
    {
        List<String> ret = new ArrayList<String>();
        Invites invite = invites.get(member);
        if (invite == null) return ret;
        return Lists.newArrayList(invite.teams);
    }

    public LandTeam getLandOwner(Coordinate land)
    {
        return _landMap.get(land);
    }

    public LandTeam getTeam(String name, boolean create)
    {
        LandTeam team = _teamMap.get(name);
        if (team == null && create)
        {
            team = new LandTeam(name);
            _teamMap.put(name, team);
        }
        return team;
    }

    public List<Coordinate> getTeamLand(String team)
    {
        ArrayList<Coordinate> ret = new ArrayList<Coordinate>();
        LandTeam t = _teamMap.get(team);
        if (t != null) ret.addAll(t.land.land);
        return ret;
    }

    public boolean hasInvite(UUID member, String team)
    {
        Invites invite = invites.get(member);
        if (invite != null) return invite.teams.contains(team);
        return false;
    }

    public boolean invite(UUID inviter, UUID invitee)
    {
        if (!isAdmin(inviter)) return false;
        String team = _playerTeams.get(inviter).teamName;
        if (hasInvite(invitee, team)) return false;
        Invites invite = invites.get(invitee);
        if (invite == null)
        {
            invite = new Invites();
            invites.put(invitee, invite);
        }
        invite.teams.add(team);
        return true;
    }

    public boolean isAdmin(UUID member)
    {
        LandTeam team = _playerTeams.get(member);
        if (team == null) return false;
        return team.admin.contains(member);
    }

    public boolean isOwned(Coordinate land)
    {
        return _landMap.containsKey(land);
    }

    public boolean isPublic(Coordinate c, LandTeam team)
    {
        return team.allPublic || _publicBlocks.containsKey(c);
    }

    public boolean isTeamLand(Coordinate chunk, String team)
    {
        LandTeam t = _teamMap.get(team);
        if (t != null) return t.land.land.contains(chunk);
        return false;
    }

    public void removeAdmin(UUID member)
    {
        LandTeam t = _playerTeams.get(member);
        if (t != null)
        {
            t.admin.remove(member);
        }
    }

    public void removeFromInvites(UUID member, String team)
    {
        Invites invite = invites.get(member);
        if (invite != null && invite.teams.contains(team))
        {
            invite.teams.remove(team);
            LandSaveHandler.saveGlobalData();
        }
    }

    public void removeFromTeam(UUID member)
    {
        addToTeam(member, getDefaultTeam().teamName);
    }

    public void removeTeamLand(String team, Coordinate land)
    {
        LandTeam t = _teamMap.get(team);
        _landMap.remove(land);
        if (t != null && t.land.removeLand(land))
        {
            LandSaveHandler.saveTeam(team);
        }
    }

    public void setPublic(Coordinate c, LandTeam owner)
    {
        _publicBlocks.put(c, owner);
        owner.anyUse.add(c);
        LandSaveHandler.saveGlobalData();
    }

    public void unsetPublic(Coordinate c)
    {
        if (!_publicBlocks.containsKey(c)) return;
        LandTeam team;
        (team = _publicBlocks.remove(c)).anyUse.remove(c);
        LandSaveHandler.saveTeam(team.teamName);
        LandSaveHandler.saveGlobalData();
    }
}
