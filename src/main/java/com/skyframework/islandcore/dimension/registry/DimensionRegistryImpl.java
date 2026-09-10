package com.skyframework.islandcore.dimension.registry;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.dimension.model.DimensionData;
import com.skyframework.islandcore.dimension.model.DimensionDefinition;
import com.skyframework.islandcore.dimension.model.DimensionGeneratorStyle;
import com.skyframework.islandcore.dimension.model.DimensionState;
import com.skyframework.islandcore.dimension.runtime.DimensionRuntimeProvider;
import com.skyframework.islandcore.dimension.storage.DimensionStorage;
import com.skyframework.islandcore.dimension.storage.NbtDimensionStorage;
import com.skyframework.islandcore.teleport.TeleportBackend;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// Deliberately independent of the island/ package as a general rule: this is a standalone module,
// following the same registry+storage shape as IslandRegistryImpl but for dimensions.
//
// Eviction note — the ONE justified exception to that independence: resolveEvacuationTarget()
// below queries IslandCoreMod.ISLAND_REGISTRY (via the public api.island.Island interface only,
// never island.lifecycle internals) to prefer the Spawn island's home when evacuating players,
// mirroring the exact same priority island.lifecycle.EvictionTargetResolver already uses for
// kicks/island deletion. This keeps the evacuated-to location consistent across both systems
// instead of players landing somewhere different depending on which system evicted them. It's a
// single, narrow, read-only lookup — not a structural dependency — so it's accepted here rather
// than duplicating a whole shared module or leaving the inconsistency in place.
public class DimensionRegistryImpl implements DimensionRegistry {

	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

	private final Map<ResourceLocation, DimensionData> dimensionsById = new HashMap<>();
	private final Map<ResourceLocation, PendingConfirmation> pendingConfirmations = new HashMap<>();
	private final Map<ResourceLocation, PendingRemoval> pendingRemovals = new HashMap<>();

	private final DimensionRuntimeProvider runtimeProvider;
	private final TeleportBackend teleportBackend;

	private DimensionStorage storage;

	// onInitialize() runs before the MinecraftServer exists, so the reference is captured lazily on server start.
	private MinecraftServer server;

	public DimensionRegistryImpl(DimensionRuntimeProvider runtimeProvider, TeleportBackend teleportBackend) {
		this.runtimeProvider = runtimeProvider;
		this.teleportBackend = teleportBackend;

		// Self-contained SERVER_STARTED/tick hooks, same pattern as IslandRegistryImpl — this
		// package can't share that listener since it must not import anything from island/.
		NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> {
			this.server = event.getServer();
			initializeStorage(this.server.getWorldPath(LevelResource.ROOT));
		});
		NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> tickAll());
	}

	public void initializeStorage(Path worldSaveDir) {
		storage = new NbtDimensionStorage(worldSaveDir.resolve("islandcore").resolve("dimensions"));

		Collection<DimensionData> loaded = storage.loadAll();

		for (DimensionData dimension : loaded) {
			dimensionsById.put(dimension.getId(), dimension);

			// This is the piece Multiworld/WorldManager don't guarantee reliably: every saved
			// dimension is re-materialized right here, on every boot, rather than leaving that up
			// to a separate step that might not run.
			if (server != null) {
				runtimeProvider.createOrLoadWorld(dimension, server);
			}
		}

		// Resume any deletion/regeneration that was mid-flight when the server last stopped. Must
		// run after the loop above so the dimension is actually loaded again first (deleteDimension
		// / regenerateDimension need a live world to evacuate/tear down).
		if (server == null) {
			return;
		}

		for (DimensionData dimension : loaded) {
			if (dimension.getState() == DimensionState.DELETING) {
				deleteDimension(dimension.getId());
			} else if (dimension.getState() == DimensionState.REGENERATING) {
				// The new seed is only persisted once the swap actually completes (see
				// finishRemoval), so a crash mid-regenerate always leaves the OLD seed still
				// stored. Regenerating with that same seed is safe and self-healing: it just
				// finishes tearing down and rebuilds an equivalent dimension, instead of leaving
				// it stuck in REGENERATING forever waiting for a manual admin regenerate.
				regenerateDimension(dimension.getId(), dimension.getSeed());
			}
		}
	}

	@Override
	public Optional<DimensionDefinition> getDimension(ResourceLocation id) {
		return Optional.ofNullable(dimensionsById.get(id));
	}

	@Override
	public Collection<DimensionDefinition> getAllDimensions() {
		return List.copyOf(dimensionsById.values());
	}

	@Override
	public boolean exists(ResourceLocation id) {
		return dimensionsById.containsKey(id);
	}

	@Override
	public DimensionDefinition createDimension(ResourceLocation id, String displayName, DimensionGeneratorStyle style, long seed) {
		if (dimensionsById.containsKey(id)) {
			throw new IllegalStateException("A dimension with id " + id + " already exists");
		}

		Instant now = Instant.now();
		DimensionData dimension = new DimensionData(id, displayName, style, seed, DimensionState.ACTIVE, now, now);

		dimensionsById.put(id, dimension);
		saveIfStorageReady(dimension);

		if (server != null) {
			runtimeProvider.createOrLoadWorld(dimension, server);
		}

		return dimension;
	}

	@Override
	public void requestDeletion(ResourceLocation id, UUID requestedBy) {
		DimensionData dimension = requireActive(id);
		pendingConfirmations.put(id, new PendingConfirmation(requestedBy, Instant.now().plus(REQUEST_TIMEOUT), false, 0L));
	}

	@Override
	public boolean confirmDeletion(ResourceLocation id, UUID requestedBy) {
		return confirmPending(id, requestedBy, false);
	}

	@Override
	public void requestRegeneration(ResourceLocation id, UUID requestedBy, long newSeed) {
		requireActive(id);
		pendingConfirmations.put(id, new PendingConfirmation(requestedBy, Instant.now().plus(REQUEST_TIMEOUT), true, newSeed));
	}

	@Override
	public boolean confirmRegeneration(ResourceLocation id, UUID requestedBy) {
		return confirmPending(id, requestedBy, true);
	}

	@Override
	public void deleteDimension(ResourceLocation id) {
		DimensionData dimension = dimensionsById.get(id);
		if (dimension == null) {
			return;
		}

		// Checkpoint first, before touching the world at all — mirrors IslandDeletionServiceImpl.
		dimension.setState(DimensionState.DELETING);
		saveIfStorageReady(dimension);

		if (server == null) {
			return;
		}

		evacuatePlayers(id, server);
		runtimeProvider.unloadAndDeleteWorld(id, server);
		pendingRemovals.put(id, new PendingRemoval(false, 0L));
	}

	@Override
	public void regenerateDimension(ResourceLocation id, long newSeed) {
		DimensionData dimension = dimensionsById.get(id);
		if (dimension == null) {
			return;
		}

		dimension.setState(DimensionState.REGENERATING);
		saveIfStorageReady(dimension);

		if (server == null) {
			return;
		}

		evacuatePlayers(id, server);
		runtimeProvider.unloadAndDeleteWorld(id, server);
		pendingRemovals.put(id, new PendingRemoval(true, newSeed));
	}

	private DimensionData requireActive(ResourceLocation id) {
		DimensionData dimension = dimensionsById.get(id);
		if (dimension == null) {
			throw new IllegalArgumentException("No existe ninguna dimensión gestionada con id " + id);
		}
		if (dimension.getState() != DimensionState.ACTIVE) {
			throw new IllegalStateException("La dimensión " + id + " ya está en proceso de " + dimension.getState() + ".");
		}
		return dimension;
	}

	private boolean confirmPending(ResourceLocation id, UUID requestedBy, boolean expectRegenerate) {
		PendingConfirmation pending = pendingConfirmations.get(id);
		if (pending == null || pending.regenerate != expectRegenerate) {
			return false;
		}

		if (Instant.now().isAfter(pending.expiresAt)) {
			pendingConfirmations.remove(id);
			return false;
		}

		if (!pending.requestedBy.equals(requestedBy)) {
			return false;
		}

		pendingConfirmations.remove(id);
		if (pending.regenerate) {
			regenerateDimension(id, pending.newSeedIfRegenerating);
		} else {
			deleteDimension(id);
		}
		return true;
	}

	private void tickAll() {
		tickPendingConfirmations();
		tickPendingRemovals();
	}

	private void tickPendingConfirmations() {
		if (pendingConfirmations.isEmpty() || server == null) {
			return;
		}

		Instant now = Instant.now();

		for (Iterator<Map.Entry<ResourceLocation, PendingConfirmation>> it = pendingConfirmations.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<ResourceLocation, PendingConfirmation> entry = it.next();
			ResourceLocation id = entry.getKey();
			PendingConfirmation request = entry.getValue();

			if (now.isAfter(request.expiresAt)) {
				it.remove();
				notifyExpired(id, request.requestedBy);
				continue;
			}

			long remainingMillis = Duration.between(now, request.expiresAt).toMillis();
			int secondsRemaining = (int) Math.max(1, (remainingMillis + 999) / 1000);

			if (secondsRemaining != request.lastNotifiedSecond) {
				request.lastNotifiedSecond = secondsRemaining;
				notifyCountdown(id, request, secondsRemaining);
			}
		}
	}

	private void notifyCountdown(ResourceLocation id, PendingConfirmation request, int secondsRemaining) {
		ServerPlayer player = server.getPlayerList().getPlayer(request.requestedBy);
		if (player == null) {
			return;
		}

		String action = request.regenerate ? "regenerará" : "eliminará";
		String confirmCommand = request.regenerate ? "/dimension regenerate" : "/dimension delete";

		player.displayClientMessage(
				Component.literal("Dimensión " + id.getPath() + " se " + action + " en " + secondsRemaining + "s - "
						+ confirmCommand + " " + id.getPath() + " confirm para confirmar")
						.withStyle(ChatFormatting.RED),
				true);
	}

	private void notifyExpired(ResourceLocation id, UUID playerUuid) {
		ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
		if (player == null) {
			return;
		}

		player.sendSystemMessage(Component.literal("La solicitud sobre la dimensión " + id.getPath() + " ha caducado."));
	}

	private void tickPendingRemovals() {
		if (pendingRemovals.isEmpty() || server == null) {
			return;
		}

		for (Iterator<Map.Entry<ResourceLocation, PendingRemoval>> it = pendingRemovals.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<ResourceLocation, PendingRemoval> entry = it.next();
			ResourceLocation id = entry.getKey();

			ResourceKey<Level> worldKey = ResourceKey.create(Registries.DIMENSION, id);
			if (server.getLevel(worldKey) != null) {
				// The runtime provider is still waiting for the dimension to be empty of
				// players/loaded chunks.
				continue;
			}

			it.remove();
			finishRemoval(id, entry.getValue());
		}
	}

	private void finishRemoval(ResourceLocation id, PendingRemoval removal) {
		DimensionData dimension = dimensionsById.get(id);

		if (removal.regenerate && dimension != null) {
			dimension.setSeed(removal.newSeedIfRegenerating);
			dimension.setState(DimensionState.ACTIVE);
			saveIfStorageReady(dimension);
			runtimeProvider.createOrLoadWorld(dimension, server);
		} else {
			if (storage != null) {
				storage.delete(id);
			}
			dimensionsById.remove(id);
		}
	}

	private void evacuatePlayers(ResourceLocation id, MinecraftServer server) {
		ResourceKey<Level> worldKey = ResourceKey.create(Registries.DIMENSION, id);

		Optional<EvacuationTarget> target = resolveEvacuationTarget(server);
		if (target.isEmpty()) {
			return;
		}

		for (ServerPlayer player : List.copyOf(server.getPlayerList().getPlayers())) {
			if (player.level().dimension().equals(worldKey)) {
				teleportBackend.teleport(player, target.get().world(), target.get().pos());
			}
		}
	}

	// See the class-level "Eviction note" comment for why this one lookup into island/ is accepted.
	private Optional<EvacuationTarget> resolveEvacuationTarget(MinecraftServer server) {
		Optional<Island> spawnIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(Island.SERVER_OWNER_UUID);
		if (spawnIsland.isPresent()) {
			Island spawn = spawnIsland.get();
			ServerLevel world = server.getLevel(spawn.getDimension());
			if (world != null) {
				return Optional.of(new EvacuationTarget(world, spawn.getHomeLocation()));
			}
		}

		ServerLevel overworld = server.overworld();
		if (overworld == null) {
			return Optional.empty();
		}
		return Optional.of(new EvacuationTarget(overworld, overworld.getSharedSpawnPos()));
	}

	private record EvacuationTarget(ServerLevel world, BlockPos pos) {
	}

	private void saveIfStorageReady(DimensionData dimension) {
		if (storage != null) {
			storage.save(dimension);
		}
	}

	// lastNotifiedSecond tracks the countdown value last shown on the action bar, so
	// tickPendingConfirmations() only re-sends when the displayed second actually changes.
	private static final class PendingConfirmation {
		final UUID requestedBy;
		final Instant expiresAt;
		final boolean regenerate;
		final long newSeedIfRegenerating;
		int lastNotifiedSecond = -1;

		PendingConfirmation(UUID requestedBy, Instant expiresAt, boolean regenerate, long newSeedIfRegenerating) {
			this.requestedBy = requestedBy;
			this.expiresAt = expiresAt;
			this.regenerate = regenerate;
			this.newSeedIfRegenerating = newSeedIfRegenerating;
		}
	}

	private static final class PendingRemoval {
		final boolean regenerate;
		final long newSeedIfRegenerating;

		PendingRemoval(boolean regenerate, long newSeedIfRegenerating) {
			this.regenerate = regenerate;
			this.newSeedIfRegenerating = newSeedIfRegenerating;
		}
	}
}
