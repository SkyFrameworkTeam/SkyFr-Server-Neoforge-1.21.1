package com.skyframework.islandcore.net.admin.island;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.core.UUIDUtil;

import java.util.UUID;

// Network equivalent of "/island admin delete <player>": resolves the target's island via
// getIslandByOwner, then calls the exact same IslandDeletionService#requestDeletion the text
// command uses. Reply is a plain ActionResultS2C — no dedicated confirmation-prompt payload,
// same as the text command's own chat message being the only feedback today.
public record AdminIslandDeleteC2S(UUID targetUuid) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<AdminIslandDeleteC2S> TYPE = new CustomPacketPayload.Type<>(NetworkChannels.ADMIN_ISLAND_DELETE_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, AdminIslandDeleteC2S> CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC, AdminIslandDeleteC2S::targetUuid,
			AdminIslandDeleteC2S::new
	);

	@Override
	public CustomPacketPayload.Type<AdminIslandDeleteC2S> type() {
		return TYPE;
	}
}
