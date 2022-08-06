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
    protected SeededServerWorld(WritableLevelData p_204149_, ResourceKey<Level> p_204150_,
            Holder<DimensionType> p_204151_, Supplier<ProfilerFiller> p_204152_, boolean p_204153_, boolean p_204154_,
            long p_204155_)
    {
        super(p_204149_, p_204150_, p_204151_, p_204152_, p_204153_, p_204154_, p_204155_);
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
