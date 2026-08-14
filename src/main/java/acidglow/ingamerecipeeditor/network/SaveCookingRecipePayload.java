package acidglow.ingamerecipeeditor.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import acidglow.ingamerecipeeditor.ModConstants;

/** New vanilla cooking-recipe draft. The server assigns the persistent custom recipe ID. */
public record SaveCookingRecipePayload(
    Identifier outputItemId,
    Identifier inputItemId,
    Identifier recipeTypeId,
    int outputCount,
    float experience,
    int cookingTime
) implements CustomPacketPayload {
    public static final Type<SaveCookingRecipePayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "save_cooking_recipe")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, SaveCookingRecipePayload> STREAM_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC, SaveCookingRecipePayload::outputItemId,
        Identifier.STREAM_CODEC, SaveCookingRecipePayload::inputItemId,
        Identifier.STREAM_CODEC, SaveCookingRecipePayload::recipeTypeId,
        ByteBufCodecs.VAR_INT, SaveCookingRecipePayload::outputCount,
        ByteBufCodecs.FLOAT, SaveCookingRecipePayload::experience,
        ByteBufCodecs.VAR_INT, SaveCookingRecipePayload::cookingTime,
        SaveCookingRecipePayload::new
    );

    @Override
    public Type<SaveCookingRecipePayload> type() {
        return TYPE;
    }
}
