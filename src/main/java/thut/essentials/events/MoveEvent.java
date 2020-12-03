package thut.essentials.events;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.GlobalPos;
import net.minecraftforge.event.entity.player.PlayerEvent;
import thut.essentials.util.CoordinateUtls;

/** Fired before the player is moved. */
public class MoveEvent extends PlayerEvent
{
    public MoveEvent(final PlayerEntity player)
    {
        super(player);
    }

    public GlobalPos getPos()
    {
        return CoordinateUtls.forMob(this.getPlayer());
    }
}