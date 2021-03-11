package thut.essentials.mixin;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.profiler.IProfiler;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.ISpawnWorldInfo;
import thut.essentials.Essentials;

@Mixin(ServerWorld.class)
public abstract class SeededServerWorld extends World
{
    protected SeededServerWorld(final ISpawnWorldInfo worldInfo, final RegistryKey<World> dimension,
            final DimensionType dimensionType, final Supplier<IProfiler> profiler, final boolean isRemote,
            final boolean isDebug, final long seed)
    {
        super(worldInfo, dimension, dimensionType, profiler, isRemote, isDebug, seed);
    }

    @Inject(method = "getSeed", at = @At(value = "RETURN"), cancellable = true)
    private void checkSeed(final CallbackInfoReturnable<Long> cir)
    {
        if (Essentials.config.versioned_dim_seed_map.containsKey(this.dimension().location()))
        {
            final Long seed = Essentials.config.versioned_dim_seed_map.get(this.dimension().location());
            cir.setReturnValue(seed);
        }
    }

}
