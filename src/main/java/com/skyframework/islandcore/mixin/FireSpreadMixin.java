package com.skyframework.islandcore.mixin;

import com.skyframework.islandcore.protection.FireProtectionListener;

import net.minecraft.world.level.block.FireBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireBlock.class)
public class FireSpreadMixin {

	// checkBurnOut is called once per cardinal direction per tick (Mojmap's equivalent of the Yarn
	// source this was ported from called it trySpreadingFire, missing the extra Direction face
	// parameter Mojmap's version has); pos is the block about to be ignited. Cancelling only this
	// call blocks that one ignition, leaving the rest of fire's tick behavior (aging, burning out,
	// etc.) untouched.
	@Inject(method = "checkBurnOut", at = @At("HEAD"), cancellable = true)
	private void islandcore$blockFireSpread(
			Level level, BlockPos pos, int chance, RandomSource random, int age, Direction face, CallbackInfo ci) {
		if (!FireProtectionListener.isFireSpreadAllowed(level, pos)) {
			ci.cancel();
		}
	}

	// getIgniteOdds(LevelReader, BlockPos) is private and, confirmed by decompiling FireBlock, has
	// exactly one call site in the whole class: tick's own separate 3x3x6 ambient-ignition loop,
	// which sets nearby AIR blocks on fire directly via level.setBlock(...), entirely without ever
	// calling checkBurnOut above. Redirecting this exact overload therefore can't reach any other
	// call site — there isn't one.
	@Shadow
	private int getIgniteOdds(LevelReader level, BlockPos pos) {
		throw new AssertionError();
	}

	// Forces that loop's own "if (odds > 0)" check to skip the position entirely — exactly as if
	// nothing flammable were nearby — without touching checkBurnOut's already-correct behavior, and
	// with zero effect when firespread=true (falls through to the real ignite odds).
	@Redirect(
			method = "tick",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/block/FireBlock;getIgniteOdds(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)I"))
	private int islandcore$blockAmbientFireSpread(FireBlock instance, LevelReader level, BlockPos pos) {
		if (level instanceof Level actualLevel && !FireProtectionListener.isFireSpreadAllowed(actualLevel, pos)) {
			return 0;
		}
		return this.getIgniteOdds(level, pos);
	}
}
