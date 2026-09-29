package thut.essentials.compat.xaeros;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import thut.essentials.Essentials;
import thut.essentials.land.claims.ClaimSyncPacket;

public class XaeroCompat
{
    public static void init()
    {
        if (ModList.get().isLoaded("xaeroworldmap") && FMLLoader.getDist() != Dist.DEDICATED_SERVER) ClaimSync.init();
        // Regardless register the sync packet
        Essentials.packets.registerBiDirectionalMessage(ClaimSyncPacket.class);
    }
}
