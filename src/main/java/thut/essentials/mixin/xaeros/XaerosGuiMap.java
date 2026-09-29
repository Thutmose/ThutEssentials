package thut.essentials.mixin.xaeros;

import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.gui.GuiMap;
import xaero.map.gui.MapTileSelection;
import xaero.map.gui.dropdown.rightclick.RightClickOption;

import java.util.ArrayList;

@Mixin(GuiMap.class)
public class XaerosGuiMap
{
    @Shadow
    private MapTileSelection mapTileSelection;

    @Inject(method = "getRightClickOptions", at = @At(value = "RETURN"), cancellable = true)
    public void thutessentials$getRightClickOptions(CallbackInfoReturnable<ArrayList<RightClickOption>> cir)
    {
        Object thisObj = this;
        GuiMap us = (GuiMap) thisObj;
        var options = cir.getReturnValue();
        RightClickOption option = new RightClickOption("Test", options.size(), us)
        {
            @Override
            public void onAction(Screen screen)
            {
                System.out.println("TEST " + mapTileSelection);
            }
        };
        options.add(option);
        cir.setReturnValue(options);
    }
}
