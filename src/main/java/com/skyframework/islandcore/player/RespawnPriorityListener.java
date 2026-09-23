package com.skyframework.islandcore.player;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerRespawnPositionEvent;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

// NEOFORGE PORT of Fabric's mixin/RespawnPriorityMixin, as an event listener instead of a Mixin. Fabric
// injected at RETURN of ServerPlayerEntity#getRespawnTarget; on Mojang mappings that is
// ServerPlayer#findRespawnPositionAndUseSpawnBlock(boolean, PostDimensionTransition), whose ONLY caller
// (verified in the neoforge-21.1.250 sources) is PlayerList#respawn — which immediately posts
// PlayerRespawnPositionEvent with the vanilla result and then uses whatever the event's transition is.
// Inspecting/replacing that already-computed vanilla result is therefore exactly what the Fabric mixin
// did, just through NeoForge's own hook.
//
// Vanilla's own target has NO restriction tying bed/anchor respawn to "the same dimension the player
// died in" — a valid bed/anchor in ANY dimension already works as-is; nothing here touches that path.
// What vanilla does NOT do is fall back to anything but the Overworld default spawn when there is no
// valid bed/anchor (DimensionTransition.missingRespawnBlock(...)). That fallback chain — player's own
// island home, then the Spawn island's home — is exactly the gap this fills, and only that gap: it
// never overrides a target vanilla already resolved from a real bed/anchor (missingRespawnBlock()
// false is left completely untouched).
public final class RespawnPriorityListener {

	private RespawnPriorityListener() {
	}

	public static void register() {
		NeoForge.EVENT_BUS.addListener(RespawnPriorityListener::onRespawnPosition);
	}

	private static void onRespawnPosition(PlayerRespawnPositionEvent event) {
		DimensionTransition vanillaTarget = event.getDimensionTransition();
		if (!vanillaTarget.missingRespawnBlock()) {
			// Valid bed/anchor found by vanilla (in whatever dimension it's actually in) — respect it
			// unchanged, no matter which dimension the player died in or which dimension the bed is in.
			return;
		}

		ServerPlayer self = (ServerPlayer) event.getEntity();
		DimensionTransition.PostDimensionTransition postDimensionTransition = vanillaTarget.postDimensionTransition();

		Optional<Island> ownIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(self.getUUID());
		DimensionTransition fallback = islandHomeTarget(self, ownIsland.orElse(null), postDimensionTransition);
		if (fallback == null) {
			Optional<Island> spawnIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(Island.SERVER_OWNER_UUID);
			fallback = islandHomeTarget(self, spawnIsland.orElse(null), postDimensionTransition);
		}

		if (fallback != null) {
			event.setDimensionTransition(fallback);
		}
		// Neither the player's own island nor the Spawn island has a usable home: leave vanilla's
		// own Overworld-default fallback (already in the event) untouched.
	}

	// Null if island is null, has no home location set, or its dimension isn't currently loaded —
	// in every one of those cases the caller is expected to try the next fallback (or, if this was
	// already the last one, leave vanilla's own default in place).
	private static DimensionTransition islandHomeTarget(
			ServerPlayer self, Island island, DimensionTransition.PostDimensionTransition postDimensionTransition) {
		if (island == null) {
			return null;
		}

		BlockPos home = island.getHomeLocation();
		if (home == null) {
			return null;
		}

		ServerLevel homeWorld = self.server.getLevel(island.getDimension());
		if (homeWorld == null) {
			return null;
		}

		Vec3 pos = new Vec3(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
		return new DimensionTransition(homeWorld, pos, Vec3.ZERO, self.getYRot(), 0.0f, postDimensionTransition);
	}
}
