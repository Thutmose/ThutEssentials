package thut.essentials.compat.xaeros;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import thut.essentials.api.events.ClaimEvent;
import thut.essentials.land.claims.ClaimInfo;
import thut.essentials.land.claims.ClaimSyncPacket;
import thut.essentials.land.claims.ClaimedVolume;
import thut.essentials.api.level.GlobalChunkPos;
import thut.essentials.api.level.StructureManager;
import xaero.map.WorldMapSession;
import xaero.map.gui.GuiMap;
import xaero.map.gui.MapTileSelection;
import xaero.map.gui.dropdown.rightclick.RightClickOption;
import xaero.map.world.MapDimension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ClaimSync
{
    public static void init()
    {
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
        if (event.level instanceof ServerLevel) return;
        // need to force a refresh of map.
        var list = StructureManager.forVolume(event.volume, event.level.dimension());
        WorldMapSession session = WorldMapSession.getCurrentSession();
        MapDimension mapDim = session.getMapProcessor().getMapWorld()
                .getDimension(ResourceKey.create(Registries.DIMENSION, event.level.dimension().location()));
        if (mapDim == null) return;
        Set<GlobalChunkPos> rSet = new HashSet<>();
        list.forEach(pos -> {
            var rPos = new GlobalChunkPos(pos.world, new ChunkPos(pos.pos.getRegionX(), pos.pos.getRegionZ()));
            if (rSet.add(rPos))
            {
                int regionX = rPos.pos.x;
                int regionZ = rPos.pos.z;
                for (int i = -1; i < 2; ++i)
                {
                    for (int j = -1; j < 2; ++j)
                    {
                        if (i == 0 && j == 0 || i * i != j * j)
                        {
                            mapDim.getHighlightHandler().clearCachedHash(regionX + i, regionZ + j);
                        }
                    }
                }
            }
        });
    }

    public static void doClaim(MapTileSelection mapTileSelection, ResourceKey<Level> rightClickDim, GuiMap us,
            ArrayList<RightClickOption> options)
    {
        var level = Minecraft.getInstance().level;
        if (rightClickDim != level.dimension()) return;

        int x0 = mapTileSelection.getStartX();
        int z0 = mapTileSelection.getStartZ();
        // Ends are not inclusive
        int x1 = mapTileSelection.getEndX();
        int z1 = mapTileSelection.getEndZ();

        int xMin = SectionPos.sectionToBlockCoord(Math.min(x0, x1));
        int xMax = SectionPos.sectionToBlockCoord(Math.max(x0, x1), 15);
        int zMin = SectionPos.sectionToBlockCoord(Math.min(z0, z1));
        int zMax = SectionPos.sectionToBlockCoord(Math.max(z0, z1), 15);

        int yTopHere = SectionPos.sectionToBlockCoord(
                SectionPos.blockToSectionCoord(Minecraft.getInstance().player.getBlockY()),15);

        BoundingBox testBox = new BoundingBox(xMin, level.getMinBuildHeight(), zMin, xMax, level.getMaxBuildHeight(),
                zMax);
        var testVol = new ClaimedVolume(testBox, new ClaimInfo());
        var claims = StructureManager.getColliding(rightClickDim, testVol);
        boolean alt = Screen.hasAltDown();
        boolean ctrl = Screen.hasControlDown();
        if(claims.isEmpty())
        {
            if (alt && ctrl) options.add(new RightClickOption("thutessentials.xaeros.claim.here", options.size(), us)
            {
                @Override
                public void onAction(Screen screen)
                {
                    int yMin = yTopHere - 15;
                    BoundingBox box = new BoundingBox(xMin, yMin, zMin, xMax, yTopHere, zMax);
                    ClaimSyncPacket.tryClaim(box);
                }
            });
            else if (alt) options.add(new RightClickOption("thutessentials.xaeros.claim.here.up", options.size(), us)
            {
                @Override
                public void onAction(Screen screen)
                {
                    int yMax = level.getMaxBuildHeight();
                    BoundingBox box = new BoundingBox(xMin, yTopHere, zMin, xMax, yMax, zMax);
                    ClaimSyncPacket.tryClaim(box);
                }
            });
            else if (ctrl) options.add(new RightClickOption("thutessentials.xaeros.claim.here.down", options.size(), us)
            {
                @Override
                public void onAction(Screen screen)
                {
                    int yMin = level.getMinBuildHeight();
                    BoundingBox box = new BoundingBox(xMin, yMin, zMin, xMax, yTopHere, zMax);
                    ClaimSyncPacket.tryClaim(box);
                }
            });
            else options.add(new RightClickOption("thutessentials.xaeros.claim", options.size(), us)
                {
                    @Override
                    public void onAction(Screen screen)
                    {
                        int yMin = level.getMinBuildHeight();
                        int yMax = level.getMaxBuildHeight();
                        BoundingBox box = new BoundingBox(xMin, yMin, zMin, xMax, yMax, zMax);
                        ClaimSyncPacket.tryClaim(box);
                    }
                });
        }
        if (!claims.isEmpty())
        {
            boolean canMerge = claims.size() > 1;
            if(canMerge)
            {
                options.add(new RightClickOption("thutessentials.xaeros.merge", options.size(), us)
                {
                    @Override
                    public void onAction(Screen screen)
                    {
                        int yMin = level.getMinBuildHeight();
                        int yMax = level.getMaxBuildHeight();
                        BoundingBox box = new BoundingBox(xMin, yMin, zMin, xMax, yMax, zMax);
                        ClaimSyncPacket.tryMerge(box);
                    }
                });
            }
            options.add(new RightClickOption("", options.size(), us)
            {
                @Override
                public void onAction(Screen screen)
                {
                    // NO-OP, this is a gap between unclaim
                }
            });
            options.add(new RightClickOption("thutessentials.xaeros.unclaim", options.size(), us)
            {
                @Override
                public void onAction(Screen screen)
                {
                    int yMin = level.getMinBuildHeight();
                    int yMax = level.getMaxBuildHeight();
                    BoundingBox box = new BoundingBox(xMin, yMin, zMin, xMax, yMax, zMax);
                    ClaimSyncPacket.tryUnclaim(box);
                }
            });
        }
    }
}
