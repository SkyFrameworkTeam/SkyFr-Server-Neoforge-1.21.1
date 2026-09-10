package com.skyframework.islandcore.teleport;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

// Abstraction over how a teleport is actually performed, so a future backend
// (e.g. Warpstone) can be swapped in without touching TeleportManager.
public interface TeleportBackend {

	boolean teleport(ServerPlayer player, ServerLevel world, BlockPos pos);
}
