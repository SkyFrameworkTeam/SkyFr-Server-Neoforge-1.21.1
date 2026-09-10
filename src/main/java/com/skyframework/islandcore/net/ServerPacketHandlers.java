package com.skyframework.islandcore.net;

import com.mojang.authlib.GameProfile;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.api.network.ActionOutcome;
import com.skyframework.islandcore.api.network.ActionReason;
import com.skyframework.islandcore.dimension.model.DimensionDefinition;
import com.skyframework.islandcore.dimension.model.DimensionGeneratorStyle;
import com.skyframework.islandcore.dimension.vanilla.PendingVanillaReset;
import com.skyframework.islandcore.island.lifecycle.IslandActionService;
import com.skyframework.islandcore.island.lifecycle.MembershipService;
import com.skyframework.islandcore.island.model.IslandMember;
import com.skyframework.islandcore.island.model.IslandRole;
import com.skyframework.islandcore.island.model.IslandSetting;
import com.skyframework.islandcore.party.lifecycle.PartyDisbandRequests;
import com.skyframework.islandcore.party.model.PartyData;
import com.skyframework.islandcore.net.admin.dimension.DimensionAdminBuilder;
import com.skyframework.islandcore.net.admin.dimension.DimensionCreateC2S;
import com.skyframework.islandcore.net.admin.dimension.DimensionDeleteC2S;
import com.skyframework.islandcore.net.admin.dimension.DimensionDeleteConfirmC2S;
import com.skyframework.islandcore.net.admin.dimension.DimensionDetailRequestC2S;
import com.skyframework.islandcore.net.admin.dimension.DimensionDetailS2C;
import com.skyframework.islandcore.net.admin.dimension.DimensionListRequestC2S;
import com.skyframework.islandcore.net.admin.dimension.DimensionListS2C;
import com.skyframework.islandcore.net.admin.dimension.DimensionRegenerateC2S;
import com.skyframework.islandcore.net.admin.dimension.DimensionRegenerateConfirmC2S;
import com.skyframework.islandcore.net.admin.island.AdminIslandBuilder;
import com.skyframework.islandcore.net.admin.island.AdminIslandDeleteC2S;
import com.skyframework.islandcore.net.admin.island.AdminIslandDeleteConfirmC2S;
import com.skyframework.islandcore.net.admin.island.AdminIslandDetailRequestC2S;
import com.skyframework.islandcore.net.admin.island.AdminIslandDetailS2C;
import com.skyframework.islandcore.net.admin.island.AdminIslandListRequestC2S;
import com.skyframework.islandcore.net.admin.island.AdminIslandListS2C;
import com.skyframework.islandcore.net.admin.spawn.SpawnAuthorizedPlayerAddC2S;
import com.skyframework.islandcore.net.admin.spawn.SpawnAuthorizedPlayerRemoveC2S;
import com.skyframework.islandcore.net.admin.spawn.SpawnBuildProtectionSetC2S;
import com.skyframework.islandcore.net.admin.spawn.SpawnBuildProtectionStatusRequestC2S;
import com.skyframework.islandcore.net.admin.spawn.SpawnBuildProtectionStatusS2C;
import com.skyframework.islandcore.net.admin.spawn.SpawnIslandCreateC2S;
import com.skyframework.islandcore.net.admin.spawn.SpawnIslandResizeC2S;
import com.skyframework.islandcore.net.admin.spawn.SpawnIslandSetHomeC2S;
import com.skyframework.islandcore.net.admin.spawn.SpawnStatusRequestC2S;
import com.skyframework.islandcore.net.admin.spawn.SpawnStatusS2C;
import com.skyframework.islandcore.net.admin.vanilla.VanillaResetCancelC2S;
import com.skyframework.islandcore.net.admin.vanilla.VanillaResetConfirmC2S;
import com.skyframework.islandcore.net.admin.vanilla.VanillaResetListRequestC2S;
import com.skyframework.islandcore.net.admin.vanilla.VanillaResetListS2C;
import com.skyframework.islandcore.net.admin.vanilla.VanillaResetQueueC2S;
import com.skyframework.islandcore.net.biome.BiomeTiersBuilder;
import com.skyframework.islandcore.net.biome.BiomeTiersRequestC2S;
import com.skyframework.islandcore.net.biome.BiomeTiersS2C;
import com.skyframework.islandcore.net.admin.defaults.AdminDefaultsBuilder;
import com.skyframework.islandcore.net.admin.defaults.AdminDefaultsStatusRequestC2S;
import com.skyframework.islandcore.net.admin.defaults.AdminDefaultsStatusS2C;
import com.skyframework.islandcore.net.admin.defaults.AdminExceptionSetServerDefaultC2S;
import com.skyframework.islandcore.net.admin.defaults.AdminFlagSetRequirementC2S;
import com.skyframework.islandcore.net.admin.defaults.AdminFlagSetServerDefaultC2S;
import com.skyframework.islandcore.net.flag.ExceptionGroupSetPresetC2S;
import com.skyframework.islandcore.net.flag.ExceptionGroupsStatusRequestC2S;
import com.skyframework.islandcore.net.flag.ExceptionGroupsStatusS2C;
import com.skyframework.islandcore.net.flag.FlagSetC2S;
import com.skyframework.islandcore.net.flag.FlagSetPresetC2S;
import com.skyframework.islandcore.net.flag.FlagsStatusBuilder;
import com.skyframework.islandcore.net.flag.FlagsStatusRequestC2S;
import com.skyframework.islandcore.net.flag.FlagsStatusS2C;
import com.skyframework.islandcore.net.handshake.ClientHandshakeC2S;
import com.skyframework.islandcore.net.handshake.ServerHandshakeS2C;
import com.skyframework.islandcore.net.island.IslandBiomeChangeC2S;
import com.skyframework.islandcore.net.island.IslandCreateC2S;
import com.skyframework.islandcore.net.island.IslandDeleteConfirmC2S;
import com.skyframework.islandcore.net.island.IslandDeleteRequestC2S;
import com.skyframework.islandcore.net.island.IslandSettingsUpdateC2S;
import com.skyframework.islandcore.net.island.IslandSnapshotBuilder;
import com.skyframework.islandcore.net.island.IslandSnapshotRequestC2S;
import com.skyframework.islandcore.net.island.IslandSnapshotS2C;
import com.skyframework.islandcore.net.island.IslandUpgradeC2S;
import com.skyframework.islandcore.net.member.MemberAllyAddC2S;
import com.skyframework.islandcore.net.member.MemberAllyRemoveC2S;
import com.skyframework.islandcore.net.member.MemberInviteAcceptC2S;
import com.skyframework.islandcore.net.member.MemberInviteC2S;
import com.skyframework.islandcore.net.member.MemberRemoveC2S;
import com.skyframework.islandcore.net.member.MemberTrustC2S;
import com.skyframework.islandcore.net.party.PartyAcceptC2S;
import com.skyframework.islandcore.net.party.PartyAllyAddC2S;
import com.skyframework.islandcore.net.party.PartyAllyRemoveC2S;
import com.skyframework.islandcore.net.party.PartyCreateC2S;
import com.skyframework.islandcore.net.party.PartyDisbandConfirmC2S;
import com.skyframework.islandcore.net.party.PartyDisbandRequestC2S;
import com.skyframework.islandcore.net.party.PartyInviteC2S;
import com.skyframework.islandcore.net.party.PartyKickC2S;
import com.skyframework.islandcore.net.party.PartyLeaveC2S;
import com.skyframework.islandcore.net.party.PartyRenameC2S;
import com.skyframework.islandcore.net.party.PartyStatusBuilder;
import com.skyframework.islandcore.net.party.PartyStatusRequestC2S;
import com.skyframework.islandcore.net.party.PartyStatusS2C;
import com.skyframework.islandcore.net.teleport.TeleportRequestC2S;
import com.skyframework.islandcore.net.teleport.TeleportStatusBuilder;
import com.skyframework.islandcore.net.teleport.TeleportStatusRequestC2S;
import com.skyframework.islandcore.net.teleport.TeleportStatusS2C;
import com.skyframework.islandcore.protection.exception.ExceptionGroup;
import com.skyframework.islandcore.protection.flag.Flag;
import com.skyframework.islandcore.protection.flag.FlagCategory;
import com.skyframework.islandcore.protection.flag.FlagPreset;
import com.skyframework.islandcore.protection.flag.FlagRegistry;
import com.skyframework.islandcore.protection.flag.TriState;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

public final class ServerPacketHandlers {

	private ServerPacketHandlers() {
	}

	public static void register(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar registrar = event.registrar("1");

		registerPayloadTypes(registrar);

		// Deliberately NOT routed through registerGuarded below: this is what DECIDES whether a
		// connection is protocol-compatible in the first place, so it must always be processed.
		registrar.playToServer(ClientHandshakeC2S.TYPE, ClientHandshakeC2S.CODEC, (payload, context) -> {
			ServerPlayer player = (ServerPlayer) context.player();
			context.enqueueWork(() -> {
				ClientSyncNotifier.markHandshakeCompleted(player.getUUID());

				boolean protocolCompatible = payload.protocolVersion() == NetworkChannels.PROTOCOL_VERSION;
				if (protocolCompatible) {
					ClientSyncNotifier.markProtocolCompatible(player.getUUID());
				} else {
					ClientSyncNotifier.markProtocolIncompatible(player.getUUID());
				}

				boolean isOperator = player.hasPermissions(2);
				PacketDistributor.sendToPlayer(player, new ServerHandshakeS2C(NetworkChannels.PROTOCOL_VERSION, protocolCompatible, isOperator));
			});
		});

		registerGuarded(registrar, IslandSnapshotRequestC2S.TYPE, IslandSnapshotRequestC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();
			IslandSnapshotS2C snapshot = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(player.getUUID())
					.map(island -> IslandSnapshotBuilder.build(context.server(), player.getUUID(), island))
					.orElseGet(() -> IslandSnapshotBuilder.buildEmpty(context.server(), player.getUUID()));

			PacketDistributor.sendToPlayer(player, snapshot);
		});

		registerIslandActionHandlers(registrar);
		registerMembershipHandlers(registrar);
		registerTeleportHandlers(registrar);
		registerBiomeTierHandlers(registrar);
		registerFlagExceptionHandlers(registrar);
		registerPartyHandlers(registrar);

		registerAdminIslandHandlers(registrar);
		registerSpawnAdminHandlers(registrar);
		registerDimensionAdminHandlers(registrar);
		registerVanillaResetAdminHandlers(registrar);
		registerAdminDefaultsHandlers(registrar);
	}

	// Every C2S receiver except the handshake itself (see the comment above that registration)
	// routes through here: once a connection is marked protocol-incompatible (mismatched
	// PROTOCOL_VERSION reported at handshake time), every later packet from it is rejected with
	// ActionResultS2C.fail instead of reaching the real handler — a fallback in case the client
	// ignores ServerHandshakeS2C#protocolCompatible=false and keeps talking anyway. Sending a real
	// ActionResultS2C (rather than silently dropping) matters here specifically because some
	// callers are sitting in a client-side PendingActionTracker.await() — silence would leave that
	// waiting forever instead of surfacing a clear error.
	//
	// Also wraps every handler body in IPayloadContext#enqueueWork: NeoForge invokes IPayloadHandler
	// on the network thread, but every handler body below touches shared mutable server state
	// (IslandRegistry, PartyRegistry, etc.) that's only safe to mutate from the main server thread —
	// Fabric's ServerPlayNetworking receivers are already scheduled there, so this restores the same
	// threading guarantee on this port instead of introducing a new class of race condition.
	private static <T extends CustomPacketPayload> void registerGuarded(
			PayloadRegistrar registrar, CustomPacketPayload.Type<T> type,
			net.minecraft.network.codec.StreamCodec<? super net.minecraft.network.RegistryFriendlyByteBuf, T> codec,
			GuardedHandler<T> handler) {
		registrar.playToServer(type, codec, (payload, context) -> {
			ServerPlayer player = (ServerPlayer) context.player();
			context.enqueueWork(() -> {
				if (ClientSyncNotifier.isProtocolIncompatible(player.getUUID())) {
					PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.PROTOCOL_MISMATCH));
					return;
				}
				handler.handle(payload, new Ctx(player, player.getServer()));
			});
		});
	}

	@FunctionalInterface
	private interface GuardedHandler<T extends CustomPacketPayload> {
		void handle(T payload, Ctx context);
	}

	// Stands in for Fabric's ServerPlayNetworking.Context here: only the two accessors every
	// handler body below actually uses.
	private record Ctx(ServerPlayer player, MinecraftServer server) {
	}

	private static void registerPayloadTypes(PayloadRegistrar registrar) {
		registrar.playToClient(ServerHandshakeS2C.TYPE, ServerHandshakeS2C.CODEC, (payload, context) -> { });
		registrar.playToClient(IslandSnapshotS2C.TYPE, IslandSnapshotS2C.CODEC, (payload, context) -> { });

		registrar.playToClient(ActionResultS2C.TYPE, ActionResultS2C.CODEC, (payload, context) -> { });
		registrar.playToClient(PendingConfirmationTickS2C.TYPE, PendingConfirmationTickS2C.CODEC, (payload, context) -> { });



		registrar.playToClient(TeleportStatusS2C.TYPE, TeleportStatusS2C.CODEC, (payload, context) -> { });

		registrar.playToClient(BiomeTiersS2C.TYPE, BiomeTiersS2C.CODEC, (payload, context) -> { });

		registrar.playToClient(FlagsStatusS2C.TYPE, FlagsStatusS2C.CODEC, (payload, context) -> { });
		registrar.playToClient(ExceptionGroupsStatusS2C.TYPE, ExceptionGroupsStatusS2C.CODEC, (payload, context) -> { });

		registrar.playToClient(AdminDefaultsStatusS2C.TYPE, AdminDefaultsStatusS2C.CODEC, (payload, context) -> { });

		registrar.playToClient(PartyStatusS2C.TYPE, PartyStatusS2C.CODEC, (payload, context) -> { });

		registrar.playToClient(AdminIslandListS2C.TYPE, AdminIslandListS2C.CODEC, (payload, context) -> { });
		registrar.playToClient(AdminIslandDetailS2C.TYPE, AdminIslandDetailS2C.CODEC, (payload, context) -> { });

		registrar.playToClient(SpawnStatusS2C.TYPE, SpawnStatusS2C.CODEC, (payload, context) -> { });
		registrar.playToClient(SpawnBuildProtectionStatusS2C.TYPE, SpawnBuildProtectionStatusS2C.CODEC, (payload, context) -> { });

		registrar.playToClient(DimensionListS2C.TYPE, DimensionListS2C.CODEC, (payload, context) -> { });
		registrar.playToClient(DimensionDetailS2C.TYPE, DimensionDetailS2C.CODEC, (payload, context) -> { });

		registrar.playToClient(VanillaResetListS2C.TYPE, VanillaResetListS2C.CODEC, (payload, context) -> { });
	}

	private static void registerIslandActionHandlers(PayloadRegistrar registrar) {
		registerGuarded(registrar, IslandCreateC2S.TYPE, IslandCreateC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();
			ActionOutcome<?> outcome = IslandActionService.create(player, context.server());
			PacketDistributor.sendToPlayer(player, ActionResultS2C.fromOutcome(outcome));
		});

		registerGuarded(registrar, IslandUpgradeC2S.TYPE, IslandUpgradeC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();
			ActionOutcome<?> outcome = IslandActionService.upgrade(player.getUUID());
			PacketDistributor.sendToPlayer(player, ActionResultS2C.fromOutcome(outcome));
		});

		registerGuarded(registrar, IslandDeleteRequestC2S.TYPE, IslandDeleteRequestC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();
			ActionOutcome<Void> outcome = IslandActionService.requestDelete(player.getUUID());
			PacketDistributor.sendToPlayer(player, ActionResultS2C.fromOutcome(outcome));
		});

		registerGuarded(registrar, IslandDeleteConfirmC2S.TYPE, IslandDeleteConfirmC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();
			ActionOutcome<Void> outcome = IslandActionService.confirmDelete(player.getUUID());
			PacketDistributor.sendToPlayer(player, ActionResultS2C.fromOutcome(outcome));
		});

		registerGuarded(registrar, IslandSettingsUpdateC2S.TYPE, IslandSettingsUpdateC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();
			// Same entry point the text command ("/island settings") calls: neither path decides on
			// its own whether a setting id maps to the new Flag system or the old IslandSetting one.
			ActionOutcome<Void> outcome = IslandActionService.updateLegacySetting(player.getUUID(), payload.settingId(), payload.value());
			PacketDistributor.sendToPlayer(player, ActionResultS2C.fromOutcome(outcome));
		});

		registerGuarded(registrar, IslandBiomeChangeC2S.TYPE, IslandBiomeChangeC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();
			ActionOutcome<?> outcome = parseBiomeId(payload.biomeId())
					.map(biomeId -> IslandActionService.changeBiome(player, biomeId, context.server()))
					.orElseGet(() -> ActionOutcome.fail(ActionReason.BIOME_NOT_FOUND));
			PacketDistributor.sendToPlayer(player, ActionResultS2C.fromOutcome(outcome));
		});
	}

	// ResourceLocation.of(...) throws on a malformed string; the text command never hits this path
	// because IdentifierArgumentType already validates the format before executeBiome runs, but a
	// network payload's String field has no such brigadier-level guard.
	private static Optional<ResourceLocation> parseBiomeId(String biomeId) {
		try {
			return Optional.of(ResourceLocation.parse(biomeId));
		} catch (RuntimeException e) {
			return Optional.empty();
		}
	}

	private static void registerMembershipHandlers(PayloadRegistrar registrar) {
		registerGuarded(registrar, MemberInviteC2S.TYPE, MemberInviteC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();
			ActionOutcome<?> outcome = MembershipService.inviteByName(player, payload.targetName(), context.server());
			PacketDistributor.sendToPlayer(player, ActionResultS2C.fromOutcome(outcome));
		});

		registerGuarded(registrar, MemberInviteAcceptC2S.TYPE, MemberInviteAcceptC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();
			ActionOutcome<?> outcome = MembershipService.acceptInvite(player, context.server());
			PacketDistributor.sendToPlayer(player, ActionResultS2C.fromOutcome(outcome));
		});

		// Toggles the target between MEMBER and CO_OWNER depending on their current role — see
		// MembershipService#toggleCoOwner. The MembersScreen "Trust" button reflects this by showing
		// the current state and calling this same packet either direction.
		registerGuarded(registrar, MemberTrustC2S.TYPE, MemberTrustC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();
			ActionOutcome<Void> outcome = MembershipService.toggleCoOwner(player, payload.targetUuid());
			PacketDistributor.sendToPlayer(player, ActionResultS2C.fromOutcome(outcome));
		});

		registerGuarded(registrar, MemberRemoveC2S.TYPE, MemberRemoveC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();
			ActionOutcome<Void> outcome = MembershipService.removeMember(player, payload.targetUuid(), context.server());
			PacketDistributor.sendToPlayer(player, ActionResultS2C.fromOutcome(outcome));
		});

		registerGuarded(registrar, MemberAllyAddC2S.TYPE, MemberAllyAddC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();

			Optional<UUID> targetUuid = MembershipService.resolvePlayerUuid(payload.targetName(), context.server());
			if (targetUuid.isEmpty()) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.TARGET_NOT_FOUND));
				return;
			}

			// Same entry point "/island ally add" calls.
			ActionOutcome<Void> outcome = MembershipService.allyAdd(player, targetUuid.get());
			PacketDistributor.sendToPlayer(player, ActionResultS2C.fromOutcome(outcome));
		});

		registerGuarded(registrar, MemberAllyRemoveC2S.TYPE, MemberAllyRemoveC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();
			// Same entry point "/island ally remove" calls.
			ActionOutcome<Void> outcome = MembershipService.allyRemove(player, payload.targetUuid());
			PacketDistributor.sendToPlayer(player, ActionResultS2C.fromOutcome(outcome));
		});
	}

	private static void registerTeleportHandlers(PayloadRegistrar registrar) {
		registerGuarded(registrar, TeleportRequestC2S.TYPE, TeleportRequestC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();
			ActionOutcome<?> outcome = dispatchTeleportRequest(player, payload.action(), context.server());
			PacketDistributor.sendToPlayer(player, ActionResultS2C.fromOutcome(outcome));
		});

		registerGuarded(registrar, TeleportStatusRequestC2S.TYPE, TeleportStatusRequestC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();
			PacketDistributor.sendToPlayer(player, TeleportStatusBuilder.build(player));
		});
	}

	// SPAWN/FARMING replicate the enabled-check SpawnCommand/FarmingCommand already do before
	// calling TeleportManager — that check lives in the text command today, not in
	// TeleportManagerImpl, so it must be repeated here for the network path to behave the same way.
	private static ActionOutcome<?> dispatchTeleportRequest(ServerPlayer player, TeleportRequestC2S.Type type, MinecraftServer server) {
		return switch (type) {
			case HOME -> IslandCoreMod.TELEPORT_MANAGER.requestHome(player);
			case SPAWN -> IslandCoreMod.SPAWN_CONFIG.isEnabled()
					? IslandCoreMod.TELEPORT_MANAGER.requestSpawn(player)
					: ActionOutcome.fail(ActionReason.SPAWN_DISABLED);
			case FARMING -> IslandCoreMod.FARMING_CONFIG.isEnabled()
					? IslandCoreMod.TELEPORT_MANAGER.requestFarming(player)
					: ActionOutcome.fail(ActionReason.FARMING_DISABLED);
			case RTP -> IslandCoreMod.TELEPORT_MANAGER.requestRtp(player);
		};
	}

	private static void registerBiomeTierHandlers(PayloadRegistrar registrar) {
		registerGuarded(registrar, BiomeTiersRequestC2S.TYPE, BiomeTiersRequestC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();
			PacketDistributor.sendToPlayer(player, BiomeTiersBuilder.build(player));
		});
	}

	// Player-facing (not admin-only), mirrors "/island flags"/"/island exceptions" exactly: every
	// handler below calls the same FlagResolver/ExceptionGroupRegistry/IslandActionService entry
	// points those text commands already use — see FlagsStatusBuilder for the read side.
	private static void registerFlagExceptionHandlers(PayloadRegistrar registrar) {
		registerGuarded(registrar, FlagsStatusRequestC2S.TYPE, FlagsStatusRequestC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();

			Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(player.getUUID());
			if (maybeIsland.isEmpty()) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.NO_ISLAND));
				return;
			}

			PacketDistributor.sendToPlayer(player, FlagsStatusBuilder.buildFlagsStatus(maybeIsland.get(), player.getUUID()));
		});

		registerGuarded(registrar, FlagSetC2S.TYPE, FlagSetC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();

			Optional<Flag> maybeFlag = FlagRegistry.get(payload.flagId());
			if (maybeFlag.isEmpty()) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.FLAG_NOT_FOUND));
				return;
			}

			TriState value;
			try {
				value = TriState.valueOf(payload.value().toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException e) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.INVALID_FLAG_VALUE));
				return;
			}

			// Same entry point "/island flags set" calls.
			ActionOutcome<Void> outcome = IslandActionService.updateFlag(player.getUUID(), maybeFlag.get(), value);
			PacketDistributor.sendToPlayer(player, ActionResultS2C.fromOutcome(outcome));
		});

		registerGuarded(registrar, FlagSetPresetC2S.TYPE, FlagSetPresetC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();
			// Same entry point "/island flags preset" calls — IslandActionService#applyFlagPreset
			// validates flagId/preset itself (via IslandRegistryApi#applyFlagPreset).
			ActionOutcome<Void> outcome = IslandActionService.applyFlagPreset(player.getUUID(), payload.flagId(), payload.preset());
			PacketDistributor.sendToPlayer(player, ActionResultS2C.fromOutcome(outcome));
		});

		registerGuarded(registrar, ExceptionGroupsStatusRequestC2S.TYPE, ExceptionGroupsStatusRequestC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();

			Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(player.getUUID());
			if (maybeIsland.isEmpty()) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.NO_ISLAND));
				return;
			}

			PacketDistributor.sendToPlayer(player, FlagsStatusBuilder.buildExceptionGroupsStatus(maybeIsland.get()));
		});

		registerGuarded(registrar, ExceptionGroupSetPresetC2S.TYPE, ExceptionGroupSetPresetC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();

			Optional<ExceptionGroup> maybeGroup = IslandCoreMod.EXCEPTION_GROUP_REGISTRY.getGroup(payload.groupId());
			if (maybeGroup.isEmpty()) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.EXCEPTION_GROUP_NOT_FOUND));
				return;
			}
			if (!maybeGroup.get().isOwnerConfigurable()) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.EXCEPTION_GROUP_NOT_OWNER_CONFIGURABLE));
				return;
			}

			// Same entry point "/island exceptions preset" calls.
			ActionOutcome<Void> outcome = IslandActionService.applyExceptionGroupPreset(player.getUUID(), payload.groupId(), payload.preset());
			PacketDistributor.sendToPlayer(player, ActionResultS2C.fromOutcome(outcome));
		});
	}

	// Admin-only: server-wide default configuration for ROLE_BASED flags and exception groups — see
	// AdminDefaultsStatusS2C's class javadoc for why ISLAND_GLOBAL flags aren't reachable here.
	private static void registerAdminDefaultsHandlers(PayloadRegistrar registrar) {
		registerGuarded(registrar, AdminDefaultsStatusRequestC2S.TYPE, AdminDefaultsStatusRequestC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			PacketDistributor.sendToPlayer(player, AdminDefaultsBuilder.build());
		});

		registerGuarded(registrar, AdminFlagSetServerDefaultC2S.TYPE, AdminFlagSetServerDefaultC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			Optional<Flag> maybeFlag = FlagRegistry.get(payload.flagId());
			if (maybeFlag.isEmpty() || maybeFlag.get().getCategory() != FlagCategory.ROLE_BASED) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.INVALID_FLAG_PRESET));
				return;
			}

			Optional<FlagPreset> preset = FlagPreset.fromId(payload.preset());
			if (preset.isEmpty()) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.INVALID_FLAG_PRESET));
				return;
			}

			IslandCoreMod.SERVER_FLAG_DEFAULTS.setRoleBasedDefault(payload.flagId(), preset.get());
			PacketDistributor.sendToPlayer(player, ActionResultS2C.ok());
		});

		registerGuarded(registrar, AdminExceptionSetServerDefaultC2S.TYPE, AdminExceptionSetServerDefaultC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			if (IslandCoreMod.EXCEPTION_GROUP_REGISTRY.getGroup(payload.groupId()).isEmpty()) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.INVALID_EXCEPTION_PRESET));
				return;
			}

			Optional<FlagPreset> preset = FlagPreset.fromId(payload.preset());
			if (preset.isEmpty()) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.INVALID_EXCEPTION_PRESET));
				return;
			}

			IslandCoreMod.SERVER_EXCEPTION_DEFAULTS.setDefault(payload.groupId(), preset.get());
			PacketDistributor.sendToPlayer(player, ActionResultS2C.ok());
		});

		registerGuarded(registrar, AdminFlagSetRequirementC2S.TYPE, AdminFlagSetRequirementC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			if (FlagRegistry.get(payload.flagId()).isEmpty()) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.FLAG_NOT_FOUND));
				return;
			}

			String node = payload.permissionNode().isEmpty() ? null : payload.permissionNode();
			IslandCoreMod.FLAG_PERMISSION_REQUIREMENTS.setRequiredPermission(payload.flagId(), node);
			PacketDistributor.sendToPlayer(player, ActionResultS2C.ok());
		});
	}

	// Mirrors "/party" exactly: every handler below calls the same PartyRegistry/
	// PartyInviteManager/PartyDisbandRequests entry points that command tree already uses — see
	// PartyStatusBuilder for the read side.
	private static void registerPartyHandlers(PayloadRegistrar registrar) {
		registerGuarded(registrar, PartyStatusRequestC2S.TYPE, PartyStatusRequestC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();
			PartyStatusS2C response = IslandCoreMod.PARTY_REGISTRY.getPartyOf(player.getUUID())
					.map(party -> PartyStatusBuilder.build(context.server(), party))
					.orElseGet(() -> PartyStatusBuilder.buildAbsent(context.server(), player.getUUID()));
			PacketDistributor.sendToPlayer(player, response);
		});

		registerGuarded(registrar, PartyCreateC2S.TYPE, PartyCreateC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();

			// Pre-checked (read-only, same PartyRegistry accessors #createParty itself uses
			// internally) rather than just try/catching createParty's IllegalStateException: that
			// exception's message is the only way to tell "already in a party" apart from "name
			// taken", and matching on message text would be fragile — this way the client gets the
			// correct ActionReason for each cause instead of one guessed at random.
			if (IslandCoreMod.PARTY_REGISTRY.getPartyOf(player.getUUID()).isPresent()) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.ALREADY_IN_PARTY));
				return;
			}
			if (IslandCoreMod.PARTY_REGISTRY.getPartyByName(payload.name()).isPresent()) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.PARTY_NAME_TAKEN));
				return;
			}

			// Same entry point "/party create" calls.
			IslandCoreMod.PARTY_REGISTRY.createParty(payload.name(), player.getUUID());
			PacketDistributor.sendToPlayer(player, ActionResultS2C.ok());
		});

		registerGuarded(registrar, PartyInviteC2S.TYPE, PartyInviteC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();

			Optional<PartyData> maybeParty = requirePartyLeader(player.getUUID());
			if (maybeParty.isEmpty()) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(notLeaderReason(player.getUUID())));
				return;
			}
			PartyData party = maybeParty.get();

			Optional<UUID> targetUuid = MembershipService.resolvePlayerUuid(payload.targetName(), context.server());
			if (targetUuid.isEmpty()) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.TARGET_NOT_FOUND));
				return;
			}

			// Same entry point "/party invite" calls — PartyInviteManager#requestInvite itself
			// throws if the target is already in a party (single possible cause here, unlike
			// createParty above, so a plain try/catch is precise enough).
			try {
				IslandCoreMod.PARTY_INVITE_MANAGER.requestInvite(party.getPartyId(), player.getUUID(), targetUuid.get());
			} catch (IllegalStateException e) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.ALREADY_IN_PARTY));
				return;
			}

			ServerPlayer targetPlayer = context.server().getPlayerList().getPlayer(targetUuid.get());
			if (targetPlayer != null) {
				targetPlayer.sendSystemMessage(Component.literal(player.getGameProfile().getName()
						+ " te ha invitado a su party \"" + party.getName()
						+ "\". Usa /party accept en los próximos 5 minutos para unirte."));
			}

			PacketDistributor.sendToPlayer(player, ActionResultS2C.ok());
		});

		registerGuarded(registrar, PartyAcceptC2S.TYPE, PartyAcceptC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();

			Optional<PartyData> maybeParty;
			try {
				// Same entry point "/party accept" calls.
				maybeParty = IslandCoreMod.PARTY_INVITE_MANAGER.acceptInvite(player.getUUID());
			} catch (IllegalStateException e) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.ALREADY_IN_PARTY));
				return;
			}

			if (maybeParty.isEmpty()) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.NO_PENDING_PARTY_INVITE));
				return;
			}
			PartyData party = maybeParty.get();

			ServerPlayer leader = context.server().getPlayerList().getPlayer(party.getLeaderUuid());
			if (leader != null) {
				leader.sendSystemMessage(Component.literal(
						player.getGameProfile().getName() + " ha aceptado tu invitación y se ha unido a la party."));
			}

			PacketDistributor.sendToPlayer(player, ActionResultS2C.ok());
		});

		registerGuarded(registrar, PartyLeaveC2S.TYPE, PartyLeaveC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();

			if (IslandCoreMod.PARTY_REGISTRY.getPartyOf(player.getUUID()).isEmpty()) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.NO_PARTY));
				return;
			}

			// Same entry point "/party leave" calls.
			IslandCoreMod.PARTY_REGISTRY.leaveParty(player.getUUID());
			PacketDistributor.sendToPlayer(player, ActionResultS2C.ok());
		});

		registerGuarded(registrar, PartyKickC2S.TYPE, PartyKickC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();

			Optional<PartyData> maybeParty = requirePartyLeader(player.getUUID());
			if (maybeParty.isEmpty()) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(notLeaderReason(player.getUUID())));
				return;
			}
			PartyData party = maybeParty.get();

			if (payload.targetUuid().equals(player.getUUID())) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.CANNOT_KICK_SELF));
				return;
			}
			if (!party.getMembers().contains(payload.targetUuid())) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.NOT_A_PARTY_MEMBER));
				return;
			}

			// Same entry point "/party kick" calls.
			IslandCoreMod.PARTY_REGISTRY.removeMember(party.getPartyId(), payload.targetUuid());

			ServerPlayer targetPlayer = context.server().getPlayerList().getPlayer(payload.targetUuid());
			if (targetPlayer != null) {
				targetPlayer.sendSystemMessage(Component.literal("Has sido expulsado de la party \"" + party.getName() + "\"."));
			}

			PacketDistributor.sendToPlayer(player, ActionResultS2C.ok());
		});

		registerGuarded(registrar, PartyRenameC2S.TYPE, PartyRenameC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();

			Optional<PartyData> maybeParty = requirePartyLeader(player.getUUID());
			if (maybeParty.isEmpty()) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(notLeaderReason(player.getUUID())));
				return;
			}
			PartyData party = maybeParty.get();

			// Same reasoning as PartyCreateC2S above: pre-checked so the taken-name case gets its
			// own precise reason rather than a generic catch.
			Optional<PartyData> maybeExisting = IslandCoreMod.PARTY_REGISTRY.getPartyByName(payload.newName());
			if (maybeExisting.isPresent() && !maybeExisting.get().getPartyId().equals(party.getPartyId())) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.PARTY_NAME_TAKEN));
				return;
			}

			// Same entry point "/party rename" calls.
			IslandCoreMod.PARTY_REGISTRY.renameParty(party.getPartyId(), payload.newName());
			PacketDistributor.sendToPlayer(player, ActionResultS2C.ok());
		});

		registerGuarded(registrar, PartyDisbandRequestC2S.TYPE, PartyDisbandRequestC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();

			Optional<PartyData> maybeParty = requirePartyLeader(player.getUUID());
			if (maybeParty.isEmpty()) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(notLeaderReason(player.getUUID())));
				return;
			}

			// Same 15s confirmation window "/party disband" arms.
			PartyDisbandRequests.request(maybeParty.get().getPartyId());
			PacketDistributor.sendToPlayer(player, ActionResultS2C.ok());
		});

		registerGuarded(registrar, PartyDisbandConfirmC2S.TYPE, PartyDisbandConfirmC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();

			Optional<PartyData> maybeParty = requirePartyLeader(player.getUUID());
			if (maybeParty.isEmpty()) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(notLeaderReason(player.getUUID())));
				return;
			}
			PartyData party = maybeParty.get();

			if (!PartyDisbandRequests.confirm(party.getPartyId())) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.NO_PENDING_PARTY_DISBAND));
				return;
			}

			MinecraftServer server = context.server();
			for (UUID memberUuid : party.getMembers()) {
				if (memberUuid.equals(player.getUUID())) {
					continue;
				}
				ServerPlayer member = server.getPlayerList().getPlayer(memberUuid);
				if (member != null) {
					member.sendSystemMessage(Component.literal("La party \"" + party.getName() + "\" ha sido disuelta por su líder."));
				}
			}

			// Same entry point "/party disband confirm" calls.
			IslandCoreMod.PARTY_REGISTRY.disbandParty(party.getPartyId());
			PacketDistributor.sendToPlayer(player, ActionResultS2C.ok());
		});

		registerGuarded(registrar, PartyAllyAddC2S.TYPE, PartyAllyAddC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();

			Optional<PartyData> maybeParty = requirePartyLeader(player.getUUID());
			if (maybeParty.isEmpty()) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(notLeaderReason(player.getUUID())));
				return;
			}
			PartyData party = maybeParty.get();

			Optional<PartyData> maybeTarget = IslandCoreMod.PARTY_REGISTRY.getPartyByName(payload.targetPartyName());
			if (maybeTarget.isEmpty()) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.PARTY_NOT_FOUND));
				return;
			}
			PartyData target = maybeTarget.get();

			if (target.getPartyId().equals(party.getPartyId())) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.PARTY_ALLY_SELF));
				return;
			}

			// Same entry point "/party ally add" calls.
			IslandCoreMod.PARTY_REGISTRY.addAlly(party.getPartyId(), target.getPartyId());
			PacketDistributor.sendToPlayer(player, ActionResultS2C.ok());
		});

		registerGuarded(registrar, PartyAllyRemoveC2S.TYPE, PartyAllyRemoveC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();

			Optional<PartyData> maybeParty = requirePartyLeader(player.getUUID());
			if (maybeParty.isEmpty()) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(notLeaderReason(player.getUUID())));
				return;
			}
			PartyData party = maybeParty.get();

			Optional<PartyData> maybeTarget = IslandCoreMod.PARTY_REGISTRY.getPartyByName(payload.targetPartyName());
			if (maybeTarget.isEmpty()) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.PARTY_NOT_FOUND));
				return;
			}
			PartyData target = maybeTarget.get();

			// Same entry point "/party ally remove" calls.
			IslandCoreMod.PARTY_REGISTRY.removeAlly(party.getPartyId(), target.getPartyId());
			PacketDistributor.sendToPlayer(player, ActionResultS2C.ok());
		});
	}

	// Shared by every leader-only party handler above: returns the sender's party if they're its
	// leader, empty otherwise — caller then uses notLeaderReason(...) to report NO_PARTY vs
	// NOT_PARTY_LEADER. Mirrors PartyCommand#requireLeaderOf.
	private static Optional<PartyData> requirePartyLeader(UUID playerUuid) {
		Optional<PartyData> maybeParty = IslandCoreMod.PARTY_REGISTRY.getPartyOf(playerUuid);
		if (maybeParty.isEmpty() || !maybeParty.get().getLeaderUuid().equals(playerUuid)) {
			return Optional.empty();
		}
		return maybeParty;
	}

	private static String notLeaderReason(UUID playerUuid) {
		return IslandCoreMod.PARTY_REGISTRY.getPartyOf(playerUuid).isEmpty() ? ActionReason.NO_PARTY : ActionReason.NOT_PARTY_LEADER;
	}

	// Admin network block: island list/detail/delete, Spawn management, Dimension Manager, vanilla
	// reset queue. Every handler below starts with the same operator check (mirrors DimensionCommand
	// and IslandCommand's "admin" subtree, both .requires(source -> source.hasPermission(2))),
	// since a network payload has no Brigadier .requires(...) gate to reject a non-operator sender
	// before the handler even runs.

	private static final ResourceKey<Level> ISLANDS_DIMENSION = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("islandcore", "islands"));

	private static boolean rejectIfNotOperator(ServerPlayer player) {
		if (player.hasPermissions(2)) {
			return false;
		}
		PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.NOT_OPERATOR));
		return true;
	}

	private static void registerAdminIslandHandlers(PayloadRegistrar registrar) {
		registerGuarded(registrar, AdminIslandListRequestC2S.TYPE, AdminIslandListRequestC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			AdminIslandListS2C response = AdminIslandBuilder.buildList(context.server(),
					IslandCoreMod.ISLAND_REGISTRY.getAllIslands(), payload.page(), payload.pageSize(), payload.searchQuery());
			PacketDistributor.sendToPlayer(player, response);
		});

		registerGuarded(registrar, AdminIslandDetailRequestC2S.TYPE, AdminIslandDetailRequestC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(payload.targetUuid());
			if (maybeIsland.isEmpty()) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.ISLAND_NOT_FOUND));
				return;
			}

			PacketDistributor.sendToPlayer(player, AdminIslandBuilder.buildDetail(context.server(), maybeIsland.get()));
		});

		registerGuarded(registrar, AdminIslandDeleteC2S.TYPE, AdminIslandDeleteC2S.CODEC, (payload, context) -> {
			ServerPlayer admin = context.player();
			if (rejectIfNotOperator(admin)) {
				return;
			}

			Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(payload.targetUuid());
			if (maybeIsland.isEmpty()) {
				PacketDistributor.sendToPlayer(admin, ActionResultS2C.fail(ActionReason.ISLAND_NOT_FOUND));
				return;
			}

			try {
				IslandCoreMod.DELETION_SERVICE.requestDeletion(maybeIsland.get().getIslandId(), admin.getUUID());
			} catch (IllegalStateException e) {
				PacketDistributor.sendToPlayer(admin, ActionResultS2C.fail(ActionReason.ISLAND_ALREADY_DELETING));
				return;
			}

			PacketDistributor.sendToPlayer(admin, ActionResultS2C.ok());
		});

		registerGuarded(registrar, AdminIslandDeleteConfirmC2S.TYPE, AdminIslandDeleteConfirmC2S.CODEC, (payload, context) -> {
			ServerPlayer admin = context.player();
			if (rejectIfNotOperator(admin)) {
				return;
			}

			Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(payload.targetUuid());
			if (maybeIsland.isEmpty()) {
				PacketDistributor.sendToPlayer(admin, ActionResultS2C.fail(ActionReason.ISLAND_NOT_FOUND));
				return;
			}

			boolean confirmed = IslandCoreMod.DELETION_SERVICE.confirmDeletion(maybeIsland.get().getIslandId(), admin.getUUID());
			PacketDistributor.sendToPlayer(admin, confirmed
					? ActionResultS2C.ok()
					: ActionResultS2C.fail(ActionReason.NO_PENDING_CONFIRMATION));
		});
	}

	private static void registerSpawnAdminHandlers(PayloadRegistrar registrar) {
		registerGuarded(registrar, SpawnStatusRequestC2S.TYPE, SpawnStatusRequestC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			SpawnStatusS2C response = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(Island.SERVER_OWNER_UUID)
					.map(island -> new SpawnStatusS2C(true, island.getIslandSize(), Optional.of(island.getHomeLocation())))
					.orElseGet(SpawnStatusS2C::absent);
			PacketDistributor.sendToPlayer(player, response);
		});

		registerGuarded(registrar, SpawnIslandCreateC2S.TYPE, SpawnIslandCreateC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			try {
				IslandCoreMod.ISLAND_REGISTRY.createSpawnIsland(ISLANDS_DIMENSION, payload.size());
			} catch (IllegalStateException e) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.SPAWN_ISLAND_ALREADY_EXISTS));
				return;
			}

			PacketDistributor.sendToPlayer(player, ActionResultS2C.ok());
		});

		registerGuarded(registrar, SpawnIslandResizeC2S.TYPE, SpawnIslandResizeC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(Island.SERVER_OWNER_UUID);
			if (maybeIsland.isEmpty()) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.SPAWN_ISLAND_NOT_FOUND));
				return;
			}

			try {
				IslandCoreMod.ISLAND_REGISTRY.resizeIsland(maybeIsland.get().getIslandId(), payload.newSize());
			} catch (IllegalArgumentException e) {
				// resizeIsland only ever rejects for the same reason /island upgrade's own size cap
				// does (requested size doesn't fit the allowed/reserved plot) — no dedicated admin
				// key was specified for this, so this reuses the closest existing semantic match.
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.PLOT_SIZE_EXCEEDED));
				return;
			}

			PacketDistributor.sendToPlayer(player, ActionResultS2C.ok());
		});

		registerGuarded(registrar, SpawnIslandSetHomeC2S.TYPE, SpawnIslandSetHomeC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(Island.SERVER_OWNER_UUID);
			if (maybeIsland.isEmpty()) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.SPAWN_ISLAND_NOT_FOUND));
				return;
			}

			Island island = maybeIsland.get();
			BlockPos pos = player.blockPosition();
			boolean inIslandsDimension = player.level().dimension().equals(ISLANDS_DIMENSION);
			boolean withinBuiltIsland = island.getBounds().contains(pos);
			if (!inIslandsDimension || !withinBuiltIsland) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.UNSAFE_LOCATION));
				return;
			}

			IslandCoreMod.ISLAND_REGISTRY.updateHomeLocation(island.getIslandId(), pos);
			PacketDistributor.sendToPlayer(player, ActionResultS2C.ok());
		});

		registerGuarded(registrar, SpawnBuildProtectionStatusRequestC2S.TYPE, SpawnBuildProtectionStatusRequestC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			SpawnBuildProtectionStatusS2C response = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(Island.SERVER_OWNER_UUID)
					.map(island -> new SpawnBuildProtectionStatusS2C(
							island.getSetting(IslandSetting.BUILD_PROTECTION),
							buildAuthorizedPlayers(context.server(), island)))
					.orElseGet(SpawnBuildProtectionStatusS2C::absent);
			PacketDistributor.sendToPlayer(player, response);
		});

		registerGuarded(registrar, SpawnBuildProtectionSetC2S.TYPE, SpawnBuildProtectionSetC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(Island.SERVER_OWNER_UUID);
			if (maybeIsland.isEmpty()) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.SPAWN_ISLAND_NOT_FOUND));
				return;
			}

			IslandCoreMod.ISLAND_REGISTRY.updateIslandSetting(
					maybeIsland.get().getIslandId(), IslandSetting.BUILD_PROTECTION, payload.enabled());
			PacketDistributor.sendToPlayer(player, ActionResultS2C.ok());
		});

		registerGuarded(registrar, SpawnAuthorizedPlayerAddC2S.TYPE, SpawnAuthorizedPlayerAddC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(Island.SERVER_OWNER_UUID);
			if (maybeIsland.isEmpty()) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.SPAWN_ISLAND_NOT_FOUND));
				return;
			}

			Optional<UUID> targetUuid = MembershipService.resolvePlayerUuid(payload.targetName(), context.server());
			if (targetUuid.isEmpty()) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.TARGET_NOT_FOUND));
				return;
			}

			MembershipService.trustOnIsland(maybeIsland.get(), targetUuid.get());
			PacketDistributor.sendToPlayer(player, ActionResultS2C.ok());
		});

		registerGuarded(registrar, SpawnAuthorizedPlayerRemoveC2S.TYPE, SpawnAuthorizedPlayerRemoveC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(Island.SERVER_OWNER_UUID);
			if (maybeIsland.isEmpty()) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.SPAWN_ISLAND_NOT_FOUND));
				return;
			}

			MembershipService.untrustOnIsland(maybeIsland.get(), payload.targetUuid());
			PacketDistributor.sendToPlayer(player, ActionResultS2C.ok());
		});
	}

	// Same MEMBER/CO_OWNER filter AdminIslandBuilder#countMembers and IslandSnapshotBuilder already
	// apply elsewhere: the owner (here, the synthetic Island.SERVER_OWNER_UUID — not a real player)
	// is never included.
	private static List<SpawnBuildProtectionStatusS2C.AuthorizedPlayerEntry> buildAuthorizedPlayers(MinecraftServer server, Island island) {
		List<SpawnBuildProtectionStatusS2C.AuthorizedPlayerEntry> entries = new ArrayList<>();
		for (IslandMember member : island.getMembers()) {
			if (member.role() != IslandRole.MEMBER && member.role() != IslandRole.CO_OWNER) {
				continue;
			}
			entries.add(new SpawnBuildProtectionStatusS2C.AuthorizedPlayerEntry(
					member.playerUuid(), resolvePlayerName(server, member.playerUuid()), member.role().name()));
		}
		return entries;
	}

	private static String resolvePlayerName(MinecraftServer server, UUID playerUuid) {
		return server.getProfileCache().get(playerUuid).map(GameProfile::getName).orElse(playerUuid.toString());
	}

	private static void registerDimensionAdminHandlers(PayloadRegistrar registrar) {
		registerGuarded(registrar, DimensionListRequestC2S.TYPE, DimensionListRequestC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			List<DimensionListS2C.DimensionEntry> entries = new ArrayList<>();
			for (DimensionDefinition dimension : IslandCoreMod.DIMENSION_REGISTRY.getAllDimensions()) {
				entries.add(DimensionAdminBuilder.buildEntry(dimension));
			}
			PacketDistributor.sendToPlayer(player, new DimensionListS2C(entries));
		});

		registerGuarded(registrar, DimensionDetailRequestC2S.TYPE, DimensionDetailRequestC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			Optional<ResourceLocation> maybeId = parseDimensionId(payload.id());
			Optional<DimensionDefinition> maybeDimension = maybeId.flatMap(IslandCoreMod.DIMENSION_REGISTRY::getDimension);
			if (maybeDimension.isEmpty()) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.DIMENSION_NOT_FOUND));
				return;
			}

			PacketDistributor.sendToPlayer(player, DimensionAdminBuilder.buildDetail(maybeDimension.get()));
		});

		registerGuarded(registrar, DimensionCreateC2S.TYPE, DimensionCreateC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			DimensionGeneratorStyle style;
			try {
				style = DimensionGeneratorStyle.valueOf(payload.style().toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException e) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.INVALID_STYLE));
				return;
			}

			Optional<ResourceLocation> maybeId = parseDimensionId(payload.id());
			if (maybeId.isEmpty()) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.INVALID_DIMENSION_ID));
				return;
			}

			long seed = payload.seed().orElseGet(() -> new Random().nextLong());

			try {
				IslandCoreMod.DIMENSION_REGISTRY.createDimension(maybeId.get(), payload.displayName(), style, seed);
			} catch (IllegalStateException e) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.DIMENSION_ALREADY_EXISTS));
				return;
			}

			PacketDistributor.sendToPlayer(player, ActionResultS2C.ok());
		});

		registerGuarded(registrar, DimensionDeleteC2S.TYPE, DimensionDeleteC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			Optional<ResourceLocation> maybeId = parseDimensionId(payload.id());
			if (maybeId.isEmpty()) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.INVALID_DIMENSION_ID));
				return;
			}

			try {
				IslandCoreMod.DIMENSION_REGISTRY.requestDeletion(maybeId.get(), player.getUUID());
			} catch (IllegalArgumentException e) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.DIMENSION_NOT_FOUND));
				return;
			} catch (IllegalStateException e) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.DIMENSION_STATE_CONFLICT));
				return;
			}

			PacketDistributor.sendToPlayer(player, ActionResultS2C.ok());
		});

		registerGuarded(registrar, DimensionDeleteConfirmC2S.TYPE, DimensionDeleteConfirmC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			Optional<ResourceLocation> maybeId = parseDimensionId(payload.id());
			if (maybeId.isEmpty()) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.INVALID_DIMENSION_ID));
				return;
			}

			boolean confirmed = IslandCoreMod.DIMENSION_REGISTRY.confirmDeletion(maybeId.get(), player.getUUID());
			PacketDistributor.sendToPlayer(player, confirmed
					? ActionResultS2C.ok()
					: ActionResultS2C.fail(ActionReason.NO_PENDING_CONFIRMATION));
		});

		registerGuarded(registrar, DimensionRegenerateC2S.TYPE, DimensionRegenerateC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			Optional<ResourceLocation> maybeId = parseDimensionId(payload.id());
			if (maybeId.isEmpty()) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.INVALID_DIMENSION_ID));
				return;
			}

			long newSeed = payload.seed().orElseGet(() -> new Random().nextLong());

			try {
				IslandCoreMod.DIMENSION_REGISTRY.requestRegeneration(maybeId.get(), player.getUUID(), newSeed);
			} catch (IllegalArgumentException e) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.DIMENSION_NOT_FOUND));
				return;
			} catch (IllegalStateException e) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.DIMENSION_STATE_CONFLICT));
				return;
			}

			PacketDistributor.sendToPlayer(player, ActionResultS2C.ok());
		});

		registerGuarded(registrar, DimensionRegenerateConfirmC2S.TYPE, DimensionRegenerateConfirmC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			Optional<ResourceLocation> maybeId = parseDimensionId(payload.id());
			if (maybeId.isEmpty()) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.INVALID_DIMENSION_ID));
				return;
			}

			boolean confirmed = IslandCoreMod.DIMENSION_REGISTRY.confirmRegeneration(maybeId.get(), player.getUUID());
			PacketDistributor.sendToPlayer(player, confirmed
					? ActionResultS2C.ok()
					: ActionResultS2C.fail(ActionReason.NO_PENDING_CONFIRMATION));
		});
	}

	// ResourceLocation.of(...) throws on a malformed string, same reasoning as parseBiomeId above: the
	// text command never hits this path because its Brigadier argument type already validates the
	// format, but a network payload's raw String field has no such guard.
	private static Optional<ResourceLocation> parseDimensionId(String idPath) {
		try {
			return Optional.of(ResourceLocation.fromNamespaceAndPath("islandcore", idPath));
		} catch (RuntimeException e) {
			return Optional.empty();
		}
	}

	private static void registerVanillaResetAdminHandlers(PayloadRegistrar registrar) {
		registerGuarded(registrar, VanillaResetListRequestC2S.TYPE, VanillaResetListRequestC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			List<VanillaResetListS2C.QueueEntry> entries = new ArrayList<>();
			for (PendingVanillaReset pending : IslandCoreMod.VANILLA_RESET_SERVICE.listPendingResets()) {
				entries.add(new VanillaResetListS2C.QueueEntry(
						pending.getDimensionKey(), Optional.ofNullable(pending.getSeed()), pending.getSeedMode().name(),
						pending.getRequestedBy(), pending.getStatus().name()));
			}
			PacketDistributor.sendToPlayer(player, new VanillaResetListS2C(entries));
		});

		registerGuarded(registrar, VanillaResetQueueC2S.TYPE, VanillaResetQueueC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			boolean alreadyQueued = IslandCoreMod.VANILLA_RESET_SERVICE.listPendingResets().stream()
					.anyMatch(entry -> entry.getDimensionKey().equals(payload.dimension()));
			if (alreadyQueued) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.VANILLA_RESET_ALREADY_QUEUED));
				return;
			}

			Long explicitSeed = "CUSTOM".equals(payload.seedMode()) ? payload.seedValue().orElse(null) : null;

			try {
				IslandCoreMod.VANILLA_RESET_SERVICE.requestReset(payload.dimension(), player.getUUID(), explicitSeed);
			} catch (IllegalArgumentException e) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.VANILLA_DIMENSION_INVALID));
				return;
			}

			PacketDistributor.sendToPlayer(player, ActionResultS2C.ok());
		});

		registerGuarded(registrar, VanillaResetConfirmC2S.TYPE, VanillaResetConfirmC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			boolean confirmed;
			try {
				confirmed = IslandCoreMod.VANILLA_RESET_SERVICE.confirmReset(payload.dimension(), player.getUUID());
			} catch (IllegalArgumentException e) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.VANILLA_DIMENSION_INVALID));
				return;
			}

			PacketDistributor.sendToPlayer(player, confirmed
					? ActionResultS2C.ok()
					: ActionResultS2C.fail(ActionReason.NO_PENDING_CONFIRMATION));
		});

		registerGuarded(registrar, VanillaResetCancelC2S.TYPE, VanillaResetCancelC2S.CODEC, (payload, context) -> {
			ServerPlayer player = context.player();
			if (rejectIfNotOperator(player)) {
				return;
			}

			boolean cancelled;
			try {
				cancelled = IslandCoreMod.VANILLA_RESET_SERVICE.cancelPendingReset(payload.dimension());
			} catch (IllegalArgumentException e) {
				PacketDistributor.sendToPlayer(player, ActionResultS2C.fail(ActionReason.VANILLA_DIMENSION_INVALID));
				return;
			}

			// Not the same 30s confirm window as NO_PENDING_CONFIRMATION's other uses (this cancels
			// an already-confirmed, QUEUED entry) but no dedicated key was specified for "nothing
			// queued to cancel", so this reuses the closest existing semantic match.
			PacketDistributor.sendToPlayer(player, cancelled
					? ActionResultS2C.ok()
					: ActionResultS2C.fail(ActionReason.NO_PENDING_CONFIRMATION));
		});
	}
}
