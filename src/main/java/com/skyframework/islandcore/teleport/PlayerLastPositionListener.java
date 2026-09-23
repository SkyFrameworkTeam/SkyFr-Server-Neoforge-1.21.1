package com.skyframework.islandcore.teleport;

import com.skyframework.islandcore.IslandCoreMod;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

// NEOFORGE PORT of Fabric's mixin/PlayerLastPositionMixin, as an event listener instead of a Mixin.
// Fabric hooked ServerPlayerEntity#teleportTo(TeleportTarget) at HEAD; on Mojang mappings that method
// is ServerPlayer#changeDimension(DimensionTransition), whose very first statement (verified in the
// neoforge-21.1.250 sources) is CommonHooks.onTravelToDimension(...), which posts
// EntityTravelToDimensionEvent. So this listener runs at exactly the same moment the Fabric mixin did:
// before anything about the teleport has been applied, when position()/serverLevel() still describe the
// dimension being LEFT. It is the single choke point every cross-dimension player teleport funnels
// through (portals, respawn, and VanillaTeleportBackend via ServerPlayer#teleportTo(level, ...)).
//
// Recorded by this listener right before a player leaves a dimension, and consulted by
// TeleportManagerImpl#requestDimensionTeleport — see PlayerLastPositionStore.
public final class PlayerLastPositionListener {

	private PlayerLastPositionListener() {
	}

	public static void register() {
		NeoForge.EVENT_BUS.addListener(PlayerLastPositionListener::onTravelToDimension);
	}

	private static void onTravelToDimension(EntityTravelToDimensionEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer self) || self.isRemoved()) {
			return;
		}

		ServerLevel currentWorld = self.serverLevel();
		if (event.getDimension() == currentWorld.dimension()) {
			// Same-dimension teleport: nothing to record, this player never "left" a dimension.
			return;
		}

		IslandCoreMod.PLAYER_LAST_POSITION_STORE.recordLastPosition(
				self.getUUID(), currentWorld.dimension().location(), self.blockPosition());
	}
}
