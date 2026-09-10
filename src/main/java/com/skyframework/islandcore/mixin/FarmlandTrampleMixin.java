package com.skyframework.islandcore.mixin;

import com.skyframework.islandcore.protection.CropTrampleProtectionListener;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// No NeoForge/Fabric API event exists for intercepting farmland trample in this version (same
// absence confirmed for explosions — see ExplosionMixin). FarmBlock#fallOn does two independent
// things in sequence: maybe convert this block to dirt (the trample), then unconditionally call
// super.fallOn(...) to apply fall damage — an @Inject cancelling the whole method would also
// suppress fall damage, which crop_trample has nothing to do with. @Redirect on just the
// turnToDirt(...) call instead leaves fall damage untouched and only ever skips the dirt
// conversion.
@Mixin(FarmBlock.class)
public class FarmlandTrampleMixin {

	@Redirect(method = "fallOn", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/FarmBlock;turnToDirt(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"))
	private static void islandcore$maybeTurnToDirt(Entity entity, BlockState state, Level world, BlockPos pos) {
		if (CropTrampleProtectionListener.isTrampleBlocked(world, pos)) {
			return;
		}
		FarmBlock.turnToDirt(entity, state, world, pos);
	}
}
