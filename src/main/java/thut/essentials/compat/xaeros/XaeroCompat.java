package thut.essentials.compat.xaeros;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;

public class XaeroCompat
{
    public static void init()
    {
        if (ModList.get().isLoaded("xaeroworldmap") && FMLLoader.getDist() != Dist.DEDICATED_SERVER) ClaimSync.init();
    }
}
