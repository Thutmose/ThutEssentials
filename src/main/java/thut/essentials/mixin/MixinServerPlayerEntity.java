package thut.essentials.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.authlib.GameProfile;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import thut.essentials.util.PlayerDataHandler;

@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayerEntity extends PlayerEntity
{

    public MixinServerPlayerEntity(final World p_i241920_1_, final BlockPos p_i241920_2_, final float p_i241920_3_,
            final GameProfile p_i241920_4_)
    {
        super(p_i241920_1_, p_i241920_2_, p_i241920_3_, p_i241920_4_);
    }

    @Inject(method = "getTabListDisplayName", at = @At(value = "RETURN"), cancellable = true)
    private void onGetTabListDisplayName(final CallbackInfoReturnable<ITextComponent> cir)
    {
        final CompoundNBT tag = PlayerDataHandler.getCustomDataTag(this);
        if (tag.contains("nick")) cir.setReturnValue(new StringTextComponent(tag.getString("nick")));
    }
}
