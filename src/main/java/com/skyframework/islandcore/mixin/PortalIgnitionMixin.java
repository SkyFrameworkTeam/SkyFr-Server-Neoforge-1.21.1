package com.skyframework.islandcore.mixin;

import com.skyframework.islandcore.IslandCoreMod;

import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Vanilla only allows lighting a Nether portal in exactly minecraft:overworld or
// minecraft:the_nether: BaseFireBlock.inPortalDimension(Level) is a hardcoded dimension identity
// check, unrelated to DimensionType/"natural" (confirmed by decompiling this method in the 1.21.1
// game jar: it does NOT read any dimension_type field at all). This widens that gate to also allow
// any dimension configured in portal/PortalLinkConfig, without touching the rest of the
// frame-detection logic (shouldLightPortalAt) that calls it.
@Mixin(BaseFireBlock.class)
public class PortalIgnitionMixin {

	@Inject(method = "inPortalDimension", at = @At("RETURN"), cancellable = true)
	private static void islandcore$allowConfiguredPortals(Level level, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValue() && IslandCoreMod.PORTAL_LINK_CONFIG.isPortalAllowed(level.dimension().location())) {
			cir.setReturnValue(true);
		}
	}
}
