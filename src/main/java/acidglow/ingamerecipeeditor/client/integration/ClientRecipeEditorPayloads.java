package acidglow.ingamerecipeeditor.client.integration;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import acidglow.ingamerecipeeditor.AcidglowsIngameRecipeEditor;
import acidglow.ingamerecipeeditor.network.EditorOperationResultPayload;
import acidglow.ingamerecipeeditor.network.RecipePreviewTagsPayload;
import acidglow.ingamerecipeeditor.network.RecipeEditorSelectionPayload;
import acidglow.ingamerecipeeditor.network.HiddenItemsPayload;
import acidglow.ingamerecipeeditor.network.RemovedRecipeListPayload;

/** Receives server-validated recipe operation feedback on the client. */
@EventBusSubscriber(modid = AcidglowsIngameRecipeEditor.MODID, value = Dist.CLIENT)
public final class ClientRecipeEditorPayloads {
    private static int previewTagContainerId = -1;
    private static List<Optional<Identifier>> previewTags = List.of();
    private static RecipeEditorSelectionPayload activeSelection;
    private static Set<Identifier> hiddenItems = Set.of();
    private static RemovedRecipeListPayload removedRecipes;

    private ClientRecipeEditorPayloads() {
    }

    @SubscribeEvent
    public static void register(RegisterClientPayloadHandlersEvent event) {
        event.register(EditorOperationResultPayload.TYPE, (payload, context) -> Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.sendSystemMessage(Component.literal(payload.message()));
            }
        }));
        event.register(RecipePreviewTagsPayload.TYPE, (payload, context) -> Minecraft.getInstance().execute(() -> {
            previewTagContainerId = payload.containerId();
            previewTags = payload.ingredientTags();
        }));
        event.register(RecipeEditorSelectionPayload.TYPE, (payload, context) -> Minecraft.getInstance().execute(() -> activeSelection = payload));
        event.register(RemovedRecipeListPayload.TYPE, (payload, context) -> Minecraft.getInstance().execute(() -> removedRecipes = payload));
        event.register(HiddenItemsPayload.TYPE, (payload, context) -> Minecraft.getInstance().execute(() -> {
            hiddenItems = payload.itemIds();
            refreshJeiVisibility();
        }));
    }

    /** Returns the exact tag used by the active recipe for one displayed grid slot. */
    public static Optional<Identifier> recipePreviewTag(int containerId, int gridSlot) {
        return containerId == previewTagContainerId && gridSlot >= 0 && gridSlot < previewTags.size()
            ? previewTags.get(gridSlot)
            : Optional.empty();
    }

    public static Optional<RecipeEditorSelectionPayload> activeSelection(int containerId) {
        return activeSelection != null && activeSelection.containerId() == containerId ? Optional.of(activeSelection) : Optional.empty();
    }

    /** Deleted default recipes for one exact output and recipe type. */
    public static List<Identifier> removedRecipes(int containerId, Identifier outputItemId, Identifier recipeTypeId) {
        return removedRecipes != null
            && removedRecipes.containerId() == containerId
            && removedRecipes.outputItemId().equals(outputItemId)
            && removedRecipes.recipeTypeId().equals(recipeTypeId)
            ? removedRecipes.recipeIds()
            : List.of();
    }

    public static boolean isItemHidden(Identifier itemId) {
        return hiddenItems.contains(itemId);
    }

    public static Set<Identifier> hiddenItems() {
        return hiddenItems;
    }

    private static void refreshJeiVisibility() {
        if (!net.neoforged.fml.ModList.get().isLoaded("jei")) {
            return;
        }
        try {
            Class.forName("acidglow.ingamerecipeeditor.client.integration.JeiHiddenItemPlugin")
                .getMethod("applyHiddenItems", Set.class)
                .invoke(null, hiddenItems);
        } catch (ReflectiveOperationException | LinkageError exception) {
            AcidglowsIngameRecipeEditor.LOGGER.warn("Could not synchronize hidden items with JEI", exception);
        }
    }
}
