package com.skyframework.islandcore.mixin;

import com.skyframework.islandcore.protection.FireProtectionListener;

import net.minecraft.world.entity.LightningBolt;
import net.minecraft.core.BlockPos;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightningBolt.class)
public class LightningFireMixin {

	// Mojmap's spawnFire(int) reads the ignition point directly off this.blockPosition() (confirmed
	// by decompiling the 1.21.1 game jar) rather than through a separate accessor the way the Yarn
	// source this was ported from did — so this HEAD injection reads it the same way instead of
	// @Shadow-ing a method that doesn't exist under this name here.
	@Inject(method = "spawnFire", at = @At("HEAD"), cancellable = true)
	private void islandcore$blockLightningFire(int count, CallbackInfo ci) {
		LightningBolt self = (LightningBolt) (Object) this;
		BlockPos pos = self.blockPosition();
		if (!FireProtectionListener.isFireSpreadAllowed(self.level(), pos)) {
			ci.cancel();
		}
	}
}
