package com.skyframework.islandcore.mixin;

import com.skyframework.islandcore.IslandCoreMod;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.SectionPos;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Defensive mitigation for a confirmed 100% vanilla NullPointerException, unrelated to our own
// code or to the Dimension Manager — investigated in depth on the Fabric side of this project
// (crash: "Cannot invoke \"it.unimi.dsi.fastutil.objects.ObjectSet.remove(Object)\" because \"$$4\"
// is null", at DistanceManager.removePlayer — Mojmap's equivalent of Yarn's
// ChunkTicketManager#handleChunkLeave — triggered while processing a player disconnect).
//
// removePlayer (confirmed by decompiling the 1.21.1 game jar with Mojmap names) does:
//   ObjectSet<ServerPlayer> objectset = this.playersPerChunk.get(chunkpos.toLong());
//   objectset.remove(player);   // <- crashes here if the lookup returned null
//   if (objectset.isEmpty()) { ...cleans up naturalSpawnChunkCounter/playerTicketManager/
//   tickingTicketsTracker... }
// with no null check before the first line — the exact same shape as the Yarn source this was
// ported from. The exact same NPE (same message, same method) was independently reported in
// RelativityMC/C2ME-fabric#523, there confirmed to be caused by C2ME's own multithreaded chunk-
// ticket management (a mod we don't use). That confirms this exact spot is a known-fragile point in
// vanilla's single-threaded bookkeeping in general, even though our own trigger is a different,
// still-unconfirmed cause.
//
// Mitigation: if the lookup would return null, substitute a fresh, empty, MUTABLE ObjectSet
// instead of letting the null through. Deliberately NOT it.unimi.dsi.fastutil.objects.ObjectSets
// .emptySet() — decompiled that too: its remove(Object) unconditionally throws
// UnsupportedOperationException, which would just trade one crash for another. A real empty set
// makes the rest of the vanilla method run exactly as it would for a legitimately-just-emptied
// position: nothing is skipped, a missing entry is simply treated as equivalent to an empty one.
//
// Plain @Redirect rather than MixinExtras' @ModifyExpressionValue (which the Fabric source used):
// achieves the identical effect — intercepting this one Long2ObjectMap#get(long) call's return
// value — without adding a new MixinExtras dependency to this project just for one mitigation.
@Mixin(DistanceManager.class)
public class ChunkTicketManagerMixin {

	@Redirect(
			method = "removePlayer",
			at = @At(
					value = "INVOKE",
					target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;get(J)Ljava/lang/Object;"))
	private Object islandcore$fallBackToEmptySetOnNullChunkTracking(
			Long2ObjectMap<ObjectSet<ServerPlayer>> instance, long chunkPos, SectionPos sectionPos, ServerPlayer player) {
		Object original = instance.get(chunkPos);
		if (original != null) {
			return original;
		}

		// Diagnostic only: lets us tell, from a normal server log, whether this still happens and
		// how often — without that, this mitigation would be silently invisible if it ever fires.
		IslandCoreMod.LOGGER.warn(
				"DistanceManager.removePlayer: no player set tracked for chunk section {} (player {}); "
						+ "substituting an empty one to avoid a known vanilla NullPointerException on disconnect. "
						+ "This indicates an upstream chunk-tracking desync we haven't root-caused — report if this appears often.",
				sectionPos, player.getUUID());

		return new ObjectOpenHashSet<ServerPlayer>();
	}
}
