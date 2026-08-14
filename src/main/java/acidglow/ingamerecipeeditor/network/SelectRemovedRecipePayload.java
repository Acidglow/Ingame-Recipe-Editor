package acidglow.ingamerecipeeditor.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import acidglow.ingamerecipeeditor.ModConstants;

/** Requests a saved deleted default recipe be previewed before it is restored. */
public record SelectRemovedRecipePayload(Identifier recipeId, Identifier recipeTypeId, Identifier outputItemId)
    implements CustomPacketPayload {
    public static final Type<SelectRemovedRecipePayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "select_removed_recipe")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectRemovedRecipePayload> STREAM_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC, SelectRemovedRecipePayload::recipeId,
        Identifier.STREAM_CODEC, SelectRemovedRecipePayload::recipeTypeId,
        Identifier.STREAM_CODEC, SelectRemovedRecipePayload::outputItemId,
        SelectRemovedRecipePayload::new
    );

    @Override
    public Type<SelectRemovedRecipePayload> type() {
        return TYPE;
    }
}
