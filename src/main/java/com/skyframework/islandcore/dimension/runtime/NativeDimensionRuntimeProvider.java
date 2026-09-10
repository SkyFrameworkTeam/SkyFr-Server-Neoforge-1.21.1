package com.skyframework.islandcore.dimension.runtime;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.dimension.model.DimensionDefinition;
import com.skyframework.islandcore.dimension.model.DimensionGeneratorStyle;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

// NATIVE NEOFORGE REPLACEMENT for the Fabric project's Fantasy-library-based
// FantasyDimensionRuntimeProvider: Fantasy is a Fabric-only library with no NeoForge port, so this
// class hand-rolls the same "create/load a persistent runtime ServerLevel, delete it later without
// a server restart" capability directly against vanilla/NeoForge internals — the same general
// technique every Forge/NeoForge "dynamic dimensions" mod uses, since neither vanilla nor NeoForge
// expose a supported public API for adding/removing a ServerLevel while the server is running.
//
// The recipe below mirrors MinecraftServer#createLevels/#stopServer line for line (construct a
// ServerLevel via its public constructor, insert it into the server's own level map, and for
// teardown drain its ChunkMap exactly the way stopServer's busy-wait loop does — just spread across
// ticks instead of blocking the server thread) rather than reverse-engineering new behavior.
//
// Only known limitation vs. Fantasy: every generator style still shares the OVERWORLD dimension
// type (fixed day/night cycle, respawn-anchor-safe, etc.) — this matches the Fabric project's own
// FantasyDimensionRuntimeProvider exactly, since it never called RuntimeWorldConfig#setDimensionType
// either.
public class NativeDimensionRuntimeProvider implements DimensionRuntimeProvider {

	private static final ChunkProgressListener NOOP_LISTENER = new ChunkProgressListener() {
		@Override
		public void updateSpawnPos(ChunkPos center) {
		}

		@Override
		public void onStatusChange(ChunkPos chunkPos, @Nullable ChunkStatus chunkStatus) {
		}

		@Override
		public void start() {
		}

		@Override
		public void stop() {
		}
	};

	// MinecraftServer#storageSource is protected — there's no public getter, so a single narrow
	// reflective read stands in for the accessor NeoForge doesn't provide. Cached once since the
	// field itself never changes for the process lifetime of a given server instance.
	private static final Field STORAGE_SOURCE_FIELD;

	static {
		try {
			STORAGE_SOURCE_FIELD = MinecraftServer.class.getDeclaredField("storageSource");
			STORAGE_SOURCE_FIELD.setAccessible(true);
		} catch (NoSuchFieldException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	// Multi-tick teardown queue: draining a ChunkMap fully (same thing MinecraftServer#stopServer
	// does for every level at once, via its own busy-wait loop) can take more than one tick, so it
	// can't safely happen synchronously inside unloadAndDeleteWorld without blocking the server.
	private final Deque<PendingUnload> pendingUnloads = new ArrayDeque<>();

	public NativeDimensionRuntimeProvider() {
		NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> tick());
	}

	@Override
	public ServerLevel createOrLoadWorld(DimensionDefinition definition, MinecraftServer server) {
		ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, definition.getId());

		ServerLevel existing = server.forgeGetWorldMap().get(key);
		if (existing != null) {
			return existing;
		}

		ChunkGenerator generator = createChunkGenerator(definition.getGeneratorStyle(), server);
		Holder<DimensionType> dimensionType = server.registryAccess()
				.registryOrThrow(Registries.DIMENSION_TYPE)
				.getHolderOrThrow(BuiltinDimensionTypes.OVERWORLD);
		LevelStem levelStem = new LevelStem(dimensionType, generator);

		ServerLevelData levelData = new DerivedLevelData(server.getWorldData(), server.getWorldData().overworldData());
		long biomeZoomSeed = BiomeManager.obfuscateSeed(definition.getSeed());

		ServerLevel level = new ServerLevel(
				server,
				Util.backgroundExecutor(),
				storageAccess(server),
				levelData,
				key,
				levelStem,
				NOOP_LISTENER,
				false,
				biomeZoomSeed,
				List.of(),
				false,
				null);

		server.forgeGetWorldMap().put(key, level);
		server.getPlayerList().addWorldborderListener(level);
		NeoForge.EVENT_BUS.post(new LevelEvent.Load(level));

		IslandCoreMod.LOGGER.info("Created runtime dimension {}", definition.getId());
		return level;
	}

	@Override
	public void unloadAndDeleteWorld(ResourceLocation id, MinecraftServer server) {
		ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);
		ServerLevel level = server.forgeGetWorldMap().get(key);
		if (level == null) {
			// Not currently loaded: nothing to delete right now. Mirrors the Fabric provider's own
			// documented guard here (see its reference to Multiworld GitHub issue #143) — recreating
			// it first just to immediately delete it again isn't worth the risk of a subtle bug.
			IslandCoreMod.LOGGER.warn("unloadAndDeleteWorld: dimension {} isn't currently loaded, skipping", id);
			return;
		}

		if (pendingUnloads.stream().anyMatch(pending -> pending.key.equals(key))) {
			return;
		}

		level.getChunkSource().removeTicketsOnClosing();
		pendingUnloads.add(new PendingUnload(key, level, server));
	}

	private void tick() {
		if (pendingUnloads.isEmpty()) {
			return;
		}

		for (Iterator<PendingUnload> it = pendingUnloads.iterator(); it.hasNext(); ) {
			PendingUnload pending = it.next();
			ServerChunkCache chunkSource = pending.level.getChunkSource();
			chunkSource.removeTicketsOnClosing();
			chunkSource.tick(() -> true, false);

			if (chunkSource.chunkMap.hasWork()) {
				continue;
			}

			it.remove();
			finishUnload(pending);
		}
	}

	private void finishUnload(PendingUnload pending) {
		NeoForge.EVENT_BUS.post(new LevelEvent.Unload(pending.level));
		try {
			pending.level.close();
		} catch (IOException e) {
			IslandCoreMod.LOGGER.error("Failed to close dimension {}", pending.key.location(), e);
		}

		pending.server.forgeGetWorldMap().remove(pending.key);

		Path dimensionDir = storageAccess(pending.server).getDimensionPath(pending.key);
		deleteDirectoryRecursively(dimensionDir);

		IslandCoreMod.LOGGER.info("Deleted runtime dimension {}", pending.key.location());
	}

	private static void deleteDirectoryRecursively(Path directory) {
		if (!Files.exists(directory)) {
			return;
		}

		try (Stream<Path> walk = Files.walk(directory)) {
			walk.sorted(Comparator.reverseOrder()).forEach(path -> {
				try {
					Files.delete(path);
				} catch (IOException e) {
					IslandCoreMod.LOGGER.error("Failed to delete {} while removing dimension save data", path, e);
				}
			});
		} catch (IOException e) {
			IslandCoreMod.LOGGER.error("Failed to walk dimension save directory {}", directory, e);
		}
	}

	private static LevelStorageSource.LevelStorageAccess storageAccess(MinecraftServer server) {
		try {
			return (LevelStorageSource.LevelStorageAccess) STORAGE_SOURCE_FIELD.get(server);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException("Failed to read MinecraftServer#storageSource reflectively", e);
		}
	}

	private static ChunkGenerator createChunkGenerator(DimensionGeneratorStyle style, MinecraftServer server) {
		Registry<Biome> biomeRegistry = server.registryAccess().registryOrThrow(Registries.BIOME);

		return switch (style) {
			case OVERWORLD_LIKE -> new NoiseBasedChunkGenerator(
					multiNoiseBiomeSource(server, MultiNoiseBiomeSourceParameterLists.OVERWORLD),
					noiseGeneratorSettings(server, NoiseGeneratorSettings.OVERWORLD));
			case NETHER_LIKE -> new NoiseBasedChunkGenerator(
					multiNoiseBiomeSource(server, MultiNoiseBiomeSourceParameterLists.NETHER),
					noiseGeneratorSettings(server, NoiseGeneratorSettings.NETHER));
			case END_LIKE -> new NoiseBasedChunkGenerator(
					TheEndBiomeSource.create(server.registryAccess().lookupOrThrow(Registries.BIOME)),
					noiseGeneratorSettings(server, NoiseGeneratorSettings.END));
			case VOID_FLAT -> new FlatLevelSource(new FlatLevelGeneratorSettings(
					Optional.empty(),
					biomeRegistry.getHolderOrThrow(Biomes.THE_VOID),
					List.of()));
		};
	}

	private static BiomeSource multiNoiseBiomeSource(MinecraftServer server, ResourceKey<MultiNoiseBiomeSourceParameterList> presetKey) {
		Holder<MultiNoiseBiomeSourceParameterList> preset = server.registryAccess()
				.registryOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
				.getHolderOrThrow(presetKey);

		return MultiNoiseBiomeSource.createFromPreset(preset);
	}

	private static Holder<NoiseGeneratorSettings> noiseGeneratorSettings(MinecraftServer server, ResourceKey<NoiseGeneratorSettings> key) {
		return server.registryAccess().registryOrThrow(Registries.NOISE_SETTINGS).getHolderOrThrow(key);
	}

	private static final class PendingUnload {
		final ResourceKey<Level> key;
		final ServerLevel level;
		final MinecraftServer server;

		PendingUnload(ResourceKey<Level> key, ServerLevel level, MinecraftServer server) {
			this.key = key;
			this.level = level;
			this.server = server;
		}
	}
}
