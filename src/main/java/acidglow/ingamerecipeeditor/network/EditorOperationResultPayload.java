package acidglow.ingamerecipeeditor.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import acidglow.ingamerecipeeditor.ModConstants;

/** Bounded result message for an editor operation. */
public record EditorOperationResultPayload(boolean success, String message) implements CustomPacketPayload {
    public static final int MAX_MESSAGE_LENGTH = 256;
    public static final Type<EditorOperationResultPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "editor_operation_result")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, EditorOperationResultPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL, EditorOperationResultPayload::success,
        ByteBufCodecs.stringUtf8(MAX_MESSAGE_LENGTH), EditorOperationResultPayload::message,
        EditorOperationResultPayload::new
    );

    public EditorOperationResultPayload {
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Editor result message is too long");
        }
    }

    @Override
    public Type<EditorOperationResultPayload> type() {
        return TYPE;
    }
}
