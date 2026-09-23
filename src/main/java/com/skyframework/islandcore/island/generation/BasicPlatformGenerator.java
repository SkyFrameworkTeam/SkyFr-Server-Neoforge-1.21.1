package com.skyframework.islandcore.island.generation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

// TODO: Placeholder temporal — sustituir por sistema de plantillas configurables por IslandType en sprint futuro.
public class BasicPlatformGenerator {

	// Layer 0 (top) = grass_block, layers 1-3 = dirt, layers 4-10 = stone, layer 11 (bottom) =
	// deepslate — see fillStateForLayer. Raised from the original DEPTH=5 (grass/dirt×3/stone) so
	// the platform reads as a real chunk of terrain instead of a thin slab; grass/dirt keep their
	// original thickness unchanged, stone now fills the extra bulk in between (same material it
	// already used, just more of it), and the new deepslate layer at the very bottom is the only
	// actual addition — a visually distinct base so digging all the way down has a clear "you've
	// hit the bottom" marker. The smooth_stone edge marker is applied on every layer, not just the
	// top, so the plot's protected boundary is visible while digging down too, not only on the
	// surface.
	private static final int DEPTH = 12;

	// Unchanged from before the DEPTH increase — see fillStateForLayer.
	private static final int DIRT_LAYERS = 3;

	public void generate(ServerLevel world, BlockPos center, int size) {
		SquareRange range = squareRange(center, size);

		for (int layer = 0; layer < DEPTH; layer++) {
			int y = center.getY() - 1 - layer;
			BlockState fillState = fillStateForLayer(layer);

			for (int x = range.minX(); x <= range.maxX(); x++) {
				for (int z = range.minZ(); z <= range.maxZ(); z++) {
					boolean isEdge = x == range.minX() || x == range.maxX() || z == range.minZ() || z == range.maxZ();
					BlockState blockState = isEdge ? Blocks.SMOOTH_STONE.defaultBlockState() : fillState;
					world.setBlockAndUpdate(new BlockPos(x, y, z), blockState);
				}
			}
		}
	}

	// Assumes newSize > oldSize (expansion only), never called for a reduction: that validation
	// belongs to the service layer (IslandRegistryImpl.resizeIsland), not to this method.
	public void generateExpansion(ServerLevel world, BlockPos center, int oldSize, int newSize) {
		SquareRange oldRange = squareRange(center, oldSize);
		SquareRange newRange = squareRange(center, newSize);

		for (int layer = 0; layer < DEPTH; layer++) {
			int y = center.getY() - 1 - layer;
			BlockState fillState = fillStateForLayer(layer);

			for (int x = newRange.minX(); x <= newRange.maxX(); x++) {
				for (int z = newRange.minZ(); z <= newRange.maxZ(); z++) {
					if (oldRange.contains(x, z)) {
						// Already part of the built island: leave it untouched.
						continue;
					}

					boolean isEdge = x == newRange.minX() || x == newRange.maxX() || z == newRange.minZ() || z == newRange.maxZ();
					BlockState blockState = isEdge ? Blocks.SMOOTH_STONE.defaultBlockState() : fillState;
					world.setBlockAndUpdate(new BlockPos(x, y, z), blockState);
				}
			}

			convertOldEdge(world, oldRange, y, fillState);
		}
	}

	// layer 0 = top surface (grass_block, matching the interior fill players actually see and
	// walk on), 1..DIRT_LAYERS = dirt, DIRT_LAYERS+1..DEPTH-2 = stone, DEPTH-1 (bottom) = deepslate.
	private static BlockState fillStateForLayer(int layer) {
		if (layer == 0) {
			return Blocks.GRASS_BLOCK.defaultBlockState();
		}
		if (layer == DEPTH - 1) {
			return Blocks.DEEPSLATE.defaultBlockState();
		}
		if (layer <= DIRT_LAYERS) {
			return Blocks.DIRT.defaultBlockState();
		}
		return Blocks.STONE.defaultBlockState();
	}

	// The old outer perimeter (oldSize) stopped being the island's edge once it grew, so it should
	// read as this layer's own fill material like the rest of the interior — but only where the
	// player hasn't already built over it; any block other than the original smooth_stone is left
	// untouched. Called once per layer with that layer's own fillState (grass_block on top, dirt in
	// the middle, stone at the bottom), so the converted block always matches its depth.
	private static void convertOldEdge(ServerLevel world, SquareRange oldRange, int y, BlockState fillState) {
		for (int x = oldRange.minX(); x <= oldRange.maxX(); x++) {
			for (int z = oldRange.minZ(); z <= oldRange.maxZ(); z++) {
				boolean wasEdge = x == oldRange.minX() || x == oldRange.maxX() || z == oldRange.minZ() || z == oldRange.maxZ();
				if (!wasEdge) {
					continue;
				}

				BlockPos pos = new BlockPos(x, y, z);
				if (world.getBlockState(pos).getBlock() == Blocks.SMOOTH_STONE) {
					world.setBlockAndUpdate(pos, fillState);
				}
			}
		}
	}

	private static SquareRange squareRange(BlockPos center, int size) {
		int half = size / 2;
		return new SquareRange(center.getX() - half, center.getX() + half, center.getZ() - half, center.getZ() + half);
	}

	private record SquareRange(int minX, int maxX, int minZ, int maxZ) {
		boolean contains(int x, int z) {
			return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
		}
	}
}
