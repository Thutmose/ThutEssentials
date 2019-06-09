package thut.essentials.world;

import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.ServerWorld;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;

public class ServerWorldMulti extends ServerWorld
{

    public ServerWorldMulti(MinecraftServer server, ISaveHandler saveHandlerIn, WorldInfo info, int dimensionId,
            Profiler profilerIn)
    {
        super(server, saveHandlerIn, info, dimensionId, profilerIn);
        init();
    }

}
