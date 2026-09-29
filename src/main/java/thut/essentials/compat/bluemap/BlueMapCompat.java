package thut.essentials.compat.bluemap;

import net.neoforged.fml.ModList;

public class BlueMapCompat
{
    public static void init()
    {
        if (ModList.get().isLoaded("bluemap")) ClaimSync.init();
    }
}
