package acidglow.ingamerecipeeditor.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import acidglow.ingamerecipeeditor.ModConstants;

/** Selects the previous or next recipe within the active type. */
public record NavigateEditorRecipePayload(int direction) implements CustomPacketPayload {
    public static final Type<NavigateEditorRecipePayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "navigate_editor_recipe")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, NavigateEditorRecipePayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, NavigateEditorRecipePayload::direction,
        NavigateEditorRecipePayload::new
    );

    @Override
    public Type<NavigateEditorRecipePayload> type() {
        return TYPE;
    }
}
