package acidglow.ingamerecipeeditor.network;

import java.util.List;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import acidglow.ingamerecipeeditor.ModConstants;

/** Exact ingredient tags for the nine recipe-preview grid slots. */
public record RecipePreviewTagsPayload(int containerId, List<Optional<Identifier>> ingredientTags) implements CustomPacketPayload {
    public static final int PREVIEW_SLOT_COUNT = 9;
    public static final Type<RecipePreviewTagsPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "recipe_preview_tags")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, RecipePreviewTagsPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, RecipePreviewTagsPayload::containerId,
        ByteBufCodecs.optional(Identifier.STREAM_CODEC).apply(ByteBufCodecs.list(PREVIEW_SLOT_COUNT)), RecipePreviewTagsPayload::ingredientTags,
        RecipePreviewTagsPayload::new
    );

    public RecipePreviewTagsPayload {
        ingredientTags = List.copyOf(ingredientTags);
        if (ingredientTags.size() != PREVIEW_SLOT_COUNT) {
            throw new IllegalArgumentException("Recipe preview must contain exactly " + PREVIEW_SLOT_COUNT + " ingredient tags");
        }
    }

    @Override
    public Type<RecipePreviewTagsPayload> type() {
        return TYPE;
    }
}
