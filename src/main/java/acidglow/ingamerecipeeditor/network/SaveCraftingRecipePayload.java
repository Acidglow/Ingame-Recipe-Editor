package acidglow.ingamerecipeeditor.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import acidglow.ingamerecipeeditor.ModConstants;

/** Saves the current server-owned crafting draft as a new custom recipe. */
public record SaveCraftingRecipePayload() implements CustomPacketPayload {
    public static final Type<SaveCraftingRecipePayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "save_crafting_recipe")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, SaveCraftingRecipePayload> STREAM_CODEC = StreamCodec.unit(
        new SaveCraftingRecipePayload()
    );

    @Override
    public Type<SaveCraftingRecipePayload> type() {
        return TYPE;
    }
}
