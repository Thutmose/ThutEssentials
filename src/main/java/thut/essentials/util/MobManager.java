package thut.essentials.util;

import com.google.common.collect.Sets;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent.PositionCheck;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent.SpawnPlacementCheck.Result;
import thut.essentials.Essentials;

import java.util.Set;

public class MobManager
{
    private static final Set<ResourceLocation> griefWhitelist = Sets.newHashSet();
    private static final Set<ResourceLocation> griefBlacklist = Sets.newHashSet();

    private static final Set<ResourceLocation> spawnWhitelist = Sets.newHashSet();
    private static final Set<ResourceLocation> spawnBlacklist = Sets.newHashSet();

    public static void init()
    {
        MobManager.griefWhitelist.clear();
        MobManager.griefBlacklist.clear();
        MobManager.spawnWhitelist.clear();
        MobManager.spawnBlacklist.clear();

        for (final String s : Essentials.config.mobGriefAllowWhitelist)
            MobManager.griefWhitelist.add(ResourceLocation.parse(s));
        for (final String s : Essentials.config.mobGriefAllowBlacklist)
            MobManager.griefBlacklist.add(ResourceLocation.parse(s));
        for (final String s : Essentials.config.mobSpawnWhitelist)
            MobManager.spawnWhitelist.add(ResourceLocation.parse(s));
        for (final String s : Essentials.config.mobSpawnBlacklist)
            MobManager.spawnBlacklist.add(ResourceLocation.parse(s));

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
            final boolean valid =
                    evt.getEntity() != null && MobManager.griefWhitelist.contains(RegHelper.getKey(evt.getEntity()));
            evt.setCanGrief(!valid);
        }
        else
        {
            final boolean valid =
                    evt.getEntity() != null && MobManager.griefBlacklist.contains(RegHelper.getKey(evt.getEntity()));
            evt.setCanGrief(!valid);
        }
    }

    @SubscribeEvent
    public static void mobSpawning(final MobSpawnEvent.SpawnPlacementCheck evt)
    {
        if (evt.getResult() != Result.DEFAULT) return;
        if (Essentials.config.mobSpawnUsesWhitelist)
        {
            final boolean valid = MobManager.spawnWhitelist.contains(RegHelper.getKey(evt.getEntityType()));
            evt.setResult(valid ? Result.DEFAULT : Result.FAIL);
            return;
        }
        final boolean valid = MobManager.spawnBlacklist.contains(RegHelper.getKey(evt.getEntityType()));
        evt.setResult(valid ? Result.FAIL : Result.DEFAULT);
    }

    @SubscribeEvent
    public static void mobSpawning(final PositionCheck evt)
    {
        if (evt.getResult() != PositionCheck.Result.DEFAULT) return;
        if (Essentials.config.mobSpawnUsesWhitelist)
        {
            final boolean valid = MobManager.spawnWhitelist.contains(RegHelper.getKey(evt.getEntity()));
            evt.setResult(valid ? PositionCheck.Result.DEFAULT : PositionCheck.Result.FAIL);
            return;
        }
        final boolean valid = MobManager.spawnBlacklist.contains(RegHelper.getKey(evt.getEntity()));
        evt.setResult(valid ? PositionCheck.Result.FAIL : PositionCheck.Result.DEFAULT);
    }
}
