package thut.essentials.events;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import thut.essentials.land.LandManager.KGobalPos;
import thut.essentials.util.CoordinateUtls;

/** Fired before the player is moved. */
public class MoveEvent extends PlayerEvent
{
    public MoveEvent(final Player player)
    {
        super(player);
    }

    public KGobalPos getPos()
    {
        return CoordinateUtls.forMob(this.getEntity());
    }
}