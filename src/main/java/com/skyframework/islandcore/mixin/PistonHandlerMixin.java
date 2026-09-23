package com.skyframework.islandcore.mixin;

import com.skyframework.islandcore.protection.PistonProtectionListener;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonStructureResolver;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// NEOFORGE PORT: kept as a Mixin rather than a NeoForge event, same as the Fabric original. The only
// NeoForge piston event (PistonEvent.Pre) fires from PistonBaseBlock#triggerEvent before the structure
// is even resolved, so it can't see the resolved push list this rule needs, and it isn't reached by
// the checkIfExtend()/canPush() pre-checks that also call PistonStructureResolver#resolve. Injecting
// at the exact point vanilla itself decides whether the push succeeds (resolve's own return value),
// before PistonBaseBlock#moveBlocks ever applies toPush/toDestroy to the world, keeps the behavior
// identical to Fabric's PistonHandlerMixin (PistonHandler#calculatePush there).
@Mixin(PistonStructureResolver.class)
public class PistonHandlerMixin {

	@Shadow
	@Final
	private Level level;

	@Shadow
	@Final
	private BlockPos startPos;

	@Shadow
	@Final
	private Direction pushDirection;

	@Inject(method = "resolve", at = @At("RETURN"), cancellable = true)
	private void islandcore$blockOutOfBoundsPush(CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ()) {
			// Vanilla already refused the push on its own (blocked/too many blocks/etc.) — nothing
			// for island protection to add.
			return;
		}

		PistonStructureResolver self = (PistonStructureResolver) (Object) this;
		if (PistonProtectionListener.isPushBlocked(level, startPos, pushDirection, self.getToPush())) {
			cir.setReturnValue(false);
		}
	}
}
