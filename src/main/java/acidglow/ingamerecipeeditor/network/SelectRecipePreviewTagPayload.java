package acidglow.ingamerecipeeditor.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import acidglow.ingamerecipeeditor.ModConstants;

/** Selects an item-tag variant for one ghost ingredient slot. */
public record SelectRecipePreviewTagPayload(int slotIndex, Identifier tagId) implements CustomPacketPayload {
    public static final Type<SelectRecipePreviewTagPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "select_recipe_preview_tag")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectRecipePreviewTagPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, SelectRecipePreviewTagPayload::slotIndex,
        Identifier.STREAM_CODEC, SelectRecipePreviewTagPayload::tagId,
        SelectRecipePreviewTagPayload::new
    );

    @Override
    public Type<SelectRecipePreviewTagPayload> type() {
        return TYPE;
    }
}
