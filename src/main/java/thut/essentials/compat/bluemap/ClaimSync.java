package thut.essentials.compat.bluemap;

import com.flowpowered.math.vector.Vector2d;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.markers.ExtrudeMarker;
import de.bluecolored.bluemap.api.markers.Marker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import thut.essentials.land.claims.CapabilityWorldVolumes;
import thut.essentials.land.claims.ClaimEvent;
import thut.essentials.land.claims.ClaimedVolume;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClaimSync
{
    static Map<ResourceKey<Level>, MarkerSet> SETS = new ConcurrentHashMap<>();

    private static Marker forVolume(ClaimedVolume volume)
    {
        var bounds = AABB.of(volume.getTotalBounds());
        double yMin = bounds.minY;
        double yMax = bounds.maxY;
        List<Vector2d> points = new ArrayList<>();
        points.add(new Vector2d(bounds.minX, bounds.minZ));
        points.add(new Vector2d(bounds.maxX, bounds.minZ));
        points.add(new Vector2d(bounds.maxX, bounds.maxZ));
        points.add(new Vector2d(bounds.minX, bounds.maxZ));
        Color cFill = new Color(volume.getColour(), 0.5f);
        Color cLine = new Color(volume.getColour(), 0.9f);
        var builder = ExtrudeMarker.builder().label(volume.getName())
                .shape(Shape.builder().addPoints(points).build(), (float) yMin, (float) yMax).fillColor(cFill)
                .lineColor(cLine);
        return builder.build();
    }

    public static void init()
    {
        // Using a listener to do something as soon as the API is available
        BlueMapAPI.onEnable(api -> {
            //code executed when the api got enabled
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server != null)
            {
                for (var level : server.getAllLevels())
                {
                    api.getWorld(level).ifPresent(world -> {
                        for (BlueMapMap map : world.getMaps())
                        {
                            var markerSet = MarkerSet.builder().label("Claims").defaultHidden(true).build();
                            SETS.put(level.dimension(), markerSet);
                            var volumes = CapabilityWorldVolumes.get(level);
                            volumes.getVolumes().forEach(volume -> {
                                if (volume instanceof ClaimedVolume v)
                                {
                                    var m = forVolume(v);
                                    v.extraKey = v.getName() + " " + v.getTotalBounds();
                                    markerSet.put(v.extraKey, m);
                                }
                            });
                            map.getMarkerSets().put("thutessentials-claims-set", markerSet);
                        }
                    });
                }
            }
        });
        NeoForge.EVENT_BUS.register(ClaimSync.class);
    }

    @SubscribeEvent
    public static void onClaim(ClaimEvent.Unclaim event)
    {
        _onClaim(event);
    }

    @SubscribeEvent
    public static void onClaim(ClaimEvent.Claim event)
    {
        _onClaim(event);
    }

    public static void _onClaim(ClaimEvent event)
    {
        if (!(event.level instanceof ServerLevel level)) return;
        var volume = event.volume;
        var set = SETS.get(level.dimension());
        if (set == null)
        {
            BlueMapAPI.getInstance().flatMap(api -> api.getWorld(level)).ifPresent(world -> {
                for (BlueMapMap map : world.getMaps())
                {
                    var markerSet = MarkerSet.builder().label("Claims").defaultHidden(true).build();
                    SETS.put(level.dimension(), markerSet);
                    map.getMarkerSets().put("thutessentials-claims-set", markerSet);
                }
            });
            set = SETS.get(level.dimension());
            if (set == null) return;
        }
        if (event instanceof ClaimEvent.Unclaim)
        {
            // Remove the claim marker
            set.remove(volume.extraKey);
        }
        else
        {
            var m = forVolume(volume);
            volume.extraKey = volume.getName() + " " + volume.getTotalBounds();
            set.put(volume.extraKey, m);
        }
    }
}
