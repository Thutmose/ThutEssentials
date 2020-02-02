package thut.essentials.events;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.player.PlayerEvent;

/** Fired before the player is moved. */
public class MoveEvent extends PlayerEvent
{
    public MoveEvent(final PlayerEntity player)
    {
        super(player);
    }

    public int[] getPos()
    {
        final BlockPos pos = this.getPlayer().getPosition();
        return new int[] { pos.getX(), pos.getY(), pos.getZ(), this.getPlayer().dimension.getId() };
    }
}