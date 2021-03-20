package thut.essentials.land;

import java.util.ArrayList;
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
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunk;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import thut.essentials.Essentials;
import thut.essentials.land.ClaimedCapability.ClaimSegment;
import thut.essentials.land.ClaimedCapability.IClaimed;
import thut.essentials.util.CoordinateUtls;
import thut.essentials.util.InventoryLogger;

public class LandManager
{
    public static class KGobalPos implements Comparable<KGobalPos>
    {
        public static KGobalPos getPosition(final RegistryKey<World> dimension, final BlockPos pos)
        {
            return new KGobalPos(GlobalPos.of(dimension, pos));
        }

        public final GlobalPos pos;

        public KGobalPos(final GlobalPos pos)
        {
            this.pos = pos;
        }

        @Override
        public boolean equals(final Object o)
        {
            if (this == o) return true;
            if (o instanceof KGobalPos)
            {
                final GlobalPos other = ((KGobalPos) o).pos;
                final boolean sameDim = this.pos.dimension().compareTo(other.dimension()) == 0;
                return sameDim && other.pos().equals(this.pos.pos());
            }
            return false;
        }

        @Override
        public int hashCode()
        {
            return this.pos.pos().hashCode();
        }

        public boolean isValid()
        {
            return this.pos != null && this.pos.pos() != null && this.pos.dimension() != null;
        }

        @Override
        public String toString()
        {
            if (this.pos == null || this.pos.dimension() == null) return "ERROR";
            return this.pos.toString();
        }

        @Override
        public int compareTo(final KGobalPos o)
        {
            return o.pos.pos().compareTo(this.pos.pos());
        }

        public BlockPos getPos()
        {
            return this.pos.pos();
        }

        public RegistryKey<World> getDimension()
        {
            return this.pos.dimension();
        }

    }

    public static class Coordinate implements Comparable<Coordinate>
    {
        private static final Map<Integer, RegistryKey<World>> _oldDim = Maps.newHashMap();

        public int x;
        public int y;
        public int z;
        public int dim;

        public static RegistryKey<World> fromOld(final int dim2)
        {
            if (Coordinate._oldDim.isEmpty()) for (final String var : Essentials.config.legacyDimMap)
                try
                {
                    final String[] args = var.split("->");
                    final Integer i = Integer.parseInt(args[0]);
                    final ResourceLocation key = new ResourceLocation(args[1]);
                    final RegistryKey<World> dim = RegistryKey.create(Registry.DIMENSION_REGISTRY, key);
                    final MinecraftServer server = LogicalSidedProvider.INSTANCE.get(LogicalSide.SERVER);
                    if (server.getLevel(dim) == null)
                    {
                        Essentials.LOGGER.error("Dim {} is not a valid world, skipping!", key);
                        continue;
                    }
                    Essentials.LOGGER.error("Dim {} mapped to {}", dim2, key);
                    Coordinate._oldDim.put(i, dim);
                }
                catch (final NumberFormatException e)
                {
                    Essentials.LOGGER.error("Error parsing dimension map for {}", var);
                }
            return Coordinate._oldDim.get(dim2);
        }

        @Override
        public boolean equals(final Object obj)
        {
            if (!(obj instanceof Coordinate)) return false;
            final Coordinate BlockPos = (Coordinate) obj;
            return this.x == BlockPos.x && this.y == BlockPos.y && this.z == BlockPos.z && this.dim == BlockPos.dim;
        }

        @Override
        public int hashCode()
        {
            return this.x + this.z << 8 + this.y << 16 + this.dim << 24;
        }

        @Override
        public int compareTo(final Coordinate p_compareTo_1_)
        {
            return this.y == p_compareTo_1_.y ? this.z == p_compareTo_1_.z ? this.x - p_compareTo_1_.x
                    : this.dim == p_compareTo_1_.dim ? this.z - p_compareTo_1_.z : this.dim - p_compareTo_1_.dim
                    : this.y - p_compareTo_1_.y;
        }
    }

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
        public Set<KGobalPos>          public_use     = Sets.newHashSet();
        /** List of public blocks for the team. TODO implement this. */
        public Set<KGobalPos>          public_break   = Sets.newHashSet();
        /** List of public blocks for the team. TODO implement this. */
        public Set<KGobalPos>          public_place   = Sets.newHashSet();
        /** Home coordinate for the team, used for thome command. */
        public KGobalPos               team_home;
        /** Deprecated for further save compat. */
        @Deprecated
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
            return this.isMember(player.getUUID());
        }

        public boolean isAdmin(final UUID id)
        {
            // True for any team with no admins, if not default team
            if (this.admin.isEmpty() && !this.teamName.equals(Essentials.config.defaultTeamName)) return true;
            return this.admin.contains(id);
        }

        public boolean isAdmin(final Entity player)
        {
            return this.isAdmin(player.getUUID());
        }

        public boolean hasRankPerm(final UUID player, final String perm)
        {
            if (this == LandManager.getDefaultTeam()) return false;
            if (this.isAdmin(player)) return true;
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
        public boolean canBreakBlock(final UUID player, final KGobalPos location)
        {
            if (this.anyBreak || this.public_break.contains(location)) return true;
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
        public boolean canPlaceBlock(final UUID player, final KGobalPos location)
        {
            if (this.anyPlace || this.public_place.contains(location)) return true;
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
        public boolean canUseStuff(final UUID player, final KGobalPos location)
        {
            if (this.allPublic || this.public_use.contains(location)) return true;
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

            LandManager.getInstance()._team_land.put(this.land.uuid, this);
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
        public static UUID _WILDUUID_ = new UUID(1234, 1234);

        public UUID uuid = UUID.randomUUID();

        public Set<KGobalPos> claims = Sets.newHashSet();
        public Set<KGobalPos> loaded = Sets.newHashSet();

        public HashSet<Coordinate> land = Sets.newHashSet();

        public int claimed = 0;

        public boolean addLand(final KGobalPos land)
        {
            return this.claims.add(land);
        }

        public int countLand()
        {
            return this.claimed + this.claims.size();
        }

        public boolean removeLand(final KGobalPos land)
        {
            this.loaded.remove(land);
            return this.claims.remove(land);
        }

        public Set<KGobalPos> getLoaded()
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

    public static boolean isWild(final LandTeam team)
    {
        if (team == null) return true;
        return team == LandManager.getWildTeam() || team == LandManager.getDefaultTeam();
    }

    public static LandTeam getTeam(final UUID id)
    {
        final LandTeam playerTeam = LandManager.getInstance()._playerTeams.get(id);
        if (playerTeam == null) return LandManager.getDefaultTeam();
        return playerTeam;
    }

    public static LandTeam getTeam(final Entity player)
    {
        return LandManager.getTeam(player.getUUID());
    }

    public static LandTeam getNotLoaded()
    {
        LandTeam not_loaded = LandManager.getInstance().getTeam("__not_a_chunk__", false);
        if (not_loaded == null)
        {
            not_loaded = LandManager.getInstance().getTeam("__not_a_chunk__", true);
            not_loaded.reserved = true;
            not_loaded.allPublic = false;
        }
        return not_loaded;
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
            wilds.uuid = TeamLand._WILDUUID_;
            wilds.land.uuid = TeamLand._WILDUUID_;
            wilds.allPublic = true;
            wilds.enterMessage = " ";
            wilds.exitMessage = " ";
            wilds.denyMessage = " ";
        }
        return wilds;
    }

    public Map<String, LandTeam>    _teamMap        = Maps.newConcurrentMap();
    public Map<KGobalPos, LandTeam> _landMap        = Maps.newConcurrentMap();
    protected Map<UUID, LandTeam>   _playerTeams    = Maps.newConcurrentMap();
    protected Map<UUID, Invites>    invites         = Maps.newHashMap();
    protected Map<UUID, LandTeam>   _protected_mobs = Maps.newConcurrentMap();
    protected Map<UUID, LandTeam>   _public_mobs    = Maps.newConcurrentMap();
    public Map<UUID, LandTeam>      _team_land      = Maps.newConcurrentMap();
    public int                      version         = LandManager.VERSION;

    LandManager()
    {
    }

    public LandTeam getTeamForLand(final UUID landId)
    {
        if (landId == null) return LandManager.getWildTeam();
        final LandTeam team = this._team_land.getOrDefault(landId, LandManager.getWildTeam());
        if (team == LandManager.getDefaultTeam()) return LandManager.getWildTeam();
        return team;
    }

    public boolean isPublicMob(final UUID mobId)
    {
        return this._public_mobs.containsKey(mobId);
    }

    public boolean isProtectedMob(final UUID mobId)
    {
        return this._protected_mobs.containsKey(mobId);
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
        if (this._teamMap.containsKey(newName)) throw new IllegalArgumentException("thutessentials.team.teamexists");
        final LandTeam team = this._teamMap.remove(oldName);
        if (team == null) throw new IllegalArgumentException("thutessentials.team.notfound");
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
        for (final KGobalPos c : team.land.claims)
            this._landMap.remove(c);
        final MinecraftServer server = LogicalSidedProvider.INSTANCE.get(LogicalSide.SERVER);
        for (final UUID id : team.member)
        {
            _default.member.add(id);
            this._playerTeams.put(id, _default);
            try
            {
                final PlayerEntity player = server.getPlayerList().getPlayer(id);
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
        this._team_land.remove(team.land.uuid);
        LandSaveHandler.saveTeam(_default.teamName);
        for (final Invites i : this.invites.values())
            i.teams.remove(teamName);
        LandSaveHandler.deleteTeam(teamName);
    }

    public void claimLand(final String team, final World world, final BlockPos pos, final boolean chunkCoords)
    {
        final IClaimed claims = this.getClaimer(world, pos, chunkCoords);
        if (claims == null)
        {
            Thread.dumpStack();
            return;
        }
        final LandTeam t = this._teamMap.get(team);
        if (t == null)
        {
            Thread.dumpStack();
            return;
        }
        final int y = chunkCoords ? pos.getY() : pos.getY() >> 4;
        final ClaimSegment seg = claims.getSegment(y);
        if (seg.owner != null)
        {
            final LandTeam prev = this._team_land.getOrDefault(seg.owner, LandManager.getWildTeam());
            if (!LandManager.isWild(prev))
            {
                Thread.dumpStack();
                return;
            }
        }
        if (seg.owner == null || !seg.owner.equals(t.land.uuid)) t.land.claimed++;
        seg.owner = t.land.uuid;
        KGobalPos c;
        if (chunkCoords) c = KGobalPos.getPosition(world.dimension(), pos);
        else
        {
            final KGobalPos b = KGobalPos.getPosition(world.dimension(), pos);
            c = CoordinateUtls.chunkPos(b);
        }
        InventoryLogger.log("claimed for team: {}", c, team);
        LandSaveHandler.saveTeam(team);
    }

    public void unclaimLand(final String team, final World world, final BlockPos pos, final boolean chunkCoords)
    {
        final IClaimed claims = this.getClaimer(world, pos, chunkCoords);
        if (claims == null)
        {
            Thread.dumpStack();
            return;
        }
        final LandTeam t = this._teamMap.get(team);
        if (t == null)
        {
            Thread.dumpStack();
            return;
        }
        // TODO remove legacy stuff
        KGobalPos c;
        if (chunkCoords) c = KGobalPos.getPosition(world.dimension(), pos);
        else
        {
            final KGobalPos b = KGobalPos.getPosition(world.dimension(), pos);
            c = CoordinateUtls.chunkPos(b);
        }
        t.land.claims.remove(c);
        final int y = chunkCoords ? pos.getY() : pos.getY() >> 4;
        final ClaimSegment seg = claims.getSegment(y);
        if (seg.owner != null && seg.owner.equals(t.land.uuid)) t.land.claimed--;
        seg.owner = null;
        InventoryLogger.log("unclaimed for team: {}", c, team);
        LandSaveHandler.saveTeam(team);
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
            final PlayerEntity player = server.getPlayerList().getPlayer(member);
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
        if (this._teamMap.containsKey(team)) throw new IllegalArgumentException("thutessentials.team.teamexists");
        final LandTeam theTeam = this.getTeam(team, true);
        this._team_land.put(theTeam.land.uuid, theTeam);
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

    public LandTeam getLandOwner(final World world, final BlockPos pos)
    {
        return this.getLandOwner(world, pos, false);
    }

    private IClaimed getClaimer(final World world, final BlockPos pos, final boolean chunkCoords)
    {
        final ChunkPos cPos = chunkCoords ? new ChunkPos(pos.getX(), pos.getZ()) : new ChunkPos(pos);

        if (!world.getServer().isSameThread()) return null;
        if (!world.hasChunk(cPos.x, cPos.z)) return null;

        final IChunk chunk = world.getChunk(cPos.x, cPos.z);
        if (chunk instanceof ICapabilityProvider)
        {
            final IClaimed claims = ((ICapabilityProvider) chunk).getCapability(ClaimedCapability.CAPABILITY).orElse(
                    null);
            return claims;
        }
        return null;
    }

    public LandTeam getLandOwner(final World world, final BlockPos pos, final boolean chunkCoords)
    {
        LandTeam owner = LandManager.getWildTeam();

        // TODO remove legacy stuff
        KGobalPos c;
        if (chunkCoords) c = KGobalPos.getPosition(world.dimension(), pos);
        else
        {
            final KGobalPos b = KGobalPos.getPosition(world.dimension(), pos);
            c = CoordinateUtls.chunkPos(b);
        }
        owner = this.getLandOwner(c);

        final IClaimed claims = this.getClaimer(world, pos, chunkCoords);
        if (claims != null)
        {
            final int y = chunkCoords ? pos.getY() : pos.getY() >> 4;
            final ClaimSegment seg = claims.getSegment(y);
            owner = this.getTeamForLand(seg.owner);
        }
        else return LandManager.getNotLoaded();
        return owner;
    }

    public LandTeam getLandOwner(final KGobalPos land)
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

    public boolean isOwned(final KGobalPos land)
    {
        return this._landMap.containsKey(land);
    }

    public boolean isPublic(final KGobalPos c, final LandTeam team)
    {
        return team.allPublic || team.public_use.contains(c);
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

    public void setPublic(final KGobalPos c, final LandTeam owner)
    {
        owner.public_use.add(c);
        LandSaveHandler.saveTeam(owner.teamName);
    }

    public void unsetPublic(final KGobalPos c, final LandTeam owner)
    {
        if (!owner.public_use.remove(c)) return;
        LandSaveHandler.saveTeam(owner.teamName);
    }

    public boolean loadLand(final KGobalPos chunk, final LandTeam team)
    {
        if (LandEventsHandler.ChunkLoadHandler.addChunks(chunk))
        {
            team.land.getLoaded().add(chunk);
            LandSaveHandler.saveTeam(team.teamName);
            return true;
        }
        return false;
    }

    public boolean unLoadLand(final KGobalPos chunk, final LandTeam team)
    {
        if (LandEventsHandler.ChunkLoadHandler.removeChunks(chunk))
        {
            team.land.getLoaded().remove(chunk);
            LandSaveHandler.saveTeam(team.teamName);
            return true;
        }
        return false;
    }
}
