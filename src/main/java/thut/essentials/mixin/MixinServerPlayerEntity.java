package thut.essentials.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import thut.essentials.util.PlayerDataHandler;

@Mixin(ServerPlayer.class)
public abstract class MixinServerPlayerEntity extends Player
{

    public MixinServerPlayerEntity(final Level p_i241920_1_, final BlockPos p_i241920_2_, final float p_i241920_3_,
            final GameProfile p_i241920_4_)
    {
        super(p_i241920_1_, p_i241920_2_, p_i241920_3_, p_i241920_4_);
    }

    @Inject(method = "getTabListDisplayName", at = @At(value = "RETURN"), cancellable = true)
    private void onGetTabListDisplayName(final CallbackInfoReturnable<Component> cir)
    {
        final CompoundTag tag = PlayerDataHandler.getCustomDataTag(this);
        if (tag.contains("nick")) cir.setReturnValue(new TextComponent(tag.getString("nick")));
    }
}
