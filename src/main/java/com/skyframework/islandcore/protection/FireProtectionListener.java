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

// Called from mixin/FireSpreadMixin and mixin/LightningFireMixin.
public final class FireProtectionListener {

	private static final ResourceKey<Level> ISLANDS_DIMENSION =
			ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("islandcore", "islands"));

	private FireProtectionListener() {
	}

	public static boolean isFireSpreadAllowed(Level world, BlockPos pos) {
		if (!world.dimension().equals(ISLANDS_DIMENSION)) {
			return true;
		}

		// Inside the islands dimension, no island claims pos: nothing to protect there,
		// behaves like vanilla (allowed).
		Optional<Island> island = IslandCoreMod.ISLAND_REGISTRY.getIslandAt(pos);
		return island.map(value -> FlagResolver.resolveGlobal(value, FlagRegistry.FIRE_SPREAD)).orElse(true);
	}
}
