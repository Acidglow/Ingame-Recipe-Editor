package acidglow.ingamerecipeeditor.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import acidglow.ingamerecipeeditor.ModConstants;

/** The active preview recipe and the valid action for the Remove Recipe button. */
public record RecipeEditorSelectionPayload(
    int containerId,
    Identifier recipeId,
    Identifier recipeTypeId,
    Identifier outputItemId,
    Action action
) implements CustomPacketPayload {
    public enum Action {
        REMOVE,
        RESTORE_DEFAULT,
        NO_DEFAULT
    }

    public static final Type<RecipeEditorSelectionPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "recipe_editor_selection")
    );
    private static final StreamCodec<ByteBuf, Action> ACTION_CODEC = ByteBufCodecs.VAR_INT.map(
        RecipeEditorSelectionPayload::actionFromId,
        Action::ordinal
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, RecipeEditorSelectionPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, RecipeEditorSelectionPayload::containerId,
        Identifier.STREAM_CODEC, RecipeEditorSelectionPayload::recipeId,
        Identifier.STREAM_CODEC, RecipeEditorSelectionPayload::recipeTypeId,
        Identifier.STREAM_CODEC, RecipeEditorSelectionPayload::outputItemId,
        ACTION_CODEC, RecipeEditorSelectionPayload::action,
        RecipeEditorSelectionPayload::new
    );

    private static Action actionFromId(int value) {
        Action[] actions = Action.values();
        if (value < 0 || value >= actions.length) {
            throw new IllegalArgumentException("Unknown recipe-editor selection action: " + value);
        }
        return actions[value];
    }

    @Override
    public Type<RecipeEditorSelectionPayload> type() {
        return TYPE;
    }
}
