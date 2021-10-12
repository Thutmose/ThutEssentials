package thut.essentials.mixin;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.WritableLevelData;
import thut.essentials.Essentials;

@Mixin(ServerLevel.class)
public abstract class SeededServerWorld extends Level
{
    protected SeededServerWorld(final WritableLevelData worldInfo, final ResourceKey<Level> dimension,
            final DimensionType dimensionType, final Supplier<ProfilerFiller> profiler, final boolean isRemote,
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
