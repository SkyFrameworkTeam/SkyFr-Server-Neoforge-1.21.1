package com.skyframework.islandcore.mixin;

import com.skyframework.islandcore.IslandCoreMod;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import net.minecraft.core.SectionPos;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Defensive mitigation for a confirmed 100% vanilla NullPointerException, unrelated to our own
// code — full root-cause investigation done separately on the Fabric side of this project
// (decompiled against the real 1.21.1 game jar): DistanceManager#removePlayer (Yarn:
// ChunkTicketManager#handleChunkLeave) assumes playersPerChunk.get(l) always returns a non-null
// ObjectSet, because addPlayer is the ONLY place that ever populates it (lazily, via
// computeIfAbsent) — removePlayer itself never checks for null before calling .remove(...) on
// whatever get(l) returned. That symmetry can break: confirmed independently in pure vanilla via
// RelativityMC/C2ME-fabric#523 (there caused by C2ME's own multithreaded chunk-ticket access, a mod
// we don't use), and suspected here to be related to a non-standard persistent-dimension
// teardown/recreation cycle (the Fabric build's Fantasy library skips vanilla's usual multi-step,
// drain-before-close sequence; see NucleoidMC/fantasy#78 for the maintainer's own acknowledgment of
// a related conflict). The exact trigger for OUR crashes remains unconfirmed — every captured stack
// trace was pure net.minecraft.*, no islandcore/Fantasy/Mixin frame in it — so this mitigation
// targets the symptom, not any dimension provider's lifecycle itself.
//
// Chosen fix (same as the current Fabric version — the older NeoForge port substituted a fresh
// empty ObjectSet via @Redirect instead, which Fabric explicitly REJECTED): bail out of removePlayer
// ENTIRELY when the lookup is missing, touching nothing else — the distance/ticket trackers are left
// completely alone, since if playersPerChunk never had an entry for this position there's no reason
// to believe those trackers have one either. (Substituting an empty set would still exercise the
// "position is now empty" cleanup branch against trackers that may never have been populated for this
// position in the first place, which isn't obviously correct and isn't needed just to avoid the
// crash.) Scope is deliberately narrow: only this one null-check, no other method on DistanceManager.
@Mixin(DistanceManager.class)
public class ChunkTicketManagerMixin {

	@Shadow
	@Final
	Long2ObjectMap<ObjectSet<ServerPlayer>> playersPerChunk;

	// Plain @Inject rather than @Redirect/@ModifyExpressionValue on the playersPerChunk.get(l) call
	// itself: a @Redirect (or MixinExtras' @ModifyExpressionValue) can only replace the VALUE of that
	// one expression — neither has any way to make the enclosing removePlayer method return early,
	// which is exactly what this fix needs (skip the rest of the method's logic entirely, not feed it
	// a substitute value). @Inject at HEAD with cancellable=true is the standard Mixin idiom for "skip
	// this method under condition X" and needs no MixinExtras — it re-derives the exact same lookup
	// key (sectionPos.chunk().toLong()) the real method body computes a few lines later, purely to
	// decide whether to bail out before any of vanilla's own logic runs.
	@Inject(method = "removePlayer", at = @At("HEAD"), cancellable = true)
	private void islandcore$skipIfNoPlayerSetTracked(SectionPos pos, ServerPlayer player, CallbackInfo ci) {
		long chunkPosLong = pos.chunk().toLong();
		if (this.playersPerChunk.get(chunkPosLong) == null) {
			// INFO, not WARN/ERROR: this is a known, deliberately-tolerated vanilla edge case (see
			// class comment above), not a real failure — kept visible at INFO (rather than DEBUG)
			// specifically so it shows up in a normal server log without needing debug logging
			// enabled, so we can tell how often it still fires after this mitigation.
			IslandCoreMod.LOGGER.info(
					"DistanceManager.removePlayer: no player set tracked for chunk section {} (player {}); "
							+ "skipping — this is a known, tolerated vanilla/Fantasy edge case, not a real failure. "
							+ "Report if this appears very frequently.",
					pos, player.getUUID());
			ci.cancel();
		}
	}
}
