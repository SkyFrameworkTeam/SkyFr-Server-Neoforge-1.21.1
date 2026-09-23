package com.skyframework.islandcore.protection;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.util.ServerLang;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

// Ported from Fabric's dedicated callback interfaces (PlayerBlockBreakEvents/UseBlockCallback/
// UseEntityCallback/AttackEntityCallback) to NeoForge's game event bus — the closest 1:1
// equivalents are BlockEvent.BreakEvent, PlayerInteractEvent.RightClickBlock,
// PlayerInteractEvent.EntityInteract and AttackEntityEvent respectively. Denial is expressed by
// cancelling the event (NeoForge's cancel semantics match Fabric's "return false"/ActionResult.FAIL
// for each of these).
public class ProtectionListeners {

	private ProtectionListeners() {
	}

	// Only meaningful to call after an action was denied: outside the islands dimension,
	// AccessController always allows, so a deny here always means we're inside it. Mirrors
	// AccessControllerImpl's classification purely to pick the right message. Was 3 static final
	// Text constants built once at class-load time — turned into a per-call method so each one can
	// be picked per the RECEIVING player's own client language (see ServerLang) instead of a single
	// server-wide value baked in at startup.
	private static Component denyMessage(BlockPos pos, ServerPlayer player) {
		Island island = IslandCoreMod.ISLAND_REGISTRY.getIslandAt(pos).orElse(null);
		if (island == null) {
			return ServerLang.of(player, "Esta zona no pertenece a ninguna isla.", "This area doesn't belong to any island.");
		}
		if (!island.getBounds().contains(pos) && !isBeyondWorldHeightLimit(pos)) {
			return ServerLang.of(player, "Esta zona está reservada para una futura ampliación de la isla.",
					"This area is reserved for a future island expansion.");
		}
		return ServerLang.of(player, "No tienes permiso para hacer esto aquí.", "You don't have permission to do this here.");
	}

	// Y=320 (one above the highest buildable layer) and Y=-64 (the world's own bottom — see
	// IslandRegistryImpl's MIN_Y/MAX_Y, which already sets every island's own vertical bounds to
	// exactly this same range) are the world's own absolute vertical limits, not this island's
	// plot edge. Vanilla already shows its own "outside the world" message there, so
	// RESERVED_PLOT_MESSAGE — meant for the island's horizontal/plot boundary — would just be a
	// confusing, redundant second message stacked on top of it.
	private static boolean isBeyondWorldHeightLimit(BlockPos pos) {
		return pos.getY() >= 320 || pos.getY() <= -64;
	}

	public static void register() {
		NeoForge.EVENT_BUS.addListener(ProtectionListeners::onBlockBreak);
		NeoForge.EVENT_BUS.addListener(ProtectionListeners::onUseBlock);
		NeoForge.EVENT_BUS.addListener(ProtectionListeners::onUseEntity);
		NeoForge.EVENT_BUS.addListener(ProtectionListeners::onAttackEntity);
	}

	private static void onBlockBreak(BlockEvent.BreakEvent event) {
		if (!(event.getLevel() instanceof ServerLevel serverWorld)) {
			return;
		}
		Player player = event.getPlayer();
		BlockPos pos = event.getPos();

		if (IslandCoreMod.ACCESS_CONTROLLER.canBreak(player.getUUID(), serverWorld, pos)) {
			return;
		}

		DeniedActionThrottler.notifyDenied(player, denyMessage(pos, (ServerPlayer) player));
		event.setCanceled(true);
	}

	private static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
		if (!(event.getLevel() instanceof ServerLevel serverWorld)) {
			return;
		}

		Player player = event.getEntity();
		InteractionHand hand = event.getHand();
		BlockHitResult hitResult = event.getHitVec();
		BlockPos clickedPos = hitResult.getBlockPos();
		ItemStack heldStack = player.getItemInHand(hand);

		BlockPos checkedPos;
		boolean allowed;
		if (heldStack.getItem() instanceof BlockItem) {
			// Heuristic: holding a placeable block is treated as an attempt to place at the clicked face,
			// rather than trying to replicate vanilla's exact interact-vs-place priority resolution.
			checkedPos = clickedPos.relative(hitResult.getDirection());
			allowed = IslandCoreMod.ACCESS_CONTROLLER.canPlace(player.getUUID(), serverWorld, checkedPos);
		} else {
			checkedPos = clickedPos;
			BlockState state = serverWorld.getBlockState(clickedPos);
			MenuProvider screenHandlerFactory = state.getMenuProvider(serverWorld, clickedPos);
			allowed = screenHandlerFactory != null
					? IslandCoreMod.ACCESS_CONTROLLER.canOpenContainer(player.getUUID(), serverWorld, clickedPos)
					: IslandCoreMod.ACCESS_CONTROLLER.canInteractBlock(player.getUUID(), serverWorld, clickedPos);
		}

		if (allowed) {
			return;
		}

		DeniedActionThrottler.notifyDenied(player, denyMessage(checkedPos, (ServerPlayer) player));
		event.setCanceled(true);
		event.setCancellationResult(InteractionResult.FAIL);
	}

	private static void onUseEntity(PlayerInteractEvent.EntityInteract event) {
		if (!(event.getLevel() instanceof ServerLevel serverWorld)) {
			return;
		}

		Player player = event.getEntity();
		Entity entity = event.getTarget();

		if (IslandCoreMod.ACCESS_CONTROLLER.canInteractEntity(player.getUUID(), serverWorld, entity)) {
			return;
		}

		DeniedActionThrottler.notifyDenied(player, denyMessage(entity.blockPosition(), (ServerPlayer) player));
		event.setCanceled(true);
		event.setCancellationResult(InteractionResult.FAIL);
	}

	private static void onAttackEntity(AttackEntityEvent event) {
		Level world = event.getEntity().level();
		if (!(world instanceof ServerLevel serverWorld)) {
			return;
		}

		Player player = event.getEntity();
		Entity entity = event.getTarget();

		if (IslandCoreMod.ACCESS_CONTROLLER.canAttackEntity(player.getUUID(), serverWorld, entity)) {
			return;
		}

		DeniedActionThrottler.notifyDenied(player, denyMessage(entity.blockPosition(), (ServerPlayer) player));
		event.setCanceled(true);
	}
}
