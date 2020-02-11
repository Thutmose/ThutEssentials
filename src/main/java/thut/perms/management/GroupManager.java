package thut.perms.management;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;

import net.minecraft.server.MinecraftServer;
import thut.perms.Perms;

public class GroupManager
{
    private static GroupManager _instance;

    /** Map of dimension id to group manager. */
    public static Map<Integer, GroupManager> _instanceMap = Maps.newHashMap();

    public Map<UUID, Group>   _groupIDMap   = Maps.newHashMap();
    public Map<String, Group> _groupNameMap = Maps.newHashMap();
    public HashSet<Group>     groups        = Sets.newHashSet();

    public Group initial = new Group("default");
    public Group mods    = new Group("mods");

    public PlayerManager   _manager = null;
    public MinecraftServer _server;

    /** @return the _instance */
    public static GroupManager get_instance()
    {
        return GroupManager._instance;
    }

    /**
     * @param _instance
     *            the _instance to set
     */
    public static void set_instance(final GroupManager _instance)
    {
        GroupManager._instance = _instance;
    }

    public GroupManager()
    {
        Perms.LOGGER.info("Initializing Group Manager.");
        this._manager = new PlayerManager(this);
    }

    public void init()
    {
        if (this.initial == null) this.initial = new Group("default");
        this._groupNameMap.put(this.initial.name, this.initial);
        if (this.mods == null) this.mods = new Group("mods");
        this._groupNameMap.put(this.mods.name, this.mods);
        for (final Group g : this.groups)
        {
            if (g.name.isEmpty()) g.name = "unnamed" + new Random().nextFloat();
            this._groupNameMap.put(g.name, g);
            for (final UUID id : g.members)
            {
                this.initial.members.remove(id);
                this.mods.members.remove(id);
                this._groupIDMap.put(id, g);
            }
        }

        // Refeshes players in groups to ensure that there is only 1 group with
        // each player, this cleans up some issues with badly formatted
        // permissions files
        for (final UUID id : this._groupIDMap.keySet())
            Perms.addToGroup(id, this._groupIDMap.get(id).name);

        this.mods.setAll(true);
        this._groupNameMap.put(this.initial.name, this.initial);
        this._groupNameMap.put(this.mods.name, this.mods);

        // Set up parents.
        for (final Group g : this.groups)
            if (g.parentName != null)
            {
                g._parent = this._groupNameMap.get(g.parentName);
                if (g._parent == null) g.parentName = null;
            }
    }

    public Group getPlayerGroup(final UUID id)
    {
        final Group ret = this._groupIDMap.get(id);
        if (ret == null)
        {
            if (this._server.getPlayerList().getOppedPlayers().getEntry(new GameProfile(id, null)) != null)
                return this.mods;
            return this.initial;
        }
        return ret;
    }

    public boolean hasPermission(final UUID id, final String perm)
    {
        final Group g = this.getPlayerGroup(id);
        final Player player = this._manager.getPlayer(id);
        final boolean canPlayerUse = player != null ? player.hasPermission(perm) : false;

        // Check if that player is specifically denied the perm.
        if (player != null && player.isDenied(perm)) return false;

        return g.hasPermission(perm) || canPlayerUse;
    }

}
