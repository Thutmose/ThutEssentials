package thut.essentials.events;

import net.minecraft.entity.player.PlayerEntity;

public class NameEvent extends Event
{
    public final PlayerEntity toName;
    String                    displayName;
    String                    newName;

    public NameEvent(PlayerEntity target, String name)
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
