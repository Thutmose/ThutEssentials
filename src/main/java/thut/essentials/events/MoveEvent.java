package thut.essentials.events;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.core.GlobalPos;
import thut.essentials.util.CoordinateUtls;

/** Fired before the player is moved. */
public class MoveEvent extends PlayerEvent
{
    public MoveEvent(final Player player)
    {
        super(player);
    }

    public GlobalPos getPos()
    {
        return CoordinateUtls.forMob(this.getEntity());
    }
}