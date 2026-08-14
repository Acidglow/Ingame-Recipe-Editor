package acidglow.ingamerecipeeditor.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import acidglow.ingamerecipeeditor.ModConstants;

/** Clears one ghost-value slot in the server-backed recipe editor. */
public record ClearEditorItemPayload(int slotIndex) implements CustomPacketPayload {
    public static final Type<ClearEditorItemPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "clear_editor_item")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ClearEditorItemPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, ClearEditorItemPayload::slotIndex,
        ClearEditorItemPayload::new
    );

    @Override
    public Type<ClearEditorItemPayload> type() {
        return TYPE;
    }
}
