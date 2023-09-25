package thut.essentials.mixin;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import thut.essentials.Essentials;

@Mixin(ServerLevel.class)
public abstract class SeededServerWorld extends Level
{
    protected SeededServerWorld(WritableLevelData p_270739_, ResourceKey<Level> p_270683_, RegistryAccess p_270200_,
            Holder<DimensionType> p_270240_, Supplier<ProfilerFiller> p_270692_, boolean p_270904_, boolean p_270470_,
            long p_270248_, int p_270466_)
    {
        super(p_270739_, p_270683_, p_270200_, p_270240_, p_270692_, p_270904_, p_270470_, p_270248_, p_270466_);
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
