package thut.essentials.events;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

@Cancelable
public class ClaimLandEvent extends Event
{
    public final BlockPos     land;
    public final String       team;
    public final PlayerEntity claimer;
    public final int          dimension;

    public ClaimLandEvent(final BlockPos land, final int dimension, final PlayerEntity claimer, final String team)
    {
        this.land = land;
        this.dimension = dimension;
        this.team = team;
        this.claimer = claimer;
    }
}