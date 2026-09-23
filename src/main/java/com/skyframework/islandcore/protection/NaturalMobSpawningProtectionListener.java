package com.skyframework.islandcore.protection;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.protection.flag.FlagRegistry;
import com.skyframework.islandcore.protection.flag.FlagResolver;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.Optional;

// Called from mixin/SpawnHelperMixin, scoped there to MobCategory.MONSTER (hostile) spawns only —
// see that mixin's comment for why. DENY (the default) blocks the natural spawn for the island at
// pos; outside a claimed island — or outside islandcore:islands entirely — is always vanilla.
public final class NaturalMobSpawningProtectionListener {

	private static final ResourceKey<Level> ISLANDS_DIMENSION =
			ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("islandcore", "islands"));

	private NaturalMobSpawningProtectionListener() {
	}

	public static boolean isSpawnBlocked(ServerLevel world, BlockPos pos) {
		if (!world.dimension().equals(ISLANDS_DIMENSION)) {
			return false;
		}

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandAt(pos);
		if (maybeIsland.isEmpty()) {
			return false;
		}

		return !FlagResolver.resolveGlobal(maybeIsland.get(), FlagRegistry.NATURAL_MOB_SPAWNING);
	}
}
