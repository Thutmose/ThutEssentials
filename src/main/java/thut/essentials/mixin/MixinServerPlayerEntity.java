package thut.essentials.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import thut.essentials.util.PlayerDataHandler;

@Mixin(ServerPlayer.class)
public abstract class MixinServerPlayerEntity extends Player
{

    public MixinServerPlayerEntity(Level p_219727_, BlockPos p_219728_, float p_219729_, GameProfile p_219730_)
    {
        super(p_219727_, p_219728_, p_219729_, p_219730_);
    }

    @Inject(method = "getTabListDisplayName", at = @At(value = "RETURN"), cancellable = true)
    private void onGetTabListDisplayName(final CallbackInfoReturnable<Component> cir)
    {
        final CompoundTag tag = PlayerDataHandler.getCustomDataTag(this);
        if (tag.contains("nick")) cir.setReturnValue(Component.literal(tag.getString("nick")));
    }
}
