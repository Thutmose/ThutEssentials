package thut.essentials.mixin;

import java.io.File;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.datafixers.DataFixer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.ChunkStorage;
import net.minecraft.server.level.ChunkMap;
import thut.essentials.commands.structures.Structuregen;

@Mixin(ChunkMap.class)
public abstract class MixinChunkManager extends ChunkStorage
{

    public MixinChunkManager(final File p_i231889_1_, final DataFixer p_i231889_2_, final boolean p_i231889_3_)
    {
        super(p_i231889_1_, p_i231889_2_, p_i231889_3_);
    }

    @Inject(method = "read", at = @At(value = "RETURN"), cancellable = true)
    public void onLoadChunkData(final ChunkPos pos, final CallbackInfoReturnable<CompoundTag> cir)
    {
        if (Structuregen.toReset.remove(pos)) cir.setReturnValue(null);
    }
}
