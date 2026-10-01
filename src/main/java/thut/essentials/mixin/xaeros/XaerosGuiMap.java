package thut.essentials.mixin.xaeros;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thut.essentials.compat.xaeros.ClaimSync;
import xaero.map.gui.GuiMap;
import xaero.map.gui.MapTileSelection;
import xaero.map.gui.dropdown.rightclick.RightClickOption;

import java.util.ArrayList;

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
        Object thisObj = this;
        GuiMap us = (GuiMap) thisObj;
        var options = cir.getReturnValue();
        ClaimSync.doClaim(mapTileSelection, rightClickDim, us, options);
        cir.setReturnValue(options);
    }
}
