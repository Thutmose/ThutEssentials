package thut.essentials.api.events;

import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
import thut.essentials.land.claims.ClaimedVolume;

public abstract class ClaimEvent extends Event
{
    public final ClaimedVolume volume;
    public final Level level;

    protected ClaimEvent(ClaimedVolume volume, Level level) {this.volume = volume;
        this.level = level;
    }

    public static class Claim extends ClaimEvent
    {
        public Claim(ClaimedVolume volume, Level level)
        {
            super(volume, level);
        }
    }

    public static class Unclaim extends ClaimEvent
    {
        public Unclaim(ClaimedVolume volume, Level level)
        {
            super(volume, level);
        }
    }
}
