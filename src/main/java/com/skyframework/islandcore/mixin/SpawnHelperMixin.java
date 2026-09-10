package com.skyframework.islandcore.mixin;

import com.skyframework.islandcore.protection.NaturalMobSpawningProtectionListener;

import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.Mob;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.NaturalSpawner;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// No NeoForge/Fabric API event exists for cancelling an individual natural mob spawn attempt in
// this version (same absence confirmed for explosions/crop trample). NaturalSpawner#
// isValidPositionForMob is the last check before an already-created Mob is actually placed in the
// world, reached ONLY from the natural per-chunk spawn cycle — never from a mob spawner block or
// /summon, so this scopes cleanly to "natural" spawning as the flag name implies. Scoped further,
// here, to MobCategory.MONSTER (hostile) only — the investigation this implements was specifically
// about hostile mobs, and blocking passive animals/villagers too would be a much bigger behavior
// change than asked for.
@Mixin(NaturalSpawner.class)
public class SpawnHelperMixin {

	@Inject(method = "isValidPositionForMob", at = @At("HEAD"), cancellable = true)
	private static void islandcore$blockNaturalHostileSpawn(
			ServerLevel world, Mob entity, double squaredDistanceToClosestPlayer, CallbackInfoReturnable<Boolean> cir) {
		if (entity.getType().getCategory() != MobCategory.MONSTER) {
			return;
		}
		if (NaturalMobSpawningProtectionListener.isSpawnBlocked(world, entity.blockPosition())) {
			cir.setReturnValue(false);
		}
	}
}
