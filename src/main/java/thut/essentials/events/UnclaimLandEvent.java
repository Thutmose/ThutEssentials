package thut.essentials.events;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
import thut.essentials.land.LandManager.KGobalPos;

@Cancelable
public class UnclaimLandEvent extends Event
{
    public final KGobalPos    land;
    public final String       team;
    public final PlayerEntity claimer;

    public UnclaimLandEvent(final KGobalPos land, final PlayerEntity claimer, final String team)
    {
        this.land = land;
        this.team = team;
        this.claimer = claimer;
    }
}