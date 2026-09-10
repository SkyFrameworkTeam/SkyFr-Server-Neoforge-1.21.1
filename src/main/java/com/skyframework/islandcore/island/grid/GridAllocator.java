package com.skyframework.islandcore.island.grid;

import net.minecraft.core.BlockPos;

public interface GridAllocator {

	int CELL_SIZE = 200;

	GridCoordinate allocateNextSlot();

	void releaseSlot(GridCoordinate coordinate);

	// Marks a coordinate as OCCUPIED directly, without searching for a free slot.
	// Used to restore allocator state from persisted islands on startup.
	void occupySlot(GridCoordinate coordinate);

	BlockPos gridToWorldPos(GridCoordinate coordinate);
}
