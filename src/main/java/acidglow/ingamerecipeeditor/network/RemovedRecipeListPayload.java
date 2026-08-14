package acidglow.ingamerecipeeditor.network;

import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import acidglow.ingamerecipeeditor.ModConstants;

/** The deleted default recipes currently available for restoration in the open editor. */
public record RemovedRecipeListPayload(
    int containerId,
    Identifier outputItemId,
    Identifier recipeTypeId,
    List<Identifier> recipeIds
) implements CustomPacketPayload {
    public static final Type<RemovedRecipeListPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "removed_recipe_list")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, RemovedRecipeListPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, RemovedRecipeListPayload::containerId,
        Identifier.STREAM_CODEC, RemovedRecipeListPayload::outputItemId,
        Identifier.STREAM_CODEC, RemovedRecipeListPayload::recipeTypeId,
        Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()), RemovedRecipeListPayload::recipeIds,
        RemovedRecipeListPayload::new
    );

    public RemovedRecipeListPayload {
        recipeIds = List.copyOf(recipeIds);
    }

    @Override
    public Type<RemovedRecipeListPayload> type() {
        return TYPE;
    }
}
