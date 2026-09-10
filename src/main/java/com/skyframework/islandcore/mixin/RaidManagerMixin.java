package com.skyframework.islandcore.mixin;

import com.skyframework.islandcore.protection.RaidProtectionListener;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raids;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// No NeoForge/Fabric API event exists for cancelling a raid start in this version (same absence
// confirmed for explosions/crop trample/natural spawning). Raids#createOrExtendRaid is the exact
// trigger point (called from RaidOmenStatusEffect when a player with the Raid Omen effect stands
// in a village) — confirmed by bytecode that its only real-world caller discards the returned Raid
// entirely, so cancelling here and returning null is safe — nothing ever dereferences it.
@Mixin(Raids.class)
public class RaidManagerMixin {

	@Shadow
	@Final
	private ServerLevel level;

	@Inject(method = "createOrExtendRaid", at = @At("HEAD"), cancellable = true)
	private void islandcore$blockRaidStart(ServerPlayer player, BlockPos pos, CallbackInfoReturnable<Raid> cir) {
		if (RaidProtectionListener.isRaidBlocked(level, pos)) {
			cir.setReturnValue(null);
		}
	}
}
