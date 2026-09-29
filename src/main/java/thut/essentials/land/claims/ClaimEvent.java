package thut.essentials.land.claims;

import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;

public abstract class ClaimEvent extends Event
{
    public final ClaimedVolume volume;
    public final Level level;

    protected ClaimEvent(ClaimedVolume volume, Level level) {this.volume = volume;
        this.level = level;
    }

    public static class Claim extends ClaimEvent
    {
        protected Claim(ClaimedVolume volume, Level level)
        {
            super(volume, level);
        }
    }

    public static class Unclaim extends ClaimEvent
    {
        protected Unclaim(ClaimedVolume volume, Level level)
        {
            super(volume, level);
        }
    }
}
