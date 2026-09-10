package com.skyframework.islandcore.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Countdown tick for a two-step confirmation window (currently only /island delete's 30-second
// confirm window; IslandDeletionService#getPendingDeletionRemainingSeconds was added in this same
// sprint specifically to back this payload). Defined and registered so a future sprint's
// broadcasting loop (and IslandCoreClient's Delete Island screen) has a stable wire format to
// target, but no periodic sender is wired up yet in this sprint — the request/confirm actions
// themselves already reply immediately via ActionResultS2C, and that's sufficient for a chat/GUI
// confirmation prompt without a live countdown. Building the tick broadcaster itself is left for
// when a screen actually needs to render a moving countdown, to avoid adding an unused tick loop.
public record PendingConfirmationTickS2C(int remainingSeconds) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<PendingConfirmationTickS2C> TYPE =
			new CustomPacketPayload.Type<>(NetworkChannels.PENDING_CONFIRMATION_TICK_S2C);

	public static final StreamCodec<RegistryFriendlyByteBuf, PendingConfirmationTickS2C> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, PendingConfirmationTickS2C::remainingSeconds,
			PendingConfirmationTickS2C::new
	);

	@Override
	public CustomPacketPayload.Type<PendingConfirmationTickS2C> type() {
		return TYPE;
	}
}
