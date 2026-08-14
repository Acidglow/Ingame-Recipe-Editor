package acidglow.ingamerecipeeditor.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import acidglow.ingamerecipeeditor.ModConstants;

/** Requests one exact recipe's removal or restoration; the server owns all validation. */
public record MutateRecipePayload(Identifier recipeId, Identifier recipeTypeId, boolean restoreDefault) implements CustomPacketPayload {
    public static final Type<MutateRecipePayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "mutate_recipe")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, MutateRecipePayload> STREAM_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC, MutateRecipePayload::recipeId,
        Identifier.STREAM_CODEC, MutateRecipePayload::recipeTypeId,
        ByteBufCodecs.BOOL, MutateRecipePayload::restoreDefault,
        MutateRecipePayload::new
    );

    @Override
    public Type<MutateRecipePayload> type() {
        return TYPE;
    }
}
