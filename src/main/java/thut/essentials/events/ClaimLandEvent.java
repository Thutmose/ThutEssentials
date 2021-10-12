package thut.essentials.events;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
import thut.essentials.land.LandManager.KGobalPos;

@Cancelable
public class ClaimLandEvent extends Event
{
    public final KGobalPos    land;
    public final String       team;
    public final Player claimer;

    public ClaimLandEvent(final KGobalPos land, final Player claimer, final String team)
    {
        this.land = land;
        this.team = team;
        this.claimer = claimer;
    }
}