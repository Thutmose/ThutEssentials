package thut.essentials.mixin;

import java.io.File;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.datafixers.DataFixer;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.storage.ChunkLoader;
import net.minecraft.world.server.ChunkManager;
import thut.essentials.commands.structures.Structuregen;

@Mixin(ChunkManager.class)
public abstract class MixinChunkManager extends ChunkLoader
{

    public MixinChunkManager(final File p_i231889_1_, final DataFixer p_i231889_2_, final boolean p_i231889_3_)
    {
        super(p_i231889_1_, p_i231889_2_, p_i231889_3_);
    }

    @Inject(method = "loadChunkData", at = @At(value = "RETURN"), cancellable = true)
    public void onLoadChunkData(final ChunkPos pos, final CallbackInfoReturnable<CompoundNBT> cir)
    {
        if (Structuregen.toReset.remove(pos)) cir.setReturnValue(null);
    }
}
