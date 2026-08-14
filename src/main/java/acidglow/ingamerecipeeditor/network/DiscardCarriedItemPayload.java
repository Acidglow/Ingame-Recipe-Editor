package acidglow.ingamerecipeeditor.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import acidglow.ingamerecipeeditor.ModConstants;

/** Discards the stack currently carried by a player in the Item Book area. */
public record DiscardCarriedItemPayload() implements CustomPacketPayload {
    public static final Type<DiscardCarriedItemPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "discard_carried_item")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, DiscardCarriedItemPayload> STREAM_CODEC =
        StreamCodec.unit(new DiscardCarriedItemPayload());

    @Override
    public Type<DiscardCarriedItemPayload> type() {
        return TYPE;
    }
}
