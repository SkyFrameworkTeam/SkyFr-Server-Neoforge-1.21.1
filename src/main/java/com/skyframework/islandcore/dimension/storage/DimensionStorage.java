package com.skyframework.islandcore.dimension.storage;

import com.skyframework.islandcore.dimension.model.DimensionData;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Optional;

public interface DimensionStorage {

	void save(DimensionData dimension);

	void delete(ResourceLocation dimensionId);

	Optional<DimensionData> load(ResourceLocation dimensionId);

	Collection<DimensionData> loadAll();
}
