package com.skyframework.islandcore.net.admin.island;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.core.UUIDUtil;

import java.util.UUID;

// Network equivalent of "/island admin delete <player> confirm". Resolves the target's island via
// getIslandByOwner (same as AdminIslandDeleteC2S) then calls the same
// IslandDeletionService#confirmDeletion the text command uses.
public record AdminIslandDeleteConfirmC2S(UUID targetUuid) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<AdminIslandDeleteConfirmC2S> TYPE =
			new CustomPacketPayload.Type<>(NetworkChannels.ADMIN_ISLAND_DELETE_CONFIRM_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, AdminIslandDeleteConfirmC2S> CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC, AdminIslandDeleteConfirmC2S::targetUuid,
			AdminIslandDeleteConfirmC2S::new
	);

	@Override
	public CustomPacketPayload.Type<AdminIslandDeleteConfirmC2S> type() {
		return TYPE;
	}
}
