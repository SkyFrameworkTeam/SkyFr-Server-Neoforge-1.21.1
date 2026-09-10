package com.skyframework.islandcore.teleport;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.api.network.ActionOutcome;
import com.skyframework.islandcore.api.network.ActionReason;
import com.skyframework.islandcore.api.permission.IslandPermissions;
import com.skyframework.islandcore.rtp.SafeRandomTeleportFinder;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class TeleportManagerImpl implements TeleportManager {

	// TODO: make configurable once a general config system exists.
	private static final int HOME_WARMUP_TICKS = 60;

	private static final double CANCEL_MOVE_DISTANCE = 0.5;

	private final TeleportBackend backend;
	private final SafeRandomTeleportFinder rtpFinder = new SafeRandomTeleportFinder();
	private final Map<UUID, PendingTeleport> pending = new HashMap<>();
	private final Map<UUID, Instant> lastHomeAt = new HashMap<>();
	// Independent from lastHomeAt: a player's /island home and /spawn cooldowns run separately.
	private final Map<UUID, Instant> lastSpawnAt = new HashMap<>();
	// Independent from the other two: /farming has its own cooldown.
	private final Map<UUID, Instant> lastFarmingAt = new HashMap<>();
	// Moved in from RtpCommand (Sprint "acciones de isla"): the cooldown state needs a single
	// shared home now that both the text command and the future network handler call requestRtp.
	private final Map<UUID, Instant> lastRtpAt = new HashMap<>();

	// onInitialize() runs before the MinecraftServer exists, so the reference is captured lazily on server start.
	private MinecraftServer server;

	public TeleportManagerImpl(TeleportBackend backend) {
		this.backend = backend;
		NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> this.server = event.getServer());
	}

	@Override
	public ActionOutcome<Void> requestHome(ServerPlayer player) {
		UUID playerUuid = player.getUUID();

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(playerUuid);
		if (maybeIsland.isEmpty()) {
			player.sendSystemMessage(Component.literal("No tienes ninguna isla todavía."));
			return ActionOutcome.fail(ActionReason.NO_ISLAND);
		}

		Island island = maybeIsland.get();
		BlockPos home = island.getHomeLocation();
		if (home == null) {
			player.sendSystemMessage(Component.literal("Tu isla no tiene un home asignado."));
			return ActionOutcome.fail(ActionReason.HOME_NOT_SET);
		}

		if (!IslandCoreMod.PERMISSION_PROVIDER.hasPermission(playerUuid, IslandPermissions.TELEPORT_COOLDOWN_BYPASS)) {
			Instant last = lastHomeAt.get(playerUuid);
			if (last != null) {
				// Re-evaluated on every call (not cached): the player's permissions can change
				// between one /island home attempt and the next.
				long cooldownMillis = IslandCoreMod.PERMISSION_PROVIDER.getHomeCooldownSeconds(playerUuid) * 1000L;
				long elapsedMillis = Duration.between(last, Instant.now()).toMillis();
				if (elapsedMillis < cooldownMillis) {
					long remainingSeconds = (cooldownMillis - elapsedMillis + 999) / 1000;
					player.sendSystemMessage(Component.literal(
							"Debes esperar " + remainingSeconds + " segundos más para volver a usar /island home."));
					return ActionOutcome.fail(ActionReason.COOLDOWN_ACTIVE);
				}
			}
		}

		// Not reachable today through any normal path (resizeIsland only ever grows the bounds,
		// and home is always initialized to the island's center on creation), but defensive for
		// whatever shrinks/relocations a future sprint might add. Auto-correct rather than fail
		// the request outright, so the player isn't stuck unable to use /island home at all.
		if (!island.getBounds().contains(home)) {
			BlockPos center = island.getCenter();
			IslandCoreMod.ISLAND_REGISTRY.updateHomeLocation(island.getIslandId(), center);
			home = center;
			player.sendSystemMessage(Component.literal(
					"Tu home estaba fuera de los límites actuales de tu isla; se ha reajustado al centro."));
		}

		// Homes set before SafeLandingChecker existed (via /island sethome without validation), or
		// with a block broken out from under them since, may not have solid ground anymore —
		// teleporting there would drop the player into the void, which (if enabled) hands off to
		// VoidRescueListener; that used to fall back to this SAME unsafe point whenever it coincided
		// with the island's center (home defaults to center on creation, so breaking the ground
		// there breaks both at once), causing an actual infinite fall/rescue loop. Search outward
		// for the nearest safe spot instead of blindly trusting the center, and persist the fix so
		// this self-corrects once instead of re-running (and re-messaging the player) on every
		// future /island home.
		ServerLevel homeWorld = server != null ? server.getLevel(island.getDimension()) : null;
		if (homeWorld != null && !SafeLandingChecker.isSafe(homeWorld, home)) {
			BlockPos safe = SafeLocationFinder.findNearestSafe(homeWorld, home, SafeLocationFinder.DEFAULT_SEARCH_RADIUS, island.getBounds())
					.orElseGet(island::getCenter);
			IslandCoreMod.ISLAND_REGISTRY.updateHomeLocation(island.getIslandId(), safe);
			home = safe;
			player.sendSystemMessage(Component.literal(
					"Tu home no tenía suelo seguro debajo; se ha reajustado a un punto seguro cercano."));
		}

		pending.put(playerUuid, new PendingTeleport(
				playerUuid, island.getDimension(), home, player.position(), HOME_WARMUP_TICKS, PendingTeleport.Kind.HOME));

		// Neutral confirmation only: the green countdown numbers (below, in tickAll) carry the
		// actual "X seconds left" information, starting almost immediately after this.
		player.sendSystemMessage(Component.literal("Preparando teletransporte a tu isla. No te muevas ni recibas daño."));
		return ActionOutcome.ok();
	}

	@Override
	public ActionOutcome<Void> requestSpawn(ServerPlayer player) {
		UUID playerUuid = player.getUUID();

		Optional<Island> maybeSpawnIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(Island.SERVER_OWNER_UUID);
		if (maybeSpawnIsland.isEmpty()) {
			player.sendSystemMessage(Component.literal("La isla de Spawn todavía no existe."));
			return ActionOutcome.fail(ActionReason.SPAWN_NOT_EXISTS);
		}

		Island spawnIsland = maybeSpawnIsland.get();
		BlockPos home = spawnIsland.getHomeLocation();

		// Same broken-block-under-home safety net as requestHome above — the Spawn island is
		// exactly as vulnerable to it (and, being everyone's fallback destination, arguably more
		// disruptive when it breaks), but had no validation here at all before this fix.
		ServerLevel spawnWorld = server != null ? server.getLevel(spawnIsland.getDimension()) : null;
		if (spawnWorld != null && home != null && !SafeLandingChecker.isSafe(spawnWorld, home)) {
			BlockPos safe = SafeLocationFinder.findNearestSafe(spawnWorld, home, SafeLocationFinder.DEFAULT_SEARCH_RADIUS, spawnIsland.getBounds())
					.orElseGet(spawnIsland::getCenter);
			IslandCoreMod.ISLAND_REGISTRY.updateHomeLocation(spawnIsland.getIslandId(), safe);
			home = safe;
			player.sendSystemMessage(Component.literal(
					"El home de Spawn no tenía suelo seguro debajo; se ha reajustado a un punto seguro cercano."));
		}

		if (!IslandCoreMod.PERMISSION_PROVIDER.hasPermission(playerUuid, IslandPermissions.SPAWN_COOLDOWN_BYPASS)) {
			Instant last = lastSpawnAt.get(playerUuid);
			if (last != null) {
				long cooldownMillis = IslandCoreMod.PERMISSION_PROVIDER.getSpawnCooldownSeconds(playerUuid) * 1000L;
				long elapsedMillis = Duration.between(last, Instant.now()).toMillis();
				if (elapsedMillis < cooldownMillis) {
					long remainingSeconds = (cooldownMillis - elapsedMillis + 999) / 1000;
					player.sendSystemMessage(Component.literal(
							"Debes esperar " + remainingSeconds + " segundos más para volver a usar /spawn."));
					return ActionOutcome.fail(ActionReason.COOLDOWN_ACTIVE);
				}
			}
		}

		pending.put(playerUuid, new PendingTeleport(
				playerUuid, spawnIsland.getDimension(), home, player.position(), HOME_WARMUP_TICKS, PendingTeleport.Kind.SPAWN));

		player.sendSystemMessage(Component.literal("Preparando teletransporte al spawn. No te muevas ni recibas daño."));
		return ActionOutcome.ok();
	}

	@Override
	public ActionOutcome<Void> requestFarming(ServerPlayer player) {
		UUID playerUuid = player.getUUID();

		ResourceLocation targetDimensionId = IslandCoreMod.FARMING_CONFIG.getTargetDimension();
		ResourceKey<Level> targetDimension = ResourceKey.create(Registries.DIMENSION, targetDimensionId);

		// Resolved (and checked for availability) right away, unlike requestHome/requestSpawn:
		// their target position comes from the Island model regardless of whether the world is
		// currently loaded, but the farming destination IS the target world's own spawn point, so
		// there's nothing to compute without a live world to ask.
		ServerLevel world = server != null ? server.getLevel(targetDimension) : null;
		if (world == null) {
			player.sendSystemMessage(Component.literal("La dimensión de farmeo no está disponible ahora mismo."));
			return ActionOutcome.fail(ActionReason.DIMENSION_UNAVAILABLE);
		}

		if (!IslandCoreMod.PERMISSION_PROVIDER.hasPermission(playerUuid, IslandPermissions.FARMING_COOLDOWN_BYPASS)) {
			Instant last = lastFarmingAt.get(playerUuid);
			if (last != null) {
				long cooldownMillis = IslandCoreMod.PERMISSION_PROVIDER.getFarmingCooldownSeconds(playerUuid) * 1000L;
				long elapsedMillis = Duration.between(last, Instant.now()).toMillis();
				if (elapsedMillis < cooldownMillis) {
					long remainingSeconds = (cooldownMillis - elapsedMillis + 999) / 1000;
					player.sendSystemMessage(Component.literal(
							"Debes esperar " + remainingSeconds + " segundos más para volver a usar /farming."));
					return ActionOutcome.fail(ActionReason.COOLDOWN_ACTIVE);
				}
			}
		}

		pending.put(playerUuid, new PendingTeleport(
				playerUuid, targetDimension, world.getSharedSpawnPos(), player.position(), HOME_WARMUP_TICKS, PendingTeleport.Kind.FARMING));

		player.sendSystemMessage(Component.literal("Preparando teletransporte a la zona de farmeo. No te muevas ni recibas daño."));
		return ActionOutcome.ok();
	}

	@Override
	public ActionOutcome<Long> requestRtp(ServerPlayer player) {
		UUID playerUuid = player.getUUID();

		if (!IslandCoreMod.RTP_CONFIG.isEnabled()) {
			return ActionOutcome.fail(ActionReason.RTP_DISABLED);
		}

		ServerLevel world = player.serverLevel();
		if (!IslandCoreMod.RTP_CONFIG.isAllowed(world.dimension().location())) {
			return ActionOutcome.fail(ActionReason.RTP_DIMENSION_NOT_ALLOWED);
		}

		if (!IslandCoreMod.PERMISSION_PROVIDER.hasPermission(playerUuid, IslandPermissions.RTP_COOLDOWN_BYPASS)) {
			Instant last = lastRtpAt.get(playerUuid);
			if (last != null) {
				long cooldownSeconds = IslandCoreMod.PERMISSION_PROVIDER.getRtpCooldownSeconds(playerUuid);
				Instant availableAt = last.plusSeconds(cooldownSeconds);
				if (Instant.now().isBefore(availableAt)) {
					long remainingSeconds = Duration.between(Instant.now(), availableAt).getSeconds();
					return new ActionOutcome<>(false, ActionReason.COOLDOWN_ACTIVE, remainingSeconds);
				}
			}
		}

		Optional<BlockPos> maybePos = rtpFinder.findSafeLocation(world, IslandCoreMod.RTP_CONFIG);
		if (maybePos.isEmpty()) {
			// No cooldown applied: don't penalize the player for bad luck finding a spot.
			return ActionOutcome.fail(ActionReason.RTP_NO_SAFE_LOCATION);
		}

		backend.teleport(player, world, maybePos.get());
		lastRtpAt.put(playerUuid, Instant.now());

		return ActionOutcome.ok();
	}

	@Override
	public long getHomeCooldownRemainingSeconds(UUID playerUuid) {
		return remainingCooldownSeconds(playerUuid, lastHomeAt, IslandPermissions.TELEPORT_COOLDOWN_BYPASS,
				IslandCoreMod.PERMISSION_PROVIDER.getHomeCooldownSeconds(playerUuid));
	}

	@Override
	public long getSpawnCooldownRemainingSeconds(UUID playerUuid) {
		return remainingCooldownSeconds(playerUuid, lastSpawnAt, IslandPermissions.SPAWN_COOLDOWN_BYPASS,
				IslandCoreMod.PERMISSION_PROVIDER.getSpawnCooldownSeconds(playerUuid));
	}

	@Override
	public long getFarmingCooldownRemainingSeconds(UUID playerUuid) {
		return remainingCooldownSeconds(playerUuid, lastFarmingAt, IslandPermissions.FARMING_COOLDOWN_BYPASS,
				IslandCoreMod.PERMISSION_PROVIDER.getFarmingCooldownSeconds(playerUuid));
	}

	@Override
	public long getRtpCooldownRemainingSeconds(UUID playerUuid) {
		return remainingCooldownSeconds(playerUuid, lastRtpAt, IslandPermissions.RTP_COOLDOWN_BYPASS,
				IslandCoreMod.PERMISSION_PROVIDER.getRtpCooldownSeconds(playerUuid));
	}

	private static long remainingCooldownSeconds(UUID playerUuid, Map<UUID, Instant> lastAt, String bypassPermission, long cooldownSeconds) {
		if (IslandCoreMod.PERMISSION_PROVIDER.hasPermission(playerUuid, bypassPermission)) {
			return 0;
		}

		Instant last = lastAt.get(playerUuid);
		if (last == null) {
			return 0;
		}

		Instant availableAt = last.plusSeconds(cooldownSeconds);
		Instant now = Instant.now();
		return now.isBefore(availableAt) ? Duration.between(now, availableAt).getSeconds() : 0;
	}

	@Override
	public void tickAll() {
		if (pending.isEmpty()) {
			return;
		}

		for (UUID playerUuid : List.copyOf(pending.keySet())) {
			PendingTeleport teleport = pending.get(playerUuid);
			if (teleport == null) {
				continue;
			}

			ServerPlayer player = resolvePlayer(playerUuid);
			if (player == null) {
				// Disconnected mid-warmup: nothing left to notify.
				pending.remove(playerUuid);
				continue;
			}

			if (player.position().distanceTo(teleport.startPosition) > CANCEL_MOVE_DISTANCE) {
				cancelPendingTeleport(playerUuid, "te has movido");
				continue;
			}

			teleport.ticksRemaining--;

			if (teleport.ticksRemaining > 0) {
				if (teleport.ticksRemaining % 20 == 0) {
					int secondsRemaining = teleport.ticksRemaining / 20;
					player.sendSystemMessage(Component.literal(secondsRemaining + "...").withStyle(ChatFormatting.GREEN));
				}
				continue;
			}

			pending.remove(playerUuid);
			completeTeleport(player, teleport);
		}
	}

	private void completeTeleport(ServerPlayer player, PendingTeleport teleport) {
		ServerLevel world = server != null ? server.getLevel(teleport.targetDimension) : null;
		if (world == null) {
			player.sendSystemMessage(Component.literal("No se ha podido completar el teletransporte: dimensión no disponible."));
			return;
		}

		boolean success = backend.teleport(player, world, teleport.targetPos);
		if (!success) {
			player.sendSystemMessage(Component.literal("No se ha podido completar el teletransporte."));
			return;
		}

		if (teleport.kind == PendingTeleport.Kind.SPAWN) {
			lastSpawnAt.put(teleport.playerUuid, Instant.now());
			player.sendSystemMessage(Component.literal("¡Teletransportado al spawn!"));
		} else if (teleport.kind == PendingTeleport.Kind.FARMING) {
			lastFarmingAt.put(teleport.playerUuid, Instant.now());
			player.sendSystemMessage(Component.literal("¡Teletransportado a la zona de farmeo!"));
		} else {
			lastHomeAt.put(teleport.playerUuid, Instant.now());
			player.sendSystemMessage(Component.literal("¡Teletransportado a tu isla!"));
		}
	}

	@Override
	public void cancelPendingTeleport(UUID playerUuid, String reason) {
		PendingTeleport removed = pending.remove(playerUuid);
		if (removed == null) {
			return;
		}

		ServerPlayer player = resolvePlayer(playerUuid);
		if (player != null) {
			player.sendSystemMessage(Component.literal("Teletransporte cancelado: " + reason + "."));
		}
	}

	private ServerPlayer resolvePlayer(UUID playerUuid) {
		return server != null ? server.getPlayerList().getPlayer(playerUuid) : null;
	}
}
