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
import com.mojang.authlib.GameProfile;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import thut.essentials.Essentials;
import thut.essentials.util.Coordinate;

public class LandManager
{

    /** Stores a list of invited to team names. */
    public static class Invites
    {
        public Set<String> teams = Sets.newHashSet();
    }

    /**
     * Stores a set of members, a set of permission strings, and an optional
     * prefix for the rank.
     */
    public static class PlayerRank
    {
        /** Who has this rank. */
        public Set<UUID>   members = Sets.newHashSet();
        /** Optional prefix for the rank. */
        public String      prefix;
        /** Perms for this rank. */
        public Set<String> perms   = Sets.newHashSet();
    }

    public static class Relation
    {
        /** Permissions for the relation */
        public Set<String> perms = Sets.newHashSet();
    }

    public static class LandTeam
    {
        // These are perms checked for ranks.
        /** Can edit enter/leave/deny messages. */
        public static final String EDITMESSAGES = "editMessages";
        /** Can claim. */
        public static final String CLAIMPERM    = "claim";
        /** can unclaim */
        public static final String UNCLAIMPERM  = "unclaim";
        /** Can change prefixes */
        public static final String SETPREFIX    = "prefix";
        /** Can set team home. */
        public static final String SETHOME      = "sethome";
        /** Can invite people */
        public static final String INVITE       = "invite";
        /** Can kick people */
        public static final String KICK         = "kick";
        /** Can chunkload. */
        public static final String LOADPERM     = "cload";
        /** Can chunkload. */
        public static final String UNLOADPERM   = "uncload";

        // These are perms checked for relations
        /** Can interact with things freely */
        public static final String PUBLIC = "public";
        /** Can place blocks */
        public static final String PLACE  = "place";
        /** Can break blocks. */
        public static final String BREAK  = "break";
        /** Are counted as "ally" by any system that cares about that. */
        public static final String ALLY   = "ally";

        public TeamLand                land           = new TeamLand();
        public String                  teamName;
        /** Admins of this team. */
        public Set<UUID>               admin          = Sets.newHashSet();
        /** UUIDs of members of this team. */
        public Set<UUID>               member         = Sets.newHashSet();
        /**
         * Mobs in here are specifically set as protected, this is a whitelist,
         * anything not in here is not protected.
         */
        public Set<UUID>               protected_mobs = Sets.newHashSet();
        /**
         * Mobs in heere are specifically set to be public, this is a
         * whitelist, anything not in here is not public, unless team is set to
         * allPublic
         */
        public Set<UUID>               public_mobs    = Sets.newHashSet();
        /** Non-Stored map for quick lookup of rank for each member. */
        public Map<UUID, PlayerRank>   _ranksMembers  = Maps.newHashMap();
        /** Maps of rank name to rank, this is what is actually stored. */
        public Map<String, PlayerRank> rankMap        = Maps.newHashMap();
        /** List of public blocks for the team. */
        public Set<Coordinate>         anyUse         = Sets.newHashSet();
        /** List of public blocks for the team. TODO implement this. */
        public Set<Coordinate>         anyBreakSet    = Sets.newHashSet();
        /** List of public blocks for the team. TODO implement this. */
        public Set<Coordinate>         anyPlaceSet    = Sets.newHashSet();
        /** Home coordinate for the team, used for thome command. */
        public Coordinate              home;
        /** Message sent on exiting team land. */
        public String                  exitMessage    = "";
        /** Mssage sent on entering team land. */
        public String                  enterMessage   = "";
        /** Message sent when denying interactions in the team. */
        public String                  denyMessage    = "";
        /** Prefix infront of team members names. */
        public String                  prefix         = "";
        /**
         * If true, this team is not cleaned up when empty, and cannot be
         * freely joined when empty.
         */
        public boolean                 reserved       = false;
        /** If this is player specific, currently not used. */
        public boolean                 players        = false;
        /** If true, players cannot take damage here. */
        public boolean                 noPlayerDamage = false;
        /** If true, INPCs cannot take damage here. */
        public boolean                 noNPCDamage    = false;
        /** If true, fakeplayers can run. */
        public boolean                 fakePlayers    = false;
        /** If true, mobs cannot spawn here. */
        public boolean                 noMobSpawn     = false;
        /** If true, team members can hurt each other. */
        public boolean                 friendlyFire   = true;
        /** If true, explosions cannot occur in team land. */
        public boolean                 noExplosions   = false;
        /**
         * If true, anything in this team's land is considered public for
         * interactions.
         */
        public boolean                 allPublic      = false;
        /** If true, any player can place in this teams land. */
        public boolean                 anyPlace       = false;
        /** If true, any player can break in this teams land. */
        public boolean                 anyBreak       = false;
        /** If false, itemframes are not protected from projectiles. */
        public boolean                 protectFrames  = true;
        /** Map of details about team relations. */
        public Map<String, Relation>   relations      = Maps.newHashMap();
        /** Last time a member of this team was seen. */
        public long                    lastSeen       = 0;
        /**
         * Override of maximum land allowed for the team, if this is not -1, it
         * will be used instead.
         */
        public int                     maxLand        = -1;
        /**
         * Override of maximum land allowed for the team, if this is not -1, it
         * will be used instead.
         */
        public int                     maxLoaded      = -1;

        /**
         * Random UUID for the team, this can be used for things like
         * accounts.
         */
        public UUID uuid = UUID.randomUUID();

        private GameProfile _teamprofile;

        public LandTeam()
        {
        }

        public LandTeam(final String name)
        {
            this.teamName = name;
        }

        public GameProfile getProfile()
        {
            if (this._teamprofile == null) this._teamprofile = new GameProfile(this.uuid, "team:" + this.teamName);
            return this._teamprofile;
        }

        public boolean isMember(final UUID id)
        {
            return this.member.contains(id);
        }

        public boolean isMember(final Entity player)
        {
            return this.isMember(player.getUniqueID());
        }

        public boolean isAdmin(final UUID id)
        {
            return this.admin.contains(id);
        }

        public boolean isAdmin(final Entity player)
        {
            return this.isAdmin(player.getUniqueID());
        }

        public boolean hasRankPerm(final UUID player, final String perm)
        {
            if (this == LandManager.getDefaultTeam()) return false;
            if (this.admin.contains(player)) return true;
            final PlayerRank rank = this._ranksMembers.get(player);
            if (rank == null) return false;
            return rank.perms.contains(perm);
        }

        public void setRankPerm(final String rankName, final String perm)
        {
            final PlayerRank rank = this.rankMap.get(rankName);
            if (rank != null) rank.perms.add(perm);
        }

        public void unsetRankPerm(final String rankName, final String perm)
        {
            final PlayerRank rank = this.rankMap.get(rankName);
            if (rank != null) rank.perms.remove(perm);
        }

        /**
         * This is for checking whether the player is in a team with a relation
         * that allows breaking blocks in our land.
         *
         * @param player
         * @return
         */
        public boolean canBreakBlock(final UUID player, final Coordinate location)
        {
            if (this.anyBreak || this.anyBreakSet.contains(location)) return true;
            final LandTeam team = LandManager.getTeam(player);
            final Relation relation = this.relations.get(team.teamName);
            if (relation != null) return relation.perms.contains(LandTeam.BREAK);
            return this.member.contains(player);
        }

        /**
         * This is for checking whether the player is in a team with a relation
         * that allows placing blocks in our land.
         *
         * @param player
         * @return
         */
        public boolean canPlaceBlock(final UUID player, final Coordinate location)
        {
            if (this.anyPlace || this.anyPlaceSet.contains(location)) return true;
            final LandTeam team = LandManager.getTeam(player);
            final Relation relation = this.relations.get(team.teamName);
            if (relation != null) return relation.perms.contains(LandTeam.PLACE);
            return this.member.contains(player);
        }

        /**
         * This is for checking whether the player is in a team with a relation
         * that allows using any random thing in our land.
         *
         * @param player
         * @return
         */
        public boolean canUseStuff(final UUID player, final Coordinate location)
        {
            if (this.allPublic || this.anyUse.contains(location)) return true;
            final LandTeam team = LandManager.getTeam(player);
            final Relation relation = this.relations.get(team.teamName);
            if (relation != null) return relation.perms.contains(LandTeam.PUBLIC);
            return this.member.contains(player);
        }

        public boolean isAlly(final UUID player)
        {
            final LandTeam team = LandManager.getTeam(player);
            if (team != null) return this.isAlly(team);
            return this.member.contains(player);
        }

        public boolean isAlly(final LandTeam team)
        {
            if (team == this) return true;
            final Relation relation = this.relations.get(team.teamName);
            if (relation != null) return relation.perms.contains(LandTeam.ALLY);
            return false;
        }

        public void init(final MinecraftServer server)
        {
            final Set<UUID> members = Sets.newHashSet(this.member);
            if (!this.teamName.equals(Essentials.config.defaultTeamName)) for (final UUID id : members)
                LandManager.getInstance()._playerTeams.put(id, this);
            for (final UUID id : this.public_mobs)
                LandManager.getInstance()._public_mobs.put(id, this);
            for (final UUID id : this.protected_mobs)
                LandManager.getInstance()._protected_mobs.put(id, this);
        }

        @Override
        public boolean equals(final Object o)
        {
            if (o instanceof LandTeam) return ((LandTeam) o).teamName.equals(this.teamName);
            return false;
        }

        @Override
        public int hashCode()
        {
            return this.teamName.hashCode();
        }
    }

    public static class TeamLand
    {
        public HashSet<Coordinate> land   = Sets.newHashSet();
        public HashSet<Coordinate> loaded = Sets.newHashSet();

        public boolean addLand(final Coordinate land)
        {
            return this.land.add(land);
        }

        public int countLand()
        {
            return this.land.size();
        }

        public boolean removeLand(final Coordinate land)
        {
            this.loaded.remove(land);
            return this.land.remove(land);
        }

        public HashSet<Coordinate> getLoaded()
        {
            if (this.loaded == null) this.loaded = Sets.newHashSet();
            return this.loaded;
        }
    }

    static LandManager instance;

    public static final int VERSION = 1;

    public static void clearInstance()
    {
        if (LandManager.instance != null)
        {
            LandSaveHandler.saveGlobalData();
            for (final String s : LandManager.instance._teamMap.keySet())
                LandSaveHandler.saveTeam(s);
        }
        LandManager.instance = null;
    }

    public static LandManager getInstance()
    {
        if (LandManager.instance == null) LandSaveHandler.loadGlobalData();
        return LandManager.instance;
    }

    public static LandTeam getTeam(final UUID id)
    {
        final LandTeam playerTeam = LandManager.getInstance()._playerTeams.get(id);
        if (playerTeam == null) return LandManager.getDefaultTeam();
        return playerTeam;
    }

    public static LandTeam getTeam(final Entity player)
    {
        return LandManager.getTeam(player.getUniqueID());
    }

    public static LandTeam getDefaultTeam()
    {
        return LandManager.getInstance().getTeam(Essentials.config.defaultTeamName, true);
    }

    public static LandTeam getWildTeam()
    {
        if (!Essentials.config.wildernessTeam) return null;
        LandTeam wilds = LandManager.getInstance().getTeam(Essentials.config.wildernessTeamName, false);
        if (wilds == null)
        {
            wilds = LandManager.getInstance().getTeam(Essentials.config.wildernessTeamName, true);
            wilds.reserved = true;
            wilds.allPublic = true;
            wilds.enterMessage = " ";
            wilds.exitMessage = " ";
            wilds.denyMessage = " ";
        }
        return wilds;
    }

    public static boolean owns(final Entity player, final Coordinate chunk)
    {
        return LandManager.getTeam(player).equals(LandManager.getInstance().getLandOwner(chunk));
    }

    public HashMap<String, LandTeam>        _teamMap        = Maps.newHashMap();
    protected HashMap<Coordinate, LandTeam> _landMap        = Maps.newHashMap();
    protected HashMap<UUID, LandTeam>       _playerTeams    = Maps.newHashMap();
    protected HashMap<UUID, Invites>        invites         = Maps.newHashMap();
    protected Map<UUID, LandTeam>           _protected_mobs = Maps.newHashMap();
    protected Map<UUID, LandTeam>           _public_mobs    = Maps.newHashMap();
    public int                              version         = LandManager.VERSION;

    LandManager()
    {
    }

    public void toggleMobProtect(final UUID mob, final LandTeam team)
    {
        if (this._protected_mobs.containsKey(mob))
        {
            this._protected_mobs.remove(mob);
            team.protected_mobs.remove(mob);
        }
        else
        {
            this._protected_mobs.put(mob, team);
            team.protected_mobs.add(mob);
        }
        LandSaveHandler.saveTeam(team.teamName);
    }

    public void toggleMobPublic(final UUID mob, final LandTeam team)
    {
        if (this._public_mobs.containsKey(mob))
        {
            this._public_mobs.remove(mob);
            team.public_mobs.remove(mob);
        }
        else
        {
            this._public_mobs.put(mob, team);
            team.public_mobs.add(mob);
        }
        LandSaveHandler.saveTeam(team.teamName);
    }

    public void renameTeam(final String oldName, final String newName) throws IllegalArgumentException
    {
        if (this._teamMap.containsKey(newName)) throw new IllegalArgumentException(
                "Error, new team name already in use");
        final LandTeam team = this._teamMap.remove(oldName);
        if (team == null) throw new IllegalArgumentException("Error, specified team not found");
        this._teamMap.put(newName, team);
        for (final Invites i : this.invites.values())
            if (i.teams.remove(oldName)) i.teams.add(newName);
        team.teamName = newName;
        LandSaveHandler.saveTeam(newName);
        LandSaveHandler.deleteTeam(oldName);
    }

    public void removeTeam(final String teamName)
    {
        final LandTeam team = this._teamMap.remove(teamName);
        final LandTeam _default = LandManager.getDefaultTeam();
        if (team == _default) return;
        for (final Coordinate c : team.land.land)
            this._landMap.remove(c);
        final MinecraftServer server = LogicalSidedProvider.INSTANCE.get(LogicalSide.SERVER);
        for (final UUID id : team.member)
        {
            _default.member.add(id);
            this._playerTeams.put(id, _default);
            try
            {
                final PlayerEntity player = server.getPlayerList().getPlayerByUUID(id);
                if (player != null)
                {
                    // TODO update name here.
                }
            }
            catch (final Exception e)
            {
                e.printStackTrace();
            }
        }
        LandSaveHandler.saveTeam(_default.teamName);
        for (final Invites i : this.invites.values())
            i.teams.remove(teamName);
        LandSaveHandler.deleteTeam(teamName);
    }

    public void addTeamLand(final String team, final Coordinate land, final boolean sync)
    {
        final LandTeam t = this._teamMap.get(team);
        if (t == null)
        {
            Thread.dumpStack();
            return;
        }
        Essentials.LOGGER.debug("claim: " + team + " Coord: " + land);
        final LandTeam prev = this._landMap.remove(land);
        t.land.addLand(land);
        if (prev != null) prev.land.removeLand(land);
        this._landMap.put(land, t);
        if (sync)
        {
            if (prev != null) LandSaveHandler.saveTeam(prev.teamName);
            LandSaveHandler.saveTeam(team);
        }
    }

    public void addAdmin(final UUID admin, final String team)
    {
        final LandTeam t = this.getTeam(team, true);
        t.admin.add(admin);
        LandSaveHandler.saveTeam(team);
    }

    public void addToTeam(final UUID member, final String team)
    {
        final LandTeam t = this.getTeam(team, true);
        if (t.admin.isEmpty() && !t.teamName.equals(Essentials.config.defaultTeamName)) t.admin.add(member);
        if (this._playerTeams.containsKey(member))
        {
            final LandTeam old = this._playerTeams.remove(member);
            old.member.remove(member);
            old.admin.remove(member);
            LandSaveHandler.saveTeam(old.teamName);
        }
        t.member.add(member);
        this._playerTeams.put(member, t);
        final Invites invite = this.invites.get(member);
        if (invite != null) invite.teams.remove(team);
        LandSaveHandler.saveTeam(team);
        final MinecraftServer server = LogicalSidedProvider.INSTANCE.get(LogicalSide.SERVER);
        try
        {
            final PlayerEntity player = server.getPlayerList().getPlayerByUUID(member);
            if (player != null)
            {
                // TODO update name here.
            }
        }
        catch (final Exception e)
        {
            e.printStackTrace();
        }
    }

    public int countLand(final String team)
    {
        final LandTeam t = this._teamMap.get(team);
        if (t != null) return t.land.countLand();
        return 0;
    }

    public void createTeam(final UUID member, final String team) throws IllegalArgumentException
    {
        if (this._teamMap.containsKey(team)) throw new IllegalArgumentException("thutessentials.error.teamexists");
        final LandTeam theTeam = this.getTeam(team, true);
        if (member != null)
        {
            this.addToTeam(member, team);
            this.addAdmin(member, team);
        }
        else // We made with no member, so this team should be reserved.
            theTeam.reserved = true;
        LandSaveHandler.saveTeam(team);
    }

    public List<String> getInvites(final UUID member)
    {
        final List<String> ret = new ArrayList<>();
        final Invites invite = this.invites.get(member);
        if (invite == null) return ret;
        return Lists.newArrayList(invite.teams);
    }

    public LandTeam getLandOwner(final Coordinate land)
    {
        final LandTeam owner = this._landMap.get(land);
        if (owner == null) return LandManager.getWildTeam();
        return owner;
    }

    public LandTeam getTeam(final String name, final boolean create)
    {
        LandTeam team = this._teamMap.get(name);
        if (team == null && create)
        {
            team = new LandTeam(name);
            this._teamMap.put(name, team);
        }
        return team;
    }

    public List<Coordinate> getTeamLand(final String team)
    {
        final ArrayList<Coordinate> ret = new ArrayList<>();
        final LandTeam t = this._teamMap.get(team);
        if (t != null) ret.addAll(t.land.land);
        return ret;
    }

    public boolean hasInvite(final UUID member, final String team)
    {
        final Invites invite = this.invites.get(member);
        if (invite != null) return invite.teams.contains(team);
        return false;
    }

    public boolean invite(final UUID inviter, final UUID invitee)
    {
        if (!this.isAdmin(inviter)) return false;
        final String team = this._playerTeams.get(inviter).teamName;
        if (this.hasInvite(invitee, team)) return false;
        Invites invite = this.invites.get(invitee);
        if (invite == null)
        {
            invite = new Invites();
            this.invites.put(invitee, invite);
        }
        invite.teams.add(team);
        return true;
    }

    public boolean isAdmin(final UUID member)
    {
        final LandTeam team = this._playerTeams.get(member);
        if (team == null) return false;
        return team.isAdmin(member);
    }

    public boolean isOwned(final Coordinate land)
    {
        return this._landMap.containsKey(land);
    }

    public boolean isPublic(final Coordinate c, final LandTeam team)
    {
        return team.allPublic || team.anyUse.contains(c);
    }

    public boolean isTeamLand(final Coordinate chunk, final String team)
    {
        final LandTeam t = this._teamMap.get(team);
        if (t != null) return t.land.land.contains(chunk);
        return false;
    }

    public void removeAdmin(final UUID member, final String teamName)
    {
        final LandTeam t = this._teamMap.get(teamName);
        if (t != null) t.admin.remove(member);
    }

    public void removeFromInvites(final UUID member, final String team)
    {
        final Invites invite = this.invites.get(member);
        if (invite != null && invite.teams.contains(team))
        {
            invite.teams.remove(team);
            LandSaveHandler.saveGlobalData();
        }
    }

    public void removeFromTeam(final UUID member)
    {
        this.addToTeam(member, LandManager.getDefaultTeam().teamName);
    }

    public void removeTeamLand(final String team, final Coordinate land)
    {
        final LandTeam t = this._teamMap.get(team);
        if (t != null && t.land.removeLand(land))
        {
            this._landMap.remove(land);
            Essentials.LOGGER.debug("unclaim: " + team + " Coord: " + land);
            // Ensure the land is unloaded if it was loaded.
            this.unLoadLand(land, t);
            LandSaveHandler.saveTeam(team);
        }
    }

    public void setPublic(final Coordinate c, final LandTeam owner)
    {
        owner.anyUse.add(c);
        LandSaveHandler.saveTeam(owner.teamName);
    }

    public void unsetPublic(final Coordinate c, final LandTeam owner)
    {
        if (!owner.anyUse.remove(c)) return;
        LandSaveHandler.saveTeam(owner.teamName);
    }

    public void loadLand(final Coordinate chunk, final LandTeam team)
    {
        if (LandEventsHandler.ChunkLoadHandler.addChunks(chunk, team.uuid))
        {
            team.land.getLoaded().add(chunk);
            LandSaveHandler.saveTeam(team.teamName);
        }
    }

    public void unLoadLand(final Coordinate chunk, final LandTeam team)
    {
        if (LandEventsHandler.ChunkLoadHandler.removeChunks(chunk))
        {
            team.land.getLoaded().remove(chunk);
            LandSaveHandler.saveTeam(team.teamName);
        }
    }
}
