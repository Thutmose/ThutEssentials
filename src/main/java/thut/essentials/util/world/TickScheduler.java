package thut.essentials.util.world;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.Lists;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.WorldTickEvent;

public class TickScheduler
{
    private static final Map<ResourceKey<Level>, List<Runnable>> endTickRuns   = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<Level>, List<Runnable>> startTickRuns = new ConcurrentHashMap<>();

    public static class CustomRunnable implements Runnable
    {
        private final Runnable wrapped;

        public int ticks = 0;

        public CustomRunnable(final Runnable wrap, final int timer)
        {
            this.wrapped = wrap;
            this.ticks = timer;
        }

        public boolean isDone()
        {
            return this.ticks <= 0;
        }

        @Override
        public void run()
        {
            this.wrapped.run();
            this.ticks--;
        }
    }

    public static void Schedule(final ResourceKey<Level> key, final Runnable task, final boolean postTick)
    {
        final Map<ResourceKey<Level>, List<Runnable>> map = postTick ? TickScheduler.endTickRuns
                : TickScheduler.startTickRuns;
        synchronized (map)
        {
            List<Runnable> list = map.get(key);
            if (list == null) map.put(key, list = Lists.newArrayList());
            list.add(task);
        }
    }

    public static void onWorldTick(final WorldTickEvent event)
    {
        if (event.world instanceof ServerLevel)
        {
            final ResourceKey<Level> key = event.world.dimension();
            final Map<ResourceKey<Level>, List<Runnable>> map = event.phase == Phase.END ? TickScheduler.endTickRuns
                    : TickScheduler.startTickRuns;
            synchronized (map)
            {
                List<Runnable> list = map.get(key);
                if (list == null) map.put(key, list = Lists.newArrayList());
                list.removeIf(r ->
                {
                    r.run();
                    if (r instanceof CustomRunnable) if (!((CustomRunnable) r).isDone()) return false;
                    return true;
                });
            }
        }
    }
}
