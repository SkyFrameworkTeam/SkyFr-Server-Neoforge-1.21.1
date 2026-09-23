package com.skyframework.islandcore.protection;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

// Called from mixin/PistonHandlerMixin. Kept as a plain class (rather than logic inline in the
// mixin) so the actual protection rule stays readable and separate from injected bytecode — same
// split ExplosionProtectionListener/ExplosionMixin already use.
public final class PistonProtectionListener {

	private static final ResourceKey<Level> ISLANDS_DIMENSION =
			ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("islandcore", "islands"));

	private PistonProtectionListener() {
	}

	// posFrom is the piston block's OWN position, which decides which island's bounds apply — one
	// shared island for the whole push, not a per-block lookup, since the pushed blocks don't have
	// islands of their own to compare against, only a single final destination each has to stay
	// inside. A piston built inside a claimed island's bounds could otherwise use its extension
	// reach to place blocks past that island's own plot edge (unclaimed territory, another
	// island's plot, or the void) even though the piston itself sits on legitimately protected
	// ground — see IslandCommand's exploit report. Outside islandcore:islands, or when the piston
	// itself isn't on any claimed island, this is always vanilla, nothing to protect.
	public static boolean isPushBlocked(Level world, BlockPos posFrom, Direction motionDirection, List<BlockPos> movedBlocks) {
		if (!world.dimension().equals(ISLANDS_DIMENSION)) {
			return false;
		}

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandAt(posFrom);
		if (maybeIsland.isEmpty()) {
			return false;
		}

		Island island = maybeIsland.get();
		for (BlockPos moved : movedBlocks) {
			if (!island.getBounds().contains(moved.relative(motionDirection))) {
				return true;
			}
		}
		return false;
	}
}
