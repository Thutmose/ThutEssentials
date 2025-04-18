package thut.essentials.events;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.minecraft.core.GlobalPos;

public class ClaimLandEvent extends Event implements ICancellableEvent
{
    public final GlobalPos    land;
    public final String       team;
    public final Player claimer;

    public ClaimLandEvent(final GlobalPos land, final Player claimer, final String team)
    {
        this.land = land;
        this.team = team;
        this.claimer = claimer;
    }
}