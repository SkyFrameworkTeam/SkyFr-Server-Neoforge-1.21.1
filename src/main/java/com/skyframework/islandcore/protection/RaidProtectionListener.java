package com.skyframework.islandcore.protection;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.protection.flag.FlagRegistry;
import com.skyframework.islandcore.protection.flag.FlagResolver;

import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Optional;

// Called from mixin/RaidManagerMixin. DENY (the default) blocks a raid from starting for the
// island at pos; outside a claimed island — or outside islandcore:islands entirely — is always
// vanilla. Raids don't naturally occur in islandcore:islands (a flat void dimension with no
// generated villages), but a player-built village-like structure there is still possible, so this
// is checked regardless rather than assumed unreachable.
public final class RaidProtectionListener {

	private static final ResourceKey<Level> ISLANDS_DIMENSION =
			ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("islandcore", "islands"));

	private RaidProtectionListener() {
	}

	public static boolean isRaidBlocked(ServerLevel world, BlockPos pos) {
		if (!world.dimension().equals(ISLANDS_DIMENSION)) {
			return false;
		}

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandAt(pos);
		if (maybeIsland.isEmpty()) {
			return false;
		}

		return !FlagResolver.resolveGlobal(maybeIsland.get(), FlagRegistry.RAIDS);
	}
}
