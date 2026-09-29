package thut.essentials.compat.xaeros;

import net.neoforged.fml.ModList;
import thut.essentials.Essentials;
import thut.essentials.land.claims.ClaimSyncPacket;

public class XaeroCompat
{
    public static void init()
    {
        if (ModList.get().isLoaded("xaeroworldmap")) ClaimSync.init();
        // Regardless register the sync packet
        Essentials.packets.registerBiDirectionalMessage(ClaimSyncPacket.class);
    }
}
