package thut.perms.management;

import java.util.List;
import java.util.UUID;

import com.google.common.collect.Lists;

public class Player extends PermissionsHolder
{
    public UUID         id;
    public List<String> groups  = Lists.newArrayList();
    public List<Group>  _groups = Lists.newArrayList();

    public Player()
    {
    }

    public boolean addGroup(final Group group)
    {
        if (this.groups.contains(group.name)) return false;
        this.groups.add(group.name);
        this._groups.add(group);
        return true;
    }

    public boolean removeGroup(final Group group)
    {
        if (!this._groups.contains(group)) return false;
        this.groups.remove(group.name);
        this._groups.remove(group);
        return true;
    }

    @Override
    public boolean isAllowed(final String permission)
    {
        for (final Group group : this._groups)
            if (group.isAllowed(permission)) return true;
        return super.isAllowed(permission);
    }

    @Override
    public boolean isDenied(final String permission)
    {
        for (final Group group : this._groups)
            if (group.isDenied(permission)) return true;
        return super.isDenied(permission);
    }

    @Override
    public boolean isAll_non_op()
    {
        return false;
    }
}
