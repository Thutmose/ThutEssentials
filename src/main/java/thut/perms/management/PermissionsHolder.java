package thut.perms.management;

import java.util.List;

import com.google.common.collect.Lists;

import net.minecraftforge.server.permission.DefaultPermissionLevel;
import thut.perms.Perms;

public abstract class PermissionsHolder
{
    public boolean           all             = false;
    public boolean           all_non_op      = true;
    public List<String>      allowedCommands = Lists.newArrayList();
    public List<String>      bannedCommands  = Lists.newArrayList();
    public String            parentName      = null;
    public PermissionsHolder _parent;
    protected List<String>   _whiteWildCards;
    protected List<String>   _blackWildCards;
    public boolean           _init           = false;

    private void init()
    {
        this._whiteWildCards = Lists.newArrayList();
        this._blackWildCards = Lists.newArrayList();
        if (this.allowedCommands == null) this.allowedCommands = Lists.newArrayList();
        if (this.bannedCommands == null) this.bannedCommands = Lists.newArrayList();
        this._init = true;
        for (final String s : this.allowedCommands)
            if (s.endsWith("*")) this._whiteWildCards.add(s.substring(0, s.length() - 1));
            else if (s.startsWith("*")) this._whiteWildCards.add(s.substring(1));
        for (final String s : this.bannedCommands)
            if (s.endsWith("*")) this._blackWildCards.add(s.substring(0, s.length() - 1));
            else if (s.startsWith("*")) this._blackWildCards.add(s.substring(1));
    }

    public List<String> getAllowedCommands()
    {
        if (this.allowedCommands == null) this.allowedCommands = Lists.newArrayList();
        return this.allowedCommands;
    }

    public List<String> getBannedCommands()
    {
        if (this.bannedCommands == null) this.bannedCommands = Lists.newArrayList();
        return this.bannedCommands;
    }

    public boolean isDenied(final String permission)
    {
        if (this._parent != null && this._parent.isDenied(permission)) return true;
        if (!this._init || this._blackWildCards == null || this.bannedCommands == null) this.init();
        for (final String pattern : this._blackWildCards)
            if (permission.startsWith(pattern)) return true;
            else if (permission.matches(pattern)) return true;
        if (this.bannedCommands.contains(permission)) return true;
        return false;
    }

    public boolean isAllowed(final String permission)
    {
        if (this._parent != null && this._parent.isAllowed(permission)) return true;
        if (this.isAll()) return true;
        if (!this._init || this._whiteWildCards == null || this.allowedCommands == null) this.init();
        if (this.isAll_non_op() && Perms.manager.getDefaultPermissionLevel(permission) == DefaultPermissionLevel.ALL)
            return true;
        for (final String pattern : this._whiteWildCards)
            if (permission.startsWith(pattern)) return true;
            else if (permission.matches(pattern)) return true;
        return this.allowedCommands.contains(permission);
    }

    public boolean hasPermission(final String permission)
    {
        // Check if permission is specifically denied.
        if (this.isDenied(permission)) return false;
        // check if permission is allowed.
        return this.isAllowed(permission);
    }

    public boolean isAll()
    {
        return this.all;
    }

    public void setAll(final boolean all)
    {
        this.all = all;
    }

    public boolean isAll_non_op()
    {
        return this.all_non_op;
    }

    public void setAll_non_op(final boolean all_non_op)
    {
        this.all_non_op = all_non_op;
    }

}
