package acidglow.ingamerecipeeditor.gametest;

import java.util.List;
import java.util.function.Consumer;
import com.google.gson.JsonObject;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import acidglow.ingamerecipeeditor.ModConstants;
import acidglow.ingamerecipeeditor.data.RecipeEditorSavedData;
import acidglow.ingamerecipeeditor.recipe.model.RecipeKey;
import acidglow.ingamerecipeeditor.recipe.model.RecipeSnapshot;
import acidglow.ingamerecipeeditor.recipe.model.RecipeState;
import acidglow.ingamerecipeeditor.recipe.service.RecipeOverlay;
import acidglow.ingamerecipeeditor.recipe.service.RecipeIngredientFactory;
import acidglow.ingamerecipeeditor.recipe.service.HiddenItemPurger;
import acidglow.ingamerecipeeditor.menu.RecipeEditorMenu;

/** Runtime smoke tests for persistence paths that require the NeoForge game-test server. */
public final class RecipeEditorGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(
        BuiltInRegistries.TEST_FUNCTION,
        ModConstants.MOD_ID
    );
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SAVED_DATA_ROUND_TRIP = TEST_FUNCTIONS.register(
        "saved_data_round_trip",
        () -> RecipeEditorGameTests::savedDataRoundTrip
    );
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TAGGED_INGREDIENT = TEST_FUNCTIONS.register(
        "tagged_ingredient",
        () -> RecipeEditorGameTests::taggedIngredient
    );
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> HIDDEN_BLOCK_PURGE = TEST_FUNCTIONS.register(
        "hidden_block_purge",
        () -> RecipeEditorGameTests::hiddenBlockPurge
    );
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> HIDDEN_EDITOR_INPUT = TEST_FUNCTIONS.register(
        "hidden_editor_input",
        () -> RecipeEditorGameTests::hiddenEditorInput
    );

    private RecipeEditorGameTests() {
    }

    public static void register(IEventBus eventBus) {
        TEST_FUNCTIONS.register(eventBus);
        eventBus.addListener(RecipeEditorGameTests::registerTests);
    }

    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
            Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "default"),
            new TestEnvironmentDefinition.AllOf(List.of())
        );
        TestData<Holder<TestEnvironmentDefinition<?>>> testData = new TestData<>(
            environment,
            Identifier.withDefaultNamespace("bastion/blocks/air"),
            20,
            0,
            true
        );
        event.registerTest(
            Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "saved_data_round_trip"),
            new FunctionGameTestInstance(SAVED_DATA_ROUND_TRIP.getKey(), testData)
        );
        event.registerTest(
            Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "tagged_ingredient"),
            new FunctionGameTestInstance(TAGGED_INGREDIENT.getKey(), testData)
        );
        event.registerTest(
            Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "hidden_block_purge"),
            new FunctionGameTestInstance(HIDDEN_BLOCK_PURGE.getKey(), testData)
        );
        event.registerTest(
            Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "hidden_editor_input"),
            new FunctionGameTestInstance(HIDDEN_EDITOR_INPUT.getKey(), testData)
        );
    }

    private static void savedDataRoundTrip(GameTestHelper helper) {
        RecipeSnapshot defaultRecipe = snapshot("oak_planks", "oak_planks");
        RecipeSnapshot customRecipe = snapshot("custom/oak_planks", "oak_planks");
        RecipeOverlay overlay = new RecipeOverlay();
        overlay.addDefault(defaultRecipe);
        overlay.remove(defaultRecipe.key());
        overlay.addCustom(customRecipe);

        RecipeEditorSavedData savedData = new RecipeEditorSavedData();
        savedData.replaceRecipeOverlay(overlay);
        RecipeOverlay restored = savedData.createRecipeOverlay();

        helper.succeedIf(() -> {
            helper.assertTrue(savedData.hasActiveRecipeChanges(), "Persisted overlay should require a reload.");
            helper.assertTrue(
                restored.state(defaultRecipe.key()).orElseThrow() instanceof RecipeState.RemovedDefaultRecipe,
                "Deleted default recipe was not restored as a tombstone."
            );
            helper.assertTrue(
                restored.state(customRecipe.key()).orElseThrow() instanceof RecipeState.NewCustomRecipe,
                "Custom recipe was not restored from saved data."
            );
        });
    }

    private static void taggedIngredient(GameTestHelper helper) {
        Identifier tagId = Identifier.withDefaultNamespace("planks");
        var ingredient = RecipeIngredientFactory.fromTag(tagId);

        helper.succeedIf(() -> {
            helper.assertTrue(ingredient.test(new ItemStack(Items.OAK_PLANKS)), "Tag ingredient should match oak planks.");
            helper.assertTrue(ingredient.test(new ItemStack(Items.SPRUCE_PLANKS)), "Tag ingredient should match spruce planks.");
            helper.assertTrue(
                ingredient.getValues().unwrap().left().filter(TagKey.create(Registries.ITEM, tagId)::equals).isPresent(),
                "Selected ingredient should retain its named item tag."
            );
        });
    }

    private static void hiddenBlockPurge(GameTestHelper helper) {
        BlockPos relativePos = new BlockPos(1, 1, 1);
        helper.setBlock(relativePos, Blocks.DIAMOND_BLOCK);
        BlockPos absolutePos = helper.absolutePos(relativePos);
        LevelChunk chunk = helper.getLevel().getChunkAt(absolutePos);

        HiddenItemPurger.purgeChunk(helper.getLevel(), chunk, java.util.Set.of(Identifier.withDefaultNamespace("diamond_block")));

        helper.succeedIf(() -> helper.assertTrue(
            helper.getBlockState(relativePos).isAir(),
            "A hidden item's placed block should be removed without dropping an item."
        ));
    }

    private static void hiddenEditorInput(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = (net.minecraft.server.level.ServerPlayer)helper.makeMockServerPlayer(GameType.CREATIVE);
        RecipeEditorMenu menu = new RecipeEditorMenu(1, player.getInventory());
        menu.setPreviewOutput(new ItemStack(Items.DIAMOND));
        player.containerMenu = menu;

        HiddenItemPurger.purgePlayerStorage(player, java.util.Set.of(Identifier.withDefaultNamespace("diamond")));

        helper.succeedIf(() -> helper.assertTrue(
            menu.getSlot(0).getItem().is(Items.DIAMOND),
            "The editor input must retain the selected hidden item so it can be revealed."
        ));
    }

    private static RecipeSnapshot snapshot(String recipePath, String outputPath) {
        Identifier recipeId = Identifier.fromNamespaceAndPath("test", recipePath);
        return new RecipeSnapshot(
            new RecipeKey(ResourceKey.create(Registries.RECIPE, recipeId), Identifier.withDefaultNamespace("crafting")),
            Identifier.fromNamespaceAndPath("test", outputPath),
            new JsonObject()
        );
    }
}
