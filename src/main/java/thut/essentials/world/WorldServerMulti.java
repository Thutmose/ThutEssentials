package thut.essentials.world;

import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;

public class WorldServerMulti extends WorldServer
{

    public WorldServerMulti(MinecraftServer server, ISaveHandler saveHandlerIn, WorldInfo info, int dimensionId,
            Profiler profilerIn)
    {
        super(server, saveHandlerIn, info, dimensionId, profilerIn);
        init();
    }

}
