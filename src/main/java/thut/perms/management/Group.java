package thut.perms.management;

import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Sets;

public class Group extends PermissionsHolder
{
    public String    name;
    public String    prefix  = "";
    public String    suffix  = "";
    public Set<UUID> members = Sets.newHashSet();

    public Group(final String name)
    {
        this.name = name;
    }
}
