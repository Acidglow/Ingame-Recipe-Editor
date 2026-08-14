package acidglow.ingamerecipeeditor.menu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import acidglow.ingamerecipeeditor.ModConstants;

/** Registers the server-backed menu used by the in-game recipe editor. */
public final class RecipeEditorMenus {
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ModConstants.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<RecipeEditorMenu>> RECIPE_EDITOR = MENUS.register(
        "recipe_editor",
        () -> IMenuTypeExtension.create(RecipeEditorMenu::new)
    );

    private RecipeEditorMenus() {
    }

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
