package com.skyframework.islandcore.protection;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

import java.util.UUID;

public interface AccessController {

	boolean canBreak(UUID playerUuid, ServerLevel world, BlockPos pos);

	boolean canPlace(UUID playerUuid, ServerLevel world, BlockPos pos);

	boolean canInteractBlock(UUID playerUuid, ServerLevel world, BlockPos pos);

	boolean canOpenContainer(UUID playerUuid, ServerLevel world, BlockPos pos);

	boolean canInteractEntity(UUID playerUuid, ServerLevel world, Entity entity);

	boolean canAttackEntity(UUID playerUuid, ServerLevel world, Entity entity);
}
