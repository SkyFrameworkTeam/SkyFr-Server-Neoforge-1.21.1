package com.skyframework.islandcore;

import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.api.permission.PermissionProvider;
import com.skyframework.islandcore.api.registry.IslandRegistryApi;
import com.skyframework.islandcore.command.DimensionCommand;
import com.skyframework.islandcore.command.IslandCommand;
import com.skyframework.islandcore.command.PartyCommand;
import com.skyframework.islandcore.dimension.registry.DimensionRegistry;
import com.skyframework.islandcore.dimension.registry.DimensionRegistryImpl;
import com.skyframework.islandcore.dimension.runtime.NativeDimensionRuntimeProvider;
import com.skyframework.islandcore.dimension.vanilla.VanillaResetConfig;
import com.skyframework.islandcore.dimension.vanilla.VanillaResetExecutor;
import com.skyframework.islandcore.dimension.vanilla.VanillaResetService;
import com.skyframework.islandcore.farming.FarmingCommand;
import com.skyframework.islandcore.farming.FarmingConfig;
import com.skyframework.islandcore.island.biome.BiomeTierRegistry;
import com.skyframework.islandcore.island.biome.BiomeTierRegistryImpl;
import com.skyframework.islandcore.island.biome.IslandBiomeApplier;
import com.skyframework.islandcore.island.entity.IslandEntityTracker;
import com.skyframework.islandcore.island.entity.IslandEntityTrackerImpl;
import com.skyframework.islandcore.island.lifecycle.IslandDeletionService;
import com.skyframework.islandcore.island.lifecycle.IslandDeletionServiceImpl;
import com.skyframework.islandcore.island.lifecycle.InviteManager;
import com.skyframework.islandcore.island.lifecycle.InviteManagerImpl;
import com.skyframework.islandcore.island.registry.IslandRegistryImpl;
import com.skyframework.islandcore.net.ClientSyncNotifier;
import com.skyframework.islandcore.net.ServerPacketHandlers;
import com.skyframework.islandcore.party.lifecycle.PartyInviteManager;
import com.skyframework.islandcore.party.lifecycle.PartyInviteManagerImpl;
import com.skyframework.islandcore.party.registry.PartyRegistry;
import com.skyframework.islandcore.party.registry.PartyRegistryImpl;
import com.skyframework.islandcore.permission.FallbackPermissionProvider;
import com.skyframework.islandcore.permission.LuckPermsProvider;
import com.skyframework.islandcore.player.FirstJoinTracker;
import com.skyframework.islandcore.player.StarterKitConfig;
import com.skyframework.islandcore.player.rescue.VoidRescueConfig;
import com.skyframework.islandcore.player.rescue.VoidRescueListener;
import com.skyframework.islandcore.portal.PortalLinkConfig;
import com.skyframework.islandcore.protection.AccessController;
import com.skyframework.islandcore.protection.AccessControllerImpl;
import com.skyframework.islandcore.protection.DamageProtectionListener;
import com.skyframework.islandcore.protection.DeniedActionThrottler;
import com.skyframework.islandcore.protection.ProtectionListeners;
import com.skyframework.islandcore.protection.exception.ExceptionGroupRegistry;
import com.skyframework.islandcore.protection.exception.ServerExceptionDefaults;
import com.skyframework.islandcore.protection.flag.FlagPermissionRequirements;
import com.skyframework.islandcore.protection.flag.ServerFlagDefaults;
import com.skyframework.islandcore.rtp.RtpCommand;
import com.skyframework.islandcore.rtp.RtpConfig;
import com.skyframework.islandcore.spawn.SpawnCommand;
import com.skyframework.islandcore.spawn.SpawnConfig;
import com.skyframework.islandcore.teleport.TeleportManager;
import com.skyframework.islandcore.teleport.TeleportManagerImpl;
import com.skyframework.islandcore.teleport.VanillaTeleportBackend;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

// NEOFORGE PORT STATUS: the island/protection/party/config/teleport/dimension/network core is
// fully ported and wired below, including a from-scratch native NeoForge replacement for the
// Fabric-only Fantasy library (see NativeDimensionRuntimeProvider) and the full ~88-file network
// payload layer (ServerPacketHandlers, rebuilt around RegisterPayloadHandlersEvent/PayloadRegistrar
// instead of Fabric's ServerPlayNetworking). Still TODO: the 9 Mixins declared in
// islandcore.mixins.json (their .java files aren't ported yet). That doesn't block compiling or
// running what IS wired below; only the client-side mod (SkyFr-Cliente-Neoforge) is still an empty
// shell.
@Mod(IslandCoreMod.MOD_ID)
public class IslandCoreMod {
	public static final String MOD_ID = "islandcore";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	// Static access is temporary for this phase of development, mirroring the Fabric project's own
	// documented stance on it — will be revisited once a proper dependency-injection/
	// service-locator pattern is needed.
	public static IslandRegistryApi ISLAND_REGISTRY;
	public static AccessController ACCESS_CONTROLLER;
	public static PermissionProvider PERMISSION_PROVIDER;
	public static IslandDeletionService DELETION_SERVICE;
	public static InviteManager INVITE_MANAGER;
	public static BiomeTierRegistry BIOME_TIER_REGISTRY;
	public static IslandBiomeApplier BIOME_APPLIER;
	public static IslandEntityTracker ENTITY_TRACKER;
	public static StarterKitConfig STARTER_KIT_CONFIG;
	public static VoidRescueConfig VOID_RESCUE_CONFIG;
	public static RtpConfig RTP_CONFIG;
	public static SpawnConfig SPAWN_CONFIG;
	public static PortalLinkConfig PORTAL_LINK_CONFIG;
	public static VanillaResetConfig VANILLA_RESET_CONFIG;
	public static FarmingConfig FARMING_CONFIG;
	public static ServerFlagDefaults SERVER_FLAG_DEFAULTS;
	public static ExceptionGroupRegistry EXCEPTION_GROUP_REGISTRY;
	public static ServerExceptionDefaults SERVER_EXCEPTION_DEFAULTS;
	public static FlagPermissionRequirements FLAG_PERMISSION_REQUIREMENTS;
	public static PartyRegistry PARTY_REGISTRY;
	public static PartyInviteManager PARTY_INVITE_MANAGER;
	public static TeleportManager TELEPORT_MANAGER;
	public static DimensionRegistry DIMENSION_REGISTRY;
	public static VanillaResetService VANILLA_RESET_SERVICE;
	public static FirstJoinTracker FIRST_JOIN_TRACKER;

	// NeoForge constructs the @Mod class itself and passes the mod event bus (registry/lifecycle
	// events) — this replaces Fabric's ModInitializer#onInitialize() as the single entry point.
	public IslandCoreMod(IEventBus modEventBus, ModContainer modContainer) {
		// Must run before anything else: mirrors the Fabric project's own placement (see its
		// class-level comment) — the earliest safe moment to apply a vanilla dimension reset
		// requested during the previous run, before server.properties/level.dat are even read.
		VanillaResetExecutor.executeIfPending();

		ISLAND_REGISTRY = new IslandRegistryImpl();
		ACCESS_CONTROLLER = new AccessControllerImpl();
		INVITE_MANAGER = new InviteManagerImpl();
		BIOME_TIER_REGISTRY = new BiomeTierRegistryImpl();
		BIOME_APPLIER = new IslandBiomeApplier();
		ENTITY_TRACKER = new IslandEntityTrackerImpl();
		DIMENSION_REGISTRY = new DimensionRegistryImpl(new NativeDimensionRuntimeProvider(), new VanillaTeleportBackend());
		FIRST_JOIN_TRACKER = new FirstJoinTracker();
		ClientSyncNotifier.register();
		modEventBus.addListener(ServerPacketHandlers::register);
		STARTER_KIT_CONFIG = new StarterKitConfig();
		VOID_RESCUE_CONFIG = new VoidRescueConfig();
		RTP_CONFIG = new RtpConfig();
		SPAWN_CONFIG = new SpawnConfig();
		PORTAL_LINK_CONFIG = new PortalLinkConfig();
		VANILLA_RESET_CONFIG = new VanillaResetConfig();
		VANILLA_RESET_SERVICE = new VanillaResetService(new VanillaTeleportBackend());
		NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> VANILLA_RESET_SERVICE.tickAll());
		FARMING_CONFIG = new FarmingConfig();
		SERVER_FLAG_DEFAULTS = new ServerFlagDefaults();
		EXCEPTION_GROUP_REGISTRY = new ExceptionGroupRegistry();
		SERVER_EXCEPTION_DEFAULTS = new ServerExceptionDefaults();
		FLAG_PERMISSION_REQUIREMENTS = new FlagPermissionRequirements();
		// Independent of ISLAND_REGISTRY (see PartyRegistryImpl's class comment): its own
		// self-contained ServerStartedEvent hook loads party storage, same pattern as the other
		// registries above.
		PARTY_REGISTRY = new PartyRegistryImpl();
		PARTY_INVITE_MANAGER = new PartyInviteManagerImpl();

		ProtectionListeners.register();
		DeniedActionThrottler.register();
		IslandCommand.register();
		DimensionCommand.register();
		PartyCommand.register();
		RtpCommand.register();
		SpawnCommand.register();
		FarmingCommand.register();

		if (ModList.get().isLoaded("luckperms")) {
			PERMISSION_PROVIDER = new LuckPermsProvider();
			LOGGER.info("LuckPerms detected: using LuckPermsProvider for permission checks.");
		} else {
			PERMISSION_PROVIDER = new FallbackPermissionProvider();
			LOGGER.info("LuckPerms not found: using FallbackPermissionProvider (permissions default to false).");
		}

		// Extra safety net on shutdown; individual mutations already persist themselves.
		NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppingEvent event) -> {
			ISLAND_REGISTRY.saveAll();
			PARTY_REGISTRY.saveAll();
		});

		// DELETION_SERVICE must exist before the server actually starts: IslandRegistryImpl's
		// ServerStartedEvent listener calls DELETION_SERVICE.executeDeletion() to resume any
		// deletion interrupted by a previous shutdown, and mod-constructor code (here) always runs
		// before any ServerStartedEvent fires.
		DELETION_SERVICE = new IslandDeletionServiceImpl(new VanillaTeleportBackend());
		NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> DELETION_SERVICE.tickAll());

		TELEPORT_MANAGER = new TeleportManagerImpl(new VanillaTeleportBackend());
		NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> TELEPORT_MANAGER.tickAll());

		NeoForge.EVENT_BUS.addListener((LivingIncomingDamageEvent event) -> {
			if (event.getEntity() instanceof ServerPlayer player) {
				TELEPORT_MANAGER.cancelPendingTeleport(player.getUUID(), "has recibido daño");
			}
		});
		// A second, independent listener on the same event: NeoForge invokes every registered
		// listener regardless of an earlier one's outcome (unlike a boolean-AND callback chain), so
		// this doesn't need to be merged with the teleport-cancellation listener above — it just
		// cancels the event itself when the protection rules say so.
		NeoForge.EVENT_BUS.addListener((LivingIncomingDamageEvent event) -> {
			if (!DamageProtectionListener.isDamageAllowed(event.getEntity().level(), event.getEntity(), event.getSource())) {
				event.setCanceled(true);
			}
		});
		// A third, independent listener: emergency void-rescue teleport, unrelated to the
		// protection rules above (which only ever look at attacker permissions/settings, not the
		// no-attacker fall-out-of-world case this handles).
		NeoForge.EVENT_BUS.addListener((LivingIncomingDamageEvent event) -> {
			if (!VoidRescueListener.isDamageAllowed(event.getEntity(), event.getSource())) {
				event.setCanceled(true);
			}
		});
		// Second, independent void-rescue detection path: a periodic Y-position check that catches
		// Creative-mode players, who are invulnerable to OUT_OF_WORLD damage and so never trigger
		// the damage event above — see VoidRescueListener's class javadoc.
		NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> VoidRescueListener.tickAll(event.getServer()));

		// Welcome teleport for brand-new players by default; reconnecting players keep vanilla's
		// normal "reappear where you left off" behavior UNLESS SpawnConfig.alwaysRespawnOnDisconnect
		// is true, in which case every connection (not just the first) gets teleported, but only the
		// actual first join ever gets the welcome message. Silently does nothing if the Spawn island
		// hasn't been created yet (no /island admin spawn create run) — vanilla Overworld spawn is
		// fine then. isFirstJoin() is called unconditionally: it also records the player as known,
		// which must happen on every join regardless of the flag below.
		NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
			ServerPlayer player = (ServerPlayer) event.getEntity();
			boolean firstJoin = FIRST_JOIN_TRACKER.isFirstJoin(player.getUUID());

			// Independent of the welcome teleport below (which silently no-ops without a Spawn
			// island): the starter kit is about the player, not the island system, so it's given
			// on every actual first join regardless of whether a Spawn island exists yet.
			if (firstJoin) {
				giveStarterKit(player);
			}

			if (!firstJoin && !SPAWN_CONFIG.isAlwaysRespawnOnDisconnect()) {
				return;
			}

			ISLAND_REGISTRY.getIslandByOwner(Island.SERVER_OWNER_UUID).ifPresent(spawnIsland -> {
				ServerLevel world = player.getServer().getLevel(spawnIsland.getDimension());
				if (world == null) {
					return;
				}

				boolean teleported = new VanillaTeleportBackend().teleport(player, world, spawnIsland.getHomeLocation());
				if (teleported && firstJoin) {
					player.sendSystemMessage(Component.literal("¡Bienvenido a SkyFramework! Usa /island create para crear tu propia isla."));
				}
			});
		});

		LOGGER.info("IslandCore (NeoForge port) initialized — core island/protection/party layer only, see class javadoc for what's still pending.");
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}

	// Skips (with a log warning) any entry whose item id doesn't resolve, rather than failing the
	// whole welcome flow over one bad config entry.
	private static void giveStarterKit(ServerPlayer player) {
		for (StarterKitConfig.ItemStackDefinition definition : STARTER_KIT_CONFIG.getItems()) {
			ResourceLocation itemId;
			try {
				itemId = ResourceLocation.parse(definition.item());
			} catch (RuntimeException e) {
				LOGGER.error("Skipping invalid starter kit item id: {}", definition.item(), e);
				continue;
			}

			Optional<Item> maybeItem = BuiltInRegistries.ITEM.getOptional(itemId);
			if (maybeItem.isEmpty()) {
				LOGGER.error("Skipping unknown starter kit item id: {}", definition.item());
				continue;
			}

			player.getInventory().add(new ItemStack(maybeItem.get(), definition.count()));
		}
	}
}
