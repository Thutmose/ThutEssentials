package thut.essentials.events;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.event.entity.player.PlayerEvent;
import thut.essentials.land.LandManager.KGobalPos;
import thut.essentials.util.CoordinateUtls;

/** Fired before the player is moved. */
public class MoveEvent extends PlayerEvent
{
    public MoveEvent(final PlayerEntity player)
    {
        super(player);
    }

    public KGobalPos getPos()
    {
        return CoordinateUtls.forMob(this.getPlayer());
    }
}