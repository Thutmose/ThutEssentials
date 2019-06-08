package thut.essentials.events;

import net.minecraft.entity.player.EntityPlayer;

public class NameEvent extends Event
{
    public final EntityPlayer toName;
    String                    displayName;
    String                    newName;

    public NameEvent(EntityPlayer target, String name)
    {
        displayName = name;
        newName = name;
        toName = target;
    }

    public void setName(String name)
    {
        newName = name;
    }

    public String getName()
    {
        return newName;
    }

    public String getDefaultName()
    {
        return displayName;
    }
}
