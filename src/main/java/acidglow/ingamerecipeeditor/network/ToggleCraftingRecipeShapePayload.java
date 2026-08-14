package acidglow.ingamerecipeeditor.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import acidglow.ingamerecipeeditor.ModConstants;

/** Toggles the unsaved crafting-recipe draft between shaped and shapeless. */
public record ToggleCraftingRecipeShapePayload() implements CustomPacketPayload {
    public static final Type<ToggleCraftingRecipeShapePayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "toggle_crafting_recipe_shape")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleCraftingRecipeShapePayload> STREAM_CODEC = StreamCodec.unit(
        new ToggleCraftingRecipeShapePayload()
    );

    @Override
    public Type<ToggleCraftingRecipeShapePayload> type() {
        return TYPE;
    }
}
