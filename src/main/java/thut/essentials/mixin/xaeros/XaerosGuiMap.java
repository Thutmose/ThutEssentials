package thut.essentials.mixin.xaeros;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thut.essentials.land.claims.ClaimSyncPacket;
import thut.essentials.land.claims.ClaimedVolume;
import thut.essentials.land.claims.StructureManager;
import xaero.map.gui.GuiMap;
import xaero.map.gui.MapTileSelection;
import xaero.map.gui.dropdown.rightclick.RightClickOption;

import java.util.ArrayList;
import java.util.List;

@Mixin(GuiMap.class)
public class XaerosGuiMap
{
    @Shadow
    private MapTileSelection mapTileSelection;
    @Shadow
    private ResourceKey<Level> rightClickDim;

    @Inject(method = "getRightClickOptions", at = @At(value = "RETURN"), cancellable = true)
    public void thutessentials$getRightClickOptions(CallbackInfoReturnable<ArrayList<RightClickOption>> cir)
    {
        var level = Minecraft.getInstance().level;
        if (rightClickDim != level.dimension()) return;
        Object thisObj = this;
        GuiMap us = (GuiMap) thisObj;
        var options = cir.getReturnValue();
        List<ChunkPos> toClaim = new ArrayList<>();
        List<ChunkPos> toUnclaim = new ArrayList<>();

        int xMin = SectionPos.sectionToBlockCoord(mapTileSelection.getStartX());
        int xMax = SectionPos.sectionToBlockCoord(mapTileSelection.getEndX() + 1);
        int zMin = SectionPos.sectionToBlockCoord(mapTileSelection.getStartZ());
        int zMax = SectionPos.sectionToBlockCoord(mapTileSelection.getEndZ() + 1);
        int yTopHere = SectionPos.sectionToBlockCoord(
                SectionPos.blockToSectionCoord(Minecraft.getInstance().player.getBlockY())) + 16;

        for (int x = mapTileSelection.getStartX(); x <= mapTileSelection.getEndX(); x++)
        {
            for (int z = mapTileSelection.getStartZ(); z <= mapTileSelection.getEndZ(); z++)
            {
                ChunkPos pos = new ChunkPos(x, z);
                var found = StructureManager.getFor(rightClickDim, pos);
                if (!found.isEmpty())
                {
                    int finalX = pos.getMiddleBlockX();
                    int finalZ = pos.getMiddleBlockZ();
                    found.removeIf(v -> {
                        var bounds = v.getTotalBounds();
                        return finalZ < bounds.minZ() || finalZ > bounds.maxZ() || finalX < bounds.minX()
                                || finalX > bounds.maxX();
                    });
                }
                if (found.isEmpty()) toClaim.add(pos);
                else toUnclaim.add(pos);
            }
        }
        boolean alt = Screen.hasAltDown();
        boolean ctrl = Screen.hasControlDown();
        if (!toClaim.isEmpty())
        {
            if (alt && ctrl) options.add(new RightClickOption("thutessentials.xaeros.claim.here", options.size(), us)
            {
                @Override
                public void onAction(Screen screen)
                {
                    int yMin = yTopHere - 16;
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
        if (!toUnclaim.isEmpty()) options.add(new RightClickOption("thutessentials.xaeros.unclaim", options.size(), us)
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
        cir.setReturnValue(options);
    }
}
