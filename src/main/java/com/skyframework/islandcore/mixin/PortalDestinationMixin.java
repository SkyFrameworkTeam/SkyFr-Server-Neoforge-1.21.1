package com.skyframework.islandcore.mixin;

import com.skyframework.islandcore.IslandCoreMod;

import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

// NetherPortalBlock.getPortalDestination hardcodes its destination as a ternary (confirmed by
// decompiling the 1.21.1 game jar): level.dimension() == NETHER ? OVERWORLD : NETHER, with zero
// concept of "the dimension this portal was originally linked from". This redirects only the
// MinecraftServer.getLevel(...) call made right after that ternary, substituting our own
// configured destination when one exists for the CURRENT (origin) dimension — everything else in
// the method (world-null check, exit-portal search/creation, coordinate-scale handling) runs
// completely untouched, using whatever world we hand back here.
//
// @Redirect (not @ModifyArg) because only @Redirect lets the handler also receive the enclosing
// method's own parameters (level/entity/pos) as trailing arguments in plain Mixin, without needing
// the MixinExtras @Local sugar.
@Mixin(NetherPortalBlock.class)
public class PortalDestinationMixin {

	@Redirect(
			method = "getPortalDestination",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/server/MinecraftServer;getLevel(Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/server/level/ServerLevel;"))
	private ServerLevel islandcore$redirectPortalDestination(
			MinecraftServer server, ResourceKey<Level> originalKey, ServerLevel level, Entity entity, BlockPos pos) {
		Optional<ResourceLocation> configured = IslandCoreMod.PORTAL_LINK_CONFIG.getDestination(level.dimension().location());
		if (configured.isPresent()) {
			return server.getLevel(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, configured.get()));
		}
		return server.getLevel(originalKey);
	}
}
