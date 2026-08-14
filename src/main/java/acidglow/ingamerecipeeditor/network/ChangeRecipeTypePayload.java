package acidglow.ingamerecipeeditor.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import acidglow.ingamerecipeeditor.ModConstants;

/** Cycles the editor's supported recipe type. */
public record ChangeRecipeTypePayload() implements CustomPacketPayload {
    public static final Type<ChangeRecipeTypePayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "change_recipe_type")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ChangeRecipeTypePayload> STREAM_CODEC =
        StreamCodec.unit(new ChangeRecipeTypePayload());

    @Override
    public Type<ChangeRecipeTypePayload> type() {
        return TYPE;
    }
}
