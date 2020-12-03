package thut.essentials.events;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.GlobalPos;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

@Cancelable
public class ClaimLandEvent extends Event
{
    public final GlobalPos    land;
    public final String       team;
    public final PlayerEntity claimer;

    public ClaimLandEvent(final GlobalPos land, final PlayerEntity claimer, final String team)
    {
        this.land = land;
        this.team = team;
        this.claimer = claimer;
    }
}