package com.skyframework.islandcore.teleport;

import net.minecraft.resources.ResourceKey;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

import java.util.UUID;

// Internal bookkeeping for a teleport currently counting down its warmup; not part of the public api/.
class PendingTeleport {

	// Which cooldown map/completion message applies once this finishes (see TeleportManagerImpl).
	enum Kind {
		HOME,
		SPAWN,
		FARMING
	}

	final UUID playerUuid;
	final ResourceKey<Level> targetDimension;
	final BlockPos targetPos;
	final Vec3 startPosition;
	final Kind kind;
	int ticksRemaining;

	PendingTeleport(UUID playerUuid, ResourceKey<Level> targetDimension, BlockPos targetPos, Vec3 startPosition, int ticksRemaining, Kind kind) {
		this.playerUuid = playerUuid;
		this.targetDimension = targetDimension;
		this.targetPos = targetPos;
		this.startPosition = startPosition;
		this.ticksRemaining = ticksRemaining;
		this.kind = kind;
	}
}
