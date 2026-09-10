package com.skyframework.islandcore.island.lifecycle;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.api.island.IslandState;
import com.skyframework.islandcore.island.model.IslandBounds;
import com.skyframework.islandcore.island.model.IslandData;
import com.skyframework.islandcore.teleport.TeleportBackend;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class IslandDeletionServiceImpl implements IslandDeletionService {

	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
	private static final int BLOCKS_PER_TICK = 4096;

	private final TeleportBackend teleportBackend;

	private final Map<UUID, PendingRequest> pendingRequests = new HashMap<>();
	private final Deque<PendingBlockClear> blockClearQueue = new ArrayDeque<>();
	// Guards against double-queuing (e.g. startup recovery re-triggering a deletion whose
	// block-clear job is already in progress from an earlier call this same run).
	private final Set<UUID> islandsBeingDeleted = new HashSet<>();

	// onInitialize() runs before the MinecraftServer exists, so the reference is captured lazily on server start.
	private MinecraftServer server;

	public IslandDeletionServiceImpl(TeleportBackend teleportBackend) {
		this.teleportBackend = teleportBackend;
		NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> this.server = event.getServer());
	}

	@Override
	public void requestDeletion(UUID islandId, UUID requestedBy) {
		Island island = IslandCoreMod.ISLAND_REGISTRY.getIsland(islandId)
				.orElseThrow(() -> new IllegalArgumentException("No existe ninguna isla con id: " + islandId));

		if (island.getState() == IslandState.DELETING) {
			throw new IllegalStateException("Esta isla ya está en proceso de eliminación.");
		}

		pendingRequests.put(islandId, new PendingRequest(requestedBy, Instant.now().plus(REQUEST_TIMEOUT)));
	}

	@Override
	public boolean confirmDeletion(UUID islandId, UUID requestedBy) {
		PendingRequest request = pendingRequests.get(islandId);
		if (request == null) {
			return false;
		}

		if (Instant.now().isAfter(request.expiresAt)) {
			pendingRequests.remove(islandId);
			return false;
		}

		if (!request.requestedBy.equals(requestedBy)) {
			return false;
		}

		pendingRequests.remove(islandId);
		executeDeletion(islandId);
		return true;
	}

	@Override
	public void executeDeletion(UUID islandId) {
		if (islandsBeingDeleted.contains(islandId)) {
			return;
		}

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIsland(islandId);
		if (maybeIsland.isEmpty()) {
			// Already fully deleted: idempotent no-op.
			return;
		}
		Island island = maybeIsland.get();

		// a) Checkpoint: mark DELETING and persist immediately, before touching the world at all.
		IslandCoreMod.ISLAND_REGISTRY.markIslandDeleting(islandId);

		// b) Evict any players currently inside.
		evictPlayers(island);

		// c) Remove non-player entities inside.
		removeEntities(island);

		// d) Queue batched block clearing over the full reserved plot (not just the physically
		// built size): step f) releases this grid cell for reuse, so anything left outside the
		// built area but still within the plot would otherwise linger for the next owner.
		islandsBeingDeleted.add(islandId);
		blockClearQueue.add(new PendingBlockClear(islandId, island.getDimension(), island.getPlotBounds()));
	}

	@Override
	public Optional<Long> getPendingDeletionRemainingSeconds(UUID islandId) {
		PendingRequest request = pendingRequests.get(islandId);
		if (request == null || Instant.now().isAfter(request.expiresAt)) {
			return Optional.empty();
		}
		return Optional.of(Duration.between(Instant.now(), request.expiresAt).getSeconds());
	}

	@Override
	public void tickAll() {
		tickPendingRequests();
		tickBlockClearing();
	}

	private void tickPendingRequests() {
		if (pendingRequests.isEmpty() || server == null) {
			return;
		}

		Instant now = Instant.now();

		for (Iterator<Map.Entry<UUID, PendingRequest>> it = pendingRequests.entrySet().iterator(); it.hasNext(); ) {
			PendingRequest request = it.next().getValue();

			if (now.isAfter(request.expiresAt)) {
				it.remove();
				notifyExpired(request.requestedBy);
				continue;
			}

			long remainingMillis = Duration.between(now, request.expiresAt).toMillis();
			int secondsRemaining = (int) Math.max(1, (remainingMillis + 999) / 1000);

			if (secondsRemaining != request.lastNotifiedSecond) {
				request.lastNotifiedSecond = secondsRemaining;
				notifyCountdown(request.requestedBy, secondsRemaining);
			}
		}
	}

	private void notifyCountdown(UUID playerUuid, int secondsRemaining) {
		ServerPlayer player = resolvePlayer(playerUuid);
		if (player == null) {
			return;
		}

		player.sendSystemMessage(
				Component.literal("Isla se eliminará en " + secondsRemaining + "s - /island delete confirm para confirmar")
						.withStyle(ChatFormatting.RED),
				true);
	}

	private void notifyExpired(UUID playerUuid) {
		ServerPlayer player = resolvePlayer(playerUuid);
		if (player == null) {
			return;
		}

		player.sendSystemMessage(Component.literal("La solicitud de borrado ha caducado. Tu isla sigue intacta."), false);
	}

	private void tickBlockClearing() {
		int budget = BLOCKS_PER_TICK;

		while (budget > 0) {
			PendingBlockClear job = blockClearQueue.peek();
			if (job == null) {
				return;
			}

			ServerLevel world = server != null ? server.getLevel(job.dimension) : null;
			if (world == null) {
				// Can't process this dimension right now; drop the job rather than spin forever.
				finishJob(job);
				continue;
			}

			while (budget > 0 && !job.isDone()) {
				world.setBlock(job.currentPos(), Blocks.AIR.defaultBlockState(), 3);
				job.advance();
				budget--;
			}

			if (job.isDone()) {
				finishJob(job);
			}
		}
	}

	private void finishJob(PendingBlockClear job) {
		blockClearQueue.poll();

		// Between block clearing and the final registry cleanup below: reset the plot's biome
		// back to void, over the full reserved plot (same reasoning as the block clearing itself —
		// step f) releases this grid cell for reuse). islandsBeingDeleted stays marked until this
		// finishes too, not just once block clearing is done, so a resumed/duplicate deletion
		// can't race with it.
		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIsland(job.islandId);
		ServerLevel world = server != null ? server.getLevel(job.dimension) : null;
		Optional<Holder.Reference<Biome>> voidBiome = server != null
				? server.registryAccess().registryOrThrow(Registries.BIOME).getHolder(ResourceLocation.parse(IslandData.DEFAULT_BIOME_ID))
				: Optional.empty();

		if (maybeIsland.isPresent() && world != null && voidBiome.isPresent()) {
			Island island = maybeIsland.get();
			IslandCoreMod.BIOME_APPLIER.enqueue(world, island.getPlotBounds(), voidBiome.get(), island.getOwnerUuid(),
					false, () -> finishDeletion(job.islandId));
		} else {
			// Defensive fallback: couldn't resolve the world/biome/island (e.g. dimension unloaded
			// or already gone) — skip straight to cleanup rather than leaving the island stuck
			// mid-deletion forever.
			finishDeletion(job.islandId);
		}
	}

	private void finishDeletion(UUID islandId) {
		islandsBeingDeleted.remove(islandId);

		// e) unregister spatial index, f) release grid slot, g) delete from storage,
		// h) remove from the in-memory maps — all handled together by the registry.
		IslandCoreMod.ISLAND_REGISTRY.deleteIsland(islandId);
	}

	private void evictPlayers(Island island) {
		if (server == null) {
			return;
		}

		Optional<EvictionTargetResolver.EvictionTarget> target = EvictionTargetResolver.resolve(server);
		if (target.isEmpty()) {
			return;
		}

		for (ServerPlayer player : List.copyOf(server.getPlayerList().getPlayers())) {
			if (!player.level().dimension().equals(island.getDimension())) {
				continue;
			}
			if (!island.getBounds().contains(player.blockPosition())) {
				continue;
			}
			teleportBackend.teleport(player, target.get().world(), target.get().pos());
		}
	}

	private void removeEntities(Island island) {
		for (Entity entity : IslandCoreMod.ENTITY_TRACKER.getEntitiesInIsland(island.getIslandId())) {
			if (!(entity instanceof Player)) {
				entity.discard();
			}
		}
	}

	private ServerPlayer resolvePlayer(UUID playerUuid) {
		return server != null ? server.getPlayerList().getPlayer(playerUuid) : null;
	}

	// lastNotifiedSecond tracks the countdown value last shown on the action bar, so
	// tickPendingRequests() only re-sends when the displayed second actually changes.
	private static final class PendingRequest {
		final UUID requestedBy;
		final Instant expiresAt;
		int lastNotifiedSecond = -1;

		PendingRequest(UUID requestedBy, Instant expiresAt) {
			this.requestedBy = requestedBy;
			this.expiresAt = expiresAt;
		}
	}

	// Cursor over a bounding box, cleared BLOCKS_PER_TICK positions at a time across ticks
	// so deleting a large island doesn't stall the server for one huge tick.
	private static final class PendingBlockClear {
		final UUID islandId;
		final ResourceKey<Level> dimension;
		final int minX;
		final int minZ;
		final int maxX;
		final int maxY;
		final int maxZ;
		int x;
		int y;
		int z;

		PendingBlockClear(UUID islandId, ResourceKey<Level> dimension, IslandBounds bounds) {
			this.islandId = islandId;
			this.dimension = dimension;
			this.minX = bounds.min().getX();
			this.maxX = bounds.max().getX();
			this.minZ = bounds.min().getZ();
			this.maxZ = bounds.max().getZ();
			this.maxY = bounds.max().getY();
			this.x = minX;
			this.y = bounds.min().getY();
			this.z = minZ;
		}

		BlockPos currentPos() {
			return new BlockPos(x, y, z);
		}

		boolean isDone() {
			return y > maxY;
		}

		void advance() {
			x++;
			if (x > maxX) {
				x = minX;
				z++;
				if (z > maxZ) {
					z = minZ;
					y++;
				}
			}
		}
	}
}
