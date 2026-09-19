package dev.cs2fabric.mixin;

import dev.cs2fabric.GunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Replaces vanilla punch/block-break/use handling while a gun is equipped.
 * Gun input itself is handled by the client tick controller so this mixin only
 * suppresses the conflicting vanilla actions.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftInputMixin {
    @Shadow public LocalPlayer player;

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void cs2fabric$replaceAttack(CallbackInfoReturnable<Boolean> cir) {
        if (cs2fabric$holdingGun()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void cs2fabric$replaceContinuousAttack(boolean breaking, CallbackInfo ci) {
        if (cs2fabric$holdingGun()) {
            ci.cancel();
        }
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void cs2fabric$replaceUse(CallbackInfo ci) {
        if (cs2fabric$holdingGun()) {
            ci.cancel();
        }
    }

    private boolean cs2fabric$holdingGun() {
        return player != null && player.getMainHandItem().getItem() instanceof GunItem;
    }
}
