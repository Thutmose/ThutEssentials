package thut.essentials.util;

import java.util.Set;

import com.google.common.collect.Sets;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.EntityMobGriefingEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import thut.essentials.Essentials;

public class MobManager
{
    private static Set<ResourceLocation> griefWhitelist = Sets.newHashSet();
    private static Set<ResourceLocation> griefBlacklist = Sets.newHashSet();

    private static Set<ResourceLocation> spawnWhitelist = Sets.newHashSet();
    private static Set<ResourceLocation> spawnBlacklist = Sets.newHashSet();

    public static void init()
    {
        MobManager.griefWhitelist.clear();
        MobManager.griefBlacklist.clear();
        MobManager.spawnWhitelist.clear();
        MobManager.spawnBlacklist.clear();

        for (final String s : Essentials.config.mobGriefAllowWhitelist)
            MobManager.griefWhitelist.add(new ResourceLocation(s));
        for (final String s : Essentials.config.mobGriefAllowBlacklist)
            MobManager.griefBlacklist.add(new ResourceLocation(s));
        for (final String s : Essentials.config.mobSpawnWhitelist)
            MobManager.spawnWhitelist.add(new ResourceLocation(s));
        for (final String s : Essentials.config.mobSpawnBlacklist)
            MobManager.spawnBlacklist.add(new ResourceLocation(s));

    }

    public static boolean isWhitelistedForGriefing(final Entity mob)
    {
        if (mob == null) return false;
        return MobManager.griefWhitelist.contains(RegHelper.getKey(mob));
    }

    @SubscribeEvent
    public static void mobGriefing(final EntityMobGriefingEvent evt)
    {
        if (Essentials.config.mobGriefAllowUsesWhitelist)
        {
            final boolean valid = evt.getEntity() != null
                    && MobManager.griefWhitelist.contains(RegHelper.getKey(evt.getEntity()));
            evt.setResult(valid ? Result.ALLOW : Result.DEFAULT);
        }
        else
        {
            final boolean valid = evt.getEntity() != null
                    && MobManager.griefBlacklist.contains(RegHelper.getKey(evt.getEntity()));
            evt.setResult(valid ? Result.DENY : Result.DEFAULT);
        }
    }

    @SubscribeEvent
    public static void mobSpawning(final MobSpawnEvent.SpawnPlacementCheck evt)
    {
        if (evt.getResult() != Result.DEFAULT) return;
        if (Essentials.config.mobSpawnUsesWhitelist)
        {
            final boolean valid = MobManager.spawnWhitelist.contains(RegHelper.getKey(evt.getEntityType()));
            evt.setResult(valid ? Result.DEFAULT : Result.DENY);
            return;
        }
        final boolean valid = MobManager.spawnBlacklist.contains(RegHelper.getKey(evt.getEntityType()));
        evt.setResult(valid ? Result.DENY : Result.DEFAULT);
    }

    @SubscribeEvent
    public static void mobSpawning(final MobSpawnEvent.FinalizeSpawn evt)
    {
        if (evt.getResult() != Result.DEFAULT) return;
        if (Essentials.config.mobSpawnUsesWhitelist)
        {
            final boolean valid = MobManager.spawnWhitelist.contains(RegHelper.getKey(evt.getEntity()));
            evt.setResult(valid ? Result.DEFAULT : Result.DENY);
            return;
        }
        final boolean valid = MobManager.spawnBlacklist.contains(RegHelper.getKey(evt.getEntity()));
        evt.setResult(valid ? Result.DENY : Result.DEFAULT);
    }
}
