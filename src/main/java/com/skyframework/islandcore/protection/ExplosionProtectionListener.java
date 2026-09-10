package com.skyframework.islandcore.protection;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.protection.flag.FlagRegistry;
import com.skyframework.islandcore.protection.flag.FlagResolver;

import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Optional;

// Called from mixin/ExplosionMixin. Kept as a plain class (rather than logic inline in the
// mixin) so the actual protection rule stays readable and separate from injected bytecode.
public final class ExplosionProtectionListener {

	private static final ResourceKey<Level> ISLANDS_DIMENSION =
			ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("islandcore", "islands"));

	private ExplosionProtectionListener() {
	}

	// Terrain destruction only: entity damage from the same explosion is untouched (future sprint).
	// pos is the explosion's own origin (Explosion#center, floored) — one flag decision per
	// explosion, not per affected block, since a single blast only has one origin and deciding per
	// block would let an explosion straddling an island's edge apply only partially, which reads as
	// a bug more than a feature. Outside a claimed island — including anywhere outside
	// islandcore:islands, though this is only ever invoked from there — is always vanilla, nothing
	// to protect.
	public static boolean isTerrainDamageBlocked(Level world, BlockPos pos) {
		if (!world.dimension().equals(ISLANDS_DIMENSION)) {
			return false;
		}

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandAt(pos);
		if (maybeIsland.isEmpty()) {
			return false;
		}

		return !FlagResolver.resolveGlobal(maybeIsland.get(), FlagRegistry.EXPLOSION_DAMAGE);
	}
}
