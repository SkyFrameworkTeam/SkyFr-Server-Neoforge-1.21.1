package com.skyframework.islandcore.net;

import net.minecraft.resources.ResourceLocation;

// Single source of truth for every "islandcore" custom payload channel. Handshake identifiers
// here MUST match IslandCoreClient's network.handshake.ClientHandshakeC2S/ServerHandshakeS2C
// exactly (that mod hardcodes them directly, with no shared module between the two projects) —
// this is the reason both sides live under the "islandcore" namespace rather than
// "islandcoreclient": the server owns this protocol.
public final class NetworkChannels {

	// Bump this by 1 every time the binary format of ANY existing payload in this package changes —
	// a field added/removed/reordered, a type swapped, anything that changes what bytes go on the
	// wire (e.g. IslandSnapshotS2C's 11 -> 13 field change). Do it in the SAME change that alters
	// the format, not as an afterthought. ClientHandshakeC2S/ServerHandshakeS2C carry this value so
	// each side can tell whether the other was built against the wire format it expects — see
	// ServerHandshakeS2C#protocolCompatible. Bumping it server-side only helps if the client's own
	// mirror constant (network.handshake package there) is bumped to match in the same release;
	// announce that in the client's chat/changelog whenever this changes, since nothing here can
	// notify that codebase automatically.
	//
	// Exception: this constant cannot protect ClientHandshakeC2S/ServerHandshakeS2C's OWN format
	// (that would require decoding the payload before knowing whether it's safe to decode it) — a
	// change to the handshake payloads themselves still requires shipping server and client
	// together, the same as every codec change in this project always has.
	public static final int PROTOCOL_VERSION = 6;

	public static final ResourceLocation HANDSHAKE_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "handshake_c2s");
	public static final ResourceLocation HANDSHAKE_S2C = ResourceLocation.fromNamespaceAndPath("islandcore", "handshake_s2c");

	public static final ResourceLocation ISLAND_SNAPSHOT_REQUEST_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "island_snapshot_request_c2s");
	public static final ResourceLocation ISLAND_SNAPSHOT_S2C = ResourceLocation.fromNamespaceAndPath("islandcore", "island_snapshot_s2c");

	// Sprint "acciones de isla":

	public static final ResourceLocation ACTION_RESULT_S2C = ResourceLocation.fromNamespaceAndPath("islandcore", "action_result_s2c");
	public static final ResourceLocation PENDING_CONFIRMATION_TICK_S2C = ResourceLocation.fromNamespaceAndPath("islandcore", "pending_confirmation_tick_s2c");

	public static final ResourceLocation ISLAND_CREATE_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "island_create_c2s");
	public static final ResourceLocation ISLAND_UPGRADE_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "island_upgrade_c2s");
	public static final ResourceLocation ISLAND_DELETE_REQUEST_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "island_delete_request_c2s");
	public static final ResourceLocation ISLAND_DELETE_CONFIRM_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "island_delete_confirm_c2s");
	public static final ResourceLocation ISLAND_SETTINGS_UPDATE_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "island_settings_update_c2s");
	public static final ResourceLocation ISLAND_BIOME_CHANGE_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "island_biome_change_c2s");

	public static final ResourceLocation MEMBER_INVITE_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "member_invite_c2s");
	public static final ResourceLocation MEMBER_INVITE_ACCEPT_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "member_invite_accept_c2s");
	public static final ResourceLocation MEMBER_INVITE_DECLINE_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "member_invite_decline_c2s");
	public static final ResourceLocation MEMBER_TRUST_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "member_trust_c2s");
	public static final ResourceLocation MEMBER_REMOVE_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "member_remove_c2s");
	public static final ResourceLocation MEMBER_ALLY_ADD_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "member_ally_add_c2s");
	public static final ResourceLocation MEMBER_ALLY_REMOVE_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "member_ally_remove_c2s");

	public static final ResourceLocation TELEPORT_REQUEST_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "teleport_request_c2s");
	public static final ResourceLocation TELEPORT_STATUS_REQUEST_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "teleport_status_request_c2s");
	public static final ResourceLocation TELEPORT_STATUS_S2C = ResourceLocation.fromNamespaceAndPath("islandcore", "teleport_status_s2c");

	public static final ResourceLocation BIOME_TIERS_REQUEST_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "biome_tiers_request_c2s");
	public static final ResourceLocation BIOME_TIERS_S2C = ResourceLocation.fromNamespaceAndPath("islandcore", "biome_tiers_s2c");

	// Admin network block: island list/detail/delete, Spawn management, Dimension Manager, vanilla
	// reset queue. All require the sender to be a server operator (see ActionReason's admin block
	// comment for the "error." prefix reason keys these use on failure).

	public static final ResourceLocation ADMIN_ISLAND_LIST_REQUEST_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "admin_island_list_request_c2s");
	public static final ResourceLocation ADMIN_ISLAND_LIST_S2C = ResourceLocation.fromNamespaceAndPath("islandcore", "admin_island_list_s2c");
	public static final ResourceLocation ADMIN_ISLAND_DETAIL_REQUEST_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "admin_island_detail_request_c2s");
	public static final ResourceLocation ADMIN_ISLAND_DETAIL_S2C = ResourceLocation.fromNamespaceAndPath("islandcore", "admin_island_detail_s2c");
	public static final ResourceLocation ADMIN_ISLAND_DELETE_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "admin_island_delete_c2s");
	public static final ResourceLocation ADMIN_ISLAND_DELETE_CONFIRM_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "admin_island_delete_confirm_c2s");

	public static final ResourceLocation SPAWN_STATUS_REQUEST_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "spawn_status_request_c2s");
	public static final ResourceLocation SPAWN_STATUS_S2C = ResourceLocation.fromNamespaceAndPath("islandcore", "spawn_status_s2c");
	public static final ResourceLocation SPAWN_ISLAND_CREATE_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "spawn_island_create_c2s");
	public static final ResourceLocation SPAWN_ISLAND_RESIZE_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "spawn_island_resize_c2s");
	public static final ResourceLocation SPAWN_ISLAND_SET_HOME_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "spawn_island_set_home_c2s");

	// Spawn island configurable build protection (BUILD_PROTECTION IslandSetting) + its
	// always-authorized (CO_OWNER/MEMBER) player list.
	public static final ResourceLocation SPAWN_BUILD_PROTECTION_STATUS_REQUEST_C2S =
			ResourceLocation.fromNamespaceAndPath("islandcore", "spawn_build_protection_status_request_c2s");
	public static final ResourceLocation SPAWN_BUILD_PROTECTION_STATUS_S2C =
			ResourceLocation.fromNamespaceAndPath("islandcore", "spawn_build_protection_status_s2c");
	public static final ResourceLocation SPAWN_BUILD_PROTECTION_SET_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "spawn_build_protection_set_c2s");
	public static final ResourceLocation SPAWN_AUTHORIZED_PLAYER_ADD_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "spawn_authorized_player_add_c2s");
	public static final ResourceLocation SPAWN_AUTHORIZED_PLAYER_REMOVE_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "spawn_authorized_player_remove_c2s");

	// Spawn Permisos/General (Sprint "teletransportes dinámicos" Admin Permisos/General work): the
	// status requests reply with the SAME FlagsStatusS2C/ExceptionGroupsStatusS2C a normal island's
	// own flags/exceptions network path already uses (both builders take an Island directly) — only
	// the request/action payloads are new, targeting the Spawn island instead of the sender's own.
	public static final ResourceLocation SPAWN_FLAGS_STATUS_REQUEST_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "spawn_flags_status_request_c2s");
	public static final ResourceLocation SPAWN_FLAGS_STATUS_S2C = ResourceLocation.fromNamespaceAndPath("islandcore", "spawn_flags_status_s2c");
	public static final ResourceLocation SPAWN_EXCEPTION_GROUPS_STATUS_REQUEST_C2S =
			ResourceLocation.fromNamespaceAndPath("islandcore", "spawn_exception_groups_status_request_c2s");
	public static final ResourceLocation SPAWN_EXCEPTION_GROUPS_STATUS_S2C = ResourceLocation.fromNamespaceAndPath("islandcore", "spawn_exception_groups_status_s2c");
	public static final ResourceLocation SPAWN_FLAG_SET_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "spawn_flag_set_c2s");
	public static final ResourceLocation SPAWN_FLAG_SET_PRESET_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "spawn_flag_set_preset_c2s");
	public static final ResourceLocation SPAWN_EXCEPTION_GROUP_SET_PRESET_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "spawn_exception_group_set_preset_c2s");

	public static final ResourceLocation DIMENSION_LIST_REQUEST_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "dimension_list_request_c2s");
	public static final ResourceLocation DIMENSION_LIST_S2C = ResourceLocation.fromNamespaceAndPath("islandcore", "dimension_list_s2c");
	public static final ResourceLocation DIMENSION_DETAIL_REQUEST_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "dimension_detail_request_c2s");
	public static final ResourceLocation DIMENSION_DETAIL_S2C = ResourceLocation.fromNamespaceAndPath("islandcore", "dimension_detail_s2c");
	public static final ResourceLocation DIMENSION_CREATE_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "dimension_create_c2s");
	public static final ResourceLocation DIMENSION_DELETE_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "dimension_delete_c2s");
	public static final ResourceLocation DIMENSION_DELETE_CONFIRM_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "dimension_delete_confirm_c2s");
	public static final ResourceLocation DIMENSION_REGENERATE_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "dimension_regenerate_c2s");
	public static final ResourceLocation DIMENSION_REGENERATE_CONFIRM_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "dimension_regenerate_confirm_c2s");

	public static final ResourceLocation VANILLA_RESET_LIST_REQUEST_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "vanilla_reset_list_request_c2s");
	public static final ResourceLocation VANILLA_RESET_LIST_S2C = ResourceLocation.fromNamespaceAndPath("islandcore", "vanilla_reset_list_s2c");
	public static final ResourceLocation VANILLA_RESET_QUEUE_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "vanilla_reset_queue_c2s");
	public static final ResourceLocation VANILLA_RESET_CONFIRM_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "vanilla_reset_confirm_c2s");
	public static final ResourceLocation VANILLA_RESET_CANCEL_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "vanilla_reset_cancel_c2s");

	// Flags + exception groups network block: player-facing (not admin-only), mirrors what
	// "/island flags"/"/island exceptions" already compute — see FlagsStatusBuilder.

	public static final ResourceLocation FLAGS_STATUS_REQUEST_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "flags_status_request_c2s");
	public static final ResourceLocation FLAGS_STATUS_S2C = ResourceLocation.fromNamespaceAndPath("islandcore", "flags_status_s2c");
	public static final ResourceLocation FLAG_SET_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "flag_set_c2s");
	public static final ResourceLocation FLAG_SET_PRESET_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "flag_set_preset_c2s");
	public static final ResourceLocation EXCEPTION_GROUPS_STATUS_REQUEST_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "exception_groups_status_request_c2s");
	public static final ResourceLocation EXCEPTION_GROUPS_STATUS_S2C = ResourceLocation.fromNamespaceAndPath("islandcore", "exception_groups_status_s2c");
	// Replaces the old boolean-shaped exception_group_set_c2s (Sprint "excepciones por rol") —
	// exception groups now resolve per role via a 4-way preset, exact mirror of FLAG_SET_PRESET_C2S.
	public static final ResourceLocation EXCEPTION_GROUP_SET_PRESET_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "exception_group_set_preset_c2s");

	// Admin-only: server-wide default configuration (not any specific island) for ROLE_BASED flags
	// and exception groups — the "servidor" layer in FlagResolver/ExceptionResolver's resolution
	// chain. Requires operator (see ServerPacketHandlers#rejectIfNotOperator), same as every other
	// admin network payload.
	public static final ResourceLocation ADMIN_DEFAULTS_STATUS_REQUEST_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "admin_defaults_status_request_c2s");
	public static final ResourceLocation ADMIN_DEFAULTS_STATUS_S2C = ResourceLocation.fromNamespaceAndPath("islandcore", "admin_defaults_status_s2c");
	public static final ResourceLocation ADMIN_FLAG_SET_SERVER_DEFAULT_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "admin_flag_set_server_default_c2s");
	public static final ResourceLocation ADMIN_GLOBAL_FLAG_SET_SERVER_DEFAULT_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "admin_global_flag_set_server_default_c2s");
	public static final ResourceLocation ADMIN_EXCEPTION_SET_SERVER_DEFAULT_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "admin_exception_set_server_default_c2s");
	public static final ResourceLocation ADMIN_FLAG_SET_REQUIREMENT_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "admin_flag_set_requirement_c2s");

	// Party network block: mirrors "/party" — see PartyStatusBuilder.

	public static final ResourceLocation PARTY_STATUS_REQUEST_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "party_status_request_c2s");
	public static final ResourceLocation PARTY_STATUS_S2C = ResourceLocation.fromNamespaceAndPath("islandcore", "party_status_s2c");
	public static final ResourceLocation PARTY_CREATE_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "party_create_c2s");
	public static final ResourceLocation PARTY_INVITE_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "party_invite_c2s");
	public static final ResourceLocation PARTY_ACCEPT_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "party_accept_c2s");
	public static final ResourceLocation PARTY_LEAVE_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "party_leave_c2s");
	public static final ResourceLocation PARTY_KICK_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "party_kick_c2s");
	public static final ResourceLocation PARTY_RENAME_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "party_rename_c2s");
	public static final ResourceLocation PARTY_DISBAND_REQUEST_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "party_disband_request_c2s");
	public static final ResourceLocation PARTY_DISBAND_CONFIRM_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "party_disband_confirm_c2s");

	// Per-player ally-location-sharing preferences — see player.PlayerLocationSharingConfig.
	public static final ResourceLocation LOCATION_SHARING_STATUS_REQUEST_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "location_sharing_status_request_c2s");
	public static final ResourceLocation LOCATION_SHARING_STATUS_S2C = ResourceLocation.fromNamespaceAndPath("islandcore", "location_sharing_status_s2c");
	public static final ResourceLocation LOCATION_SHARING_SET_C2S = ResourceLocation.fromNamespaceAndPath("islandcore", "location_sharing_set_c2s");

	// Periodic push (not requested by the client) — see island.lifecycle.AllyLocationBroadcaster.
	public static final ResourceLocation ALLY_LOCATIONS_S2C = ResourceLocation.fromNamespaceAndPath("islandcore", "ally_locations_s2c");

	private NetworkChannels() {
	}
}
