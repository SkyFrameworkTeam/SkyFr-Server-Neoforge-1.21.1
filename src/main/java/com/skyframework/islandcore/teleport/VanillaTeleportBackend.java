package com.skyframework.islandcore.teleport;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

import java.util.Set;

public class VanillaTeleportBackend implements TeleportBackend {

	@Override
	public boolean teleport(ServerPlayer player, ServerLevel world, BlockPos pos) {
		// Empty flag set: all coordinates are absolute. Keep the player's current
		// yaw/pitch (no forced camera reset).
		return player.teleportTo(
				world,
				pos.getX() + 0.5,
				pos.getY(),
				pos.getZ() + 0.5,
				Set.of(),
				player.getYRot(),
				player.getXRot()
		);
	}
}
