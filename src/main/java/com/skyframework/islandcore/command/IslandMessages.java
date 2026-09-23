package com.skyframework.islandcore.command;

import com.mojang.authlib.GameProfile;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.api.island.IslandState;
import com.skyframework.islandcore.island.entity.EntityCategory;
import com.skyframework.islandcore.island.model.IslandBounds;
import com.skyframework.islandcore.island.model.IslandMember;
import com.skyframework.islandcore.island.model.IslandRole;
import com.skyframework.islandcore.util.ServerLang;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Shared chat-output formatting, used by both the temporary /ic debug command
// tree and the real /island command tree, to avoid duplicating it in both places.
public final class IslandMessages {

	private static final DateTimeFormatter DATE_FORMATTER =
			DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

	private IslandMessages() {
	}

	// /island admin list (all islands): both the owner and the island itself are shown as short,
	// readable, colored labels instead of raw UUIDs, each with a hover tooltip and click-to-copy
	// for the full UUID underneath — nicer to read, while keeping the raw id one click away for
	// anyone who needs it (e.g. for /island admin list <player> or /island admin delete).
	public static void sendIslandSummary(CommandSourceStack source, Island island) {
		BlockPos center = island.getCenter();
		UUID ownerUuid = island.getOwnerUuid();
		boolean isSpawnIsland = ownerUuid.equals(Island.SERVER_OWNER_UUID);
		// Island.SERVER_OWNER_UUID isn't a real player: resolveName's server.getProfileCache() lookup
		// would just fail and fall back to printing the raw UUID, so short-circuit with a readable
		// label instead — same special-casing AdminIslandBuilder already does for isSpawnIsland.
		String ownerName = isSpawnIsland ? "Server" : resolveName(source.getServer(), ownerUuid);

		MutableComponent ownerText = Component.literal(ownerName)
				.withStyle(ChatFormatting.BOLD, ChatFormatting.AQUA)
				.withStyle(style -> style
						.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(ownerUuid.toString())))
						.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, ownerUuid.toString())));

		UUID islandId = island.getIslandId();
		String islandLabel = isSpawnIsland ? "Isla de Spawn" : "Isla de " + ownerName;
		MutableComponent islandText = Component.literal(islandLabel)
				.withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD)
				.withStyle(style -> style
						.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(islandId.toString())))
						.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, islandId.toString())));

		source.sendSuccess(() -> Component.literal("- ")
				.append(islandText)
				.append(Component.literal(" owner="))
				.append(ownerText)
				.append(Component.literal(" center=(" + center.getX() + ", " + center.getY() + ", " + center.getZ() + ")"
						+ " state=" + island.getState())), false);
	}

	// Player-facing view: readable, no raw UUIDs/bounds, technical fields omitted.
	// Used by /island info and /island list.
	public static void sendIslandSummaryPlayer(CommandSourceStack source, Island island) {
		ServerPlayer player = source.getPlayer();
		source.sendSuccess(() -> ServerLang.of(player, "=== Tu Isla ===", "=== Your Island ===").copy().withStyle(ChatFormatting.BOLD, ChatFormatting.AQUA), false);

		int currentSize = island.getIslandSize();
		int maxSize = IslandCoreMod.PERMISSION_PROVIDER.getHighestSizeAllowed(island.getOwnerUuid());
		source.sendSuccess(() -> labeled("Tamaño: ", currentSize + " / " + maxSize), false);

		source.sendSuccess(() -> labeled("Tipo: ", island.getIslandType().getDisplayName()), false);

		BlockPos home = island.getHomeLocation();
		source.sendSuccess(() -> labeled("Home: ", home.getX() + ", " + home.getY() + ", " + home.getZ()), false);

		if (island.getState() != IslandState.ACTIVE) {
			source.sendSuccess(() -> ServerLang.of(player, "Estado: " + island.getState(), "State: " + island.getState()).copy().withStyle(ChatFormatting.RED), false);
		}

		sendMembersSectionPlayer(source, island);
		sendEntitySection(source, island);
	}

	// Full technical view, organized into titled sections. Shows exactly the same fields as
	// before (Sprint 14.5 only restyled this, no data added or removed). Used by
	// /island admin list <player>.
	public static void sendIslandSummaryAdmin(CommandSourceStack source, Island island) {
		MinecraftServer server = source.getServer();
		String ownerName = resolveName(server, island.getOwnerUuid());

		source.sendSuccess(() -> Component.literal("=== Isla de " + ownerName + " (ADMIN) ===").withStyle(ChatFormatting.BOLD, ChatFormatting.RED), false);

		sectionTitle(source, "Identificadores", ChatFormatting.GRAY);
		source.sendSuccess(() -> labeled("islandId: ", island.getIslandId().toString()), false);
		source.sendSuccess(() -> labeled("ownerUuid: ", island.getOwnerUuid().toString()), false);

		sectionTitle(source, "Ubicación y Tamaño", ChatFormatting.AQUA);
		source.sendSuccess(() -> labeled("dimension: ", island.getDimension().location().toString()), false);
		source.sendSuccess(() -> labeled("grid: ", island.getGridX() + ", " + island.getGridZ()), false);
		BlockPos center = island.getCenter();
		source.sendSuccess(() -> labeled("center: ", center.getX() + ", " + center.getY() + ", " + center.getZ()), false);
		source.sendSuccess(() -> labeled("bounds: ", formatBounds(island.getBounds())), false);
		source.sendSuccess(() -> labeled("plotBounds: ", formatBounds(island.getPlotBounds())), false);
		source.sendSuccess(() -> labeled("islandSize: ", String.valueOf(island.getIslandSize())), false);
		source.sendSuccess(() -> labeled("plotSize: ", String.valueOf(island.getPlotSize())), false);
		source.sendSuccess(() -> labeled("islandType: ", island.getIslandType().getId()), false);
		BlockPos home = island.getHomeLocation();
		source.sendSuccess(() -> labeled("homeLocation: ", home.getX() + ", " + home.getY() + ", " + home.getZ()), false);

		sendMembersSectionAdmin(source, island);

		sectionTitle(source, "Estado y Fechas", ChatFormatting.GRAY);
		ChatFormatting stateColor = island.getState() == IslandState.ACTIVE ? ChatFormatting.WHITE : ChatFormatting.RED;
		source.sendSuccess(() -> Component.literal("state: ").withStyle(ChatFormatting.GRAY)
				.append(Component.literal(island.getState().toString()).withStyle(stateColor)), false);
		source.sendSuccess(() -> labeled("createdAt: ", DATE_FORMATTER.format(island.getCreatedAt())), false);
		source.sendSuccess(() -> labeled("updatedAt: ", DATE_FORMATTER.format(island.getUpdatedAt())), false);

		sendEntitySection(source, island);
	}

	private static void sendMembersSectionAdmin(CommandSourceStack source, Island island) {
		sectionTitle(source, "Miembros", ChatFormatting.GOLD);

		MinecraftServer server = source.getServer();

		source.sendSuccess(() -> memberLineAdmin(server, island.getOwnerUuid(), IslandRole.OWNER), false);

		for (IslandMember member : island.getMembers()) {
			if (member.role() != IslandRole.MEMBER && member.role() != IslandRole.CO_OWNER) {
				continue;
			}
			source.sendSuccess(() -> memberLineAdmin(server, member.playerUuid(), member.role()), false);
		}
	}

	// Package-visible (not private): reused by IslandCommand's /island flags listing for the same
	// titled-section look as sendIslandSummaryPlayer/sendIslandSummaryAdmin.
	static void sectionTitle(CommandSourceStack source, String title, ChatFormatting color) {
		source.sendSuccess(() -> Component.literal(title).withStyle(ChatFormatting.BOLD, color), false);
	}

	private static String formatBounds(IslandBounds bounds) {
		BlockPos min = bounds.min();
		BlockPos max = bounds.max();
		return "(" + min.getX() + ", " + min.getY() + ", " + min.getZ() + ") -> ("
				+ max.getX() + ", " + max.getY() + ", " + max.getZ() + ")";
	}

	private static void sendMembersSectionPlayer(CommandSourceStack source, Island island) {
		ServerPlayer player = source.getPlayer();
		source.sendSuccess(() -> ServerLang.of(player, "Miembros", "Members").copy().withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD), false);

		MinecraftServer server = source.getServer();

		source.sendSuccess(() -> memberLine(player, server, island.getOwnerUuid(), IslandRole.OWNER), false);

		for (IslandMember member : island.getMembers()) {
			// VISITOR/DENIED aren't explicit members: nothing currently stores them here, but
			// filter defensively in case that ever changes.
			if (member.role() != IslandRole.MEMBER && member.role() != IslandRole.CO_OWNER) {
				continue;
			}
			source.sendSuccess(() -> memberLine(player, server, member.playerUuid(), member.role()), false);
		}
	}

	private static Component memberLine(ServerPlayer player, MinecraftServer server, UUID playerUuid, IslandRole role) {
		String name = resolveName(server, playerUuid);
		return ServerLang.of(player, "- " + name + " ", "- " + name + " ")
				.copy()
				.append(ServerLang.of(player, "(" + role + ")", "(" + role + ")").copy().withStyle(roleColor(role)));
	}

	private static Component memberLineAdmin(MinecraftServer server, UUID playerUuid, IslandRole role) {
		String name = resolveName(server, playerUuid);
		return Component.literal("- " + name + " (" + playerUuid + ") ")
				.append(Component.literal("[" + role + "]").withStyle(roleColor(role)));
	}

	private static ChatFormatting roleColor(IslandRole role) {
		return switch (role) {
			case OWNER -> ChatFormatting.GOLD;
			case MEMBER -> ChatFormatting.GREEN;
			case CO_OWNER -> ChatFormatting.AQUA;
			case ALLY -> ChatFormatting.YELLOW;
			case VISITOR, DENIED -> ChatFormatting.GRAY;
		};
	}

	private static String resolveName(MinecraftServer server, UUID playerUuid) {
		return server.getProfileCache().get(playerUuid).map(GameProfile::getName).orElse(playerUuid.toString());
	}

	private static void sendEntitySection(CommandSourceStack source, Island island) {
		source.sendSuccess(() -> Component.literal("Entidades").withStyle(ChatFormatting.BOLD, ChatFormatting.LIGHT_PURPLE), false);

		Map<EntityCategory, Integer> counts = IslandCoreMod.ENTITY_TRACKER.countByCategory(island.getIslandId());
		int total = counts.values().stream().mapToInt(Integer::intValue).sum();

		List<String> parts = new ArrayList<>();
		addCategoryPart(parts, counts, EntityCategory.PLAYERS, "jugador", "jugadores");
		addCategoryPart(parts, counts, EntityCategory.PASSIVE, "pasivo", "pasivos");
		addCategoryPart(parts, counts, EntityCategory.HOSTILE, "hostil", "hostiles");
		addCategoryPart(parts, counts, EntityCategory.ITEMS, "objeto", "objetos");
		addCategoryPart(parts, counts, EntityCategory.COBBLEMON, "Cobblemon", "Cobblemon");
		addCategoryPart(parts, counts, EntityCategory.OTHER, "otro", "otros");

		String summary = "Total (" + total + ")" + (parts.isEmpty() ? "" : ": " + String.join(", ", parts));
		source.sendSuccess(() -> Component.literal(summary), false);
	}

	private static void addCategoryPart(
			List<String> parts, Map<EntityCategory, Integer> counts, EntityCategory category, String singular, String plural) {
		int count = counts.getOrDefault(category, 0);
		if (count > 0) {
			parts.add(count + " " + (count == 1 ? singular : plural));
		}
	}

	// Package-visible (not private): reused by IslandCommand's /island flags listing.
	static Component labeled(String label, String value) {
		return Component.literal(label).withStyle(ChatFormatting.GRAY).append(Component.literal(value).withStyle(ChatFormatting.WHITE));
	}
}
