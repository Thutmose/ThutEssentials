package thut.essentials.util.world;

import com.google.common.collect.Lists;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TickScheduler
{
    private static final Map<ResourceKey<Level>, List<Runnable>> endTickRuns = new ConcurrentHashMap<>();
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
        final Map<ResourceKey<Level>, List<Runnable>> map = postTick
                ? TickScheduler.endTickRuns
                : TickScheduler.startTickRuns;
        synchronized (map)
        {
            List<Runnable> list = map.computeIfAbsent(key, k -> Lists.newArrayList());
            list.add(task);
        }
    }

    public static void onWorldTickPost(final LevelTickEvent.Post event)
    {
        if (event.getLevel() instanceof ServerLevel)
        {
            final ResourceKey<Level> key = event.getLevel().dimension();
            final Map<ResourceKey<Level>, List<Runnable>> map = TickScheduler.endTickRuns;
            synchronized (map)
            {
                List<Runnable> list = map.computeIfAbsent(key, k -> Lists.newArrayList());
                list.removeIf(r -> {
                    r.run();
                    if (r instanceof CustomRunnable) return ((CustomRunnable) r).isDone();
                    return true;
                });
            }
        }
    }

    public static void onWorldTickPre(final LevelTickEvent.Pre event)
    {
        if (event.getLevel() instanceof ServerLevel)
        {
            final ResourceKey<Level> key = event.getLevel().dimension();
            final Map<ResourceKey<Level>, List<Runnable>> map = TickScheduler.startTickRuns;
            synchronized (map)
            {
                List<Runnable> list = map.computeIfAbsent(key, k -> Lists.newArrayList());
                list.removeIf(r -> {
                    r.run();
                    if (r instanceof CustomRunnable) return ((CustomRunnable) r).isDone();
                    return true;
                });
            }
        }
    }
}
