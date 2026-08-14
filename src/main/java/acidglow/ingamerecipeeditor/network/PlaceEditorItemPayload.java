package acidglow.ingamerecipeeditor.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import acidglow.ingamerecipeeditor.ModConstants;

/** Places an Item Book selection; Shift controls whether inventory receives a full stack. */
public record PlaceEditorItemPayload(int slotIndex, Identifier itemId, boolean fullStack) implements CustomPacketPayload {
    public static final Type<PlaceEditorItemPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "place_editor_item")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, PlaceEditorItemPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, PlaceEditorItemPayload::slotIndex,
        Identifier.STREAM_CODEC, PlaceEditorItemPayload::itemId,
        ByteBufCodecs.BOOL, PlaceEditorItemPayload::fullStack,
        PlaceEditorItemPayload::new
    );

    @Override
    public Type<PlaceEditorItemPayload> type() {
        return TYPE;
    }
}
