package thut.essentials.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.ChunkStorage;
import thut.essentials.commands.structures.Structuregen;

@Mixin(ChunkStorage.class)
public abstract class MixinChunkManager
{
    @Inject(method = "read", at = @At(value = "HEAD"), cancellable = true)
    public void onLoadChunkData(final ChunkPos pos, final CallbackInfoReturnable<CompoundTag> cir)
    {
        if (Structuregen.toReset.remove(pos)) cir.setReturnValue(null);
    }
}
