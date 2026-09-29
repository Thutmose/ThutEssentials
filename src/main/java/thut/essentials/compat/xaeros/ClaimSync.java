package thut.essentials.compat.xaeros;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import xaero.map.WorldMap;

public class ClaimSync
{
    public static void init()
    {
        final IEventBus modEventBus = ModLoadingContext.get().getActiveContainer().getEventBus();
        modEventBus.register(ClaimSync.class);
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onClientComplete(FMLLoadCompleteEvent event)
    {
    }

    @SubscribeEvent
    public static void serverDummy(FMLLoadCompleteEvent event)
    {
    }
}
