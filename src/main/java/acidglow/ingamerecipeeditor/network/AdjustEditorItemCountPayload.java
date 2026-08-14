package acidglow.ingamerecipeeditor.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import acidglow.ingamerecipeeditor.ModConstants;

/** Adjusts the count of the editor's output/input ghost stack by exactly one. */
public record AdjustEditorItemCountPayload(int delta) implements CustomPacketPayload {
    public static final Type<AdjustEditorItemCountPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "adjust_editor_item_count")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, AdjustEditorItemCountPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, AdjustEditorItemCountPayload::delta,
        AdjustEditorItemCountPayload::new
    );

    @Override
    public Type<AdjustEditorItemCountPayload> type() {
        return TYPE;
    }
}
