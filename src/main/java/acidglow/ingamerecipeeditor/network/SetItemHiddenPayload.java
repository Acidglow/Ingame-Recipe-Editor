package acidglow.ingamerecipeeditor.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import acidglow.ingamerecipeeditor.ModConstants;

/** Sets whether one input item is globally hidden by the recipe editor. */
public record SetItemHiddenPayload(Identifier itemId, boolean hidden) implements CustomPacketPayload {
    public static final Type<SetItemHiddenPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "set_item_hidden")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, SetItemHiddenPayload> STREAM_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC, SetItemHiddenPayload::itemId,
        ByteBufCodecs.BOOL, SetItemHiddenPayload::hidden,
        SetItemHiddenPayload::new
    );

    @Override
    public Type<SetItemHiddenPayload> type() {
        return TYPE;
    }
}
