package com.skyframework.islandcore.teleport;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.Set;

// Shared "is this position safe to land a player on" criteria, extracted so
// SafeRandomTeleportFinder (rtp) and any other teleport destination (sethome, void rescue,
// /island home) all agree on exactly the same definition instead of duplicating it.
public final class SafeLandingChecker {

	// Solid-looking or seemingly-empty blocks that are still dangerous to land on/in.
	private static final Set<Block> UNSAFE_BLOCKS = Set.of(
			Blocks.LAVA,
			Blocks.FIRE,
			Blocks.SOUL_FIRE,
			Blocks.CACTUS,
			Blocks.MAGMA_BLOCK,
			Blocks.WATER,
			Blocks.POWDER_SNOW
	);

	private SafeLandingChecker() {
	}

	// feet is the position the player's feet would occupy after teleporting.
	public static boolean isSafe(ServerLevel world, BlockPos feet) {
		BlockPos ground = feet.below();
		BlockState groundState = world.getBlockState(ground);
		if (UNSAFE_BLOCKS.contains(groundState.getBlock()) || !groundState.isFaceSturdy(world, ground, Direction.UP)) {
			return false;
		}

		return isFreeAndSafe(world, feet) && isFreeAndSafe(world, feet.above());
	}

	private static boolean isFreeAndSafe(ServerLevel world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		if (UNSAFE_BLOCKS.contains(state.getBlock())) {
			return false;
		}
		return state.isAir() || state.getCollisionShape(world, pos).isEmpty();
	}
}
