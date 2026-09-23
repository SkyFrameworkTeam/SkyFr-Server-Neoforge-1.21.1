package com.skyframework.islandcore.mixin;

import com.skyframework.islandcore.protection.FireProtectionListener;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LightningBolt;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightningBolt.class)
public class LightningFireMixin {

	// Yarn's getAffectedBlockPos() is Mojmap's private getStrikePosition() (the block the bolt strikes:
	// position() nudged 1e-6 down; confirmed against the 1.21.1 LightningBolt sources). Mirrors the
	// Fabric mixin exactly by checking that same position, even though vanilla's own spawnFire(int)
	// places the fire at this.blockPosition() — a difference of at most one block, only when the bolt
	// lands exactly on a block face.
	@Shadow
	private BlockPos getStrikePosition() {
		throw new AssertionError();
	}

	@Inject(method = "spawnFire", at = @At("HEAD"), cancellable = true)
	private void islandcore$blockLightningFire(int count, CallbackInfo ci) {
		LightningBolt self = (LightningBolt) (Object) this;
		if (!FireProtectionListener.isFireSpreadAllowed(self.level(), getStrikePosition())) {
			ci.cancel();
		}
	}
}
