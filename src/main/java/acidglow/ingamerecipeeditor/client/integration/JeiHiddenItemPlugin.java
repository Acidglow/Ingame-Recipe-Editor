package acidglow.ingamerecipeeditor.client.integration;

import java.util.Set;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IEditModeConfig;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import acidglow.ingamerecipeeditor.ModConstants;

/** Optional JEI bridge which mirrors the server-controlled hidden item list. */
@JeiPlugin
public final class JeiHiddenItemPlugin implements IModPlugin {
    private static IJeiRuntime runtime;
    private static Set<Identifier> appliedItems = Set.of();

    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "hidden_items");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        applyHiddenItems(ClientRecipeEditorPayloads.hiddenItems());
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
        appliedItems = Set.of();
    }

    /** Called reflectively so the main client code remains safe when JEI is absent. */
    public static void applyHiddenItems(Set<Identifier> hiddenItems) {
        if (runtime == null) {
            return;
        }
        IEditModeConfig visibility = runtime.getEditModeConfig();
        appliedItems.stream().filter(itemId -> !hiddenItems.contains(itemId)).forEach(itemId ->
            typedItem(itemId).ifPresent(item -> visibility.showIngredientUsingConfigFile(item, IEditModeConfig.HideMode.SINGLE))
        );
        hiddenItems.stream().filter(itemId -> !appliedItems.contains(itemId)).forEach(itemId ->
            typedItem(itemId).ifPresent(item -> visibility.hideIngredientUsingConfigFile(item, IEditModeConfig.HideMode.SINGLE))
        );
        appliedItems = Set.copyOf(hiddenItems);
    }

    private static java.util.Optional<mezz.jei.api.ingredients.ITypedIngredient<ItemStack>> typedItem(Identifier itemId) {
        Item item = BuiltInRegistries.ITEM.getValue(itemId);
        if (item == null || item.getDefaultInstance().isEmpty()) {
            return java.util.Optional.empty();
        }
        return runtime.getIngredientManager().createTypedIngredient(VanillaTypes.ITEM_STACK, new ItemStack(item), true);
    }
}
