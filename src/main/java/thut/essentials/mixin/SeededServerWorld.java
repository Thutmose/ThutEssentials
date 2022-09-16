package thut.essentials.mixin;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.Holder;
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

    protected SeededServerWorld(WritableLevelData p_220352_, ResourceKey<Level> p_220353_,
            Holder<DimensionType> p_220354_, Supplier<ProfilerFiller> p_220355_, boolean p_220356_, boolean p_220357_,
            long p_220358_, int p_220359_)
    {
        super(p_220352_, p_220353_, p_220354_, p_220355_, p_220356_, p_220357_, p_220358_, p_220359_);
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
