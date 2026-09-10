package acidglow.ingamerecipeeditor.gametest;

import java.util.List;
import java.util.function.Consumer;
import com.google.gson.JsonObject;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
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
import acidglow.ingamerecipeeditor.network.RecipeEditorPayloads;
import acidglow.ingamerecipeeditor.network.SaveCookingRecipePayload;

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
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> HIDDEN_ITEM_FRAME_PURGE = TEST_FUNCTIONS.register(
        "hidden_item_frame_purge",
        () -> RecipeEditorGameTests::hiddenItemFramePurge
    );
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> HIDDEN_DROPPED_ITEM_STACKS_PURGE = TEST_FUNCTIONS.register(
        "hidden_dropped_item_stacks_purge",
        () -> RecipeEditorGameTests::hiddenDroppedItemStacksPurge
    );
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> INVALID_COOKING_SAVE_REJECTED = TEST_FUNCTIONS.register(
        "invalid_cooking_save_rejected",
        () -> RecipeEditorGameTests::invalidCookingSaveRejected
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
        event.registerTest(
            Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "hidden_item_frame_purge"),
            new FunctionGameTestInstance(HIDDEN_ITEM_FRAME_PURGE.getKey(), testData)
        );
        event.registerTest(
            Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "hidden_dropped_item_stacks_purge"),
            new FunctionGameTestInstance(HIDDEN_DROPPED_ITEM_STACKS_PURGE.getKey(), testData)
        );
        event.registerTest(
            Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "invalid_cooking_save_rejected"),
            new FunctionGameTestInstance(INVALID_COOKING_SAVE_REJECTED.getKey(), testData)
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

    private static void hiddenItemFramePurge(GameTestHelper helper) {
        BlockPos relativePos = new BlockPos(2, 1, 2);
        BlockPos absolutePos = helper.absolutePos(relativePos);
        ItemFrame itemFrame = new ItemFrame(helper.getLevel(), absolutePos, Direction.NORTH);
        itemFrame.setItem(new ItemStack(Items.DIAMOND));
        helper.getLevel().addFreshEntity(itemFrame);
        LevelChunk chunk = helper.getLevel().getChunkAt(absolutePos);

        HiddenItemPurger.purgeChunk(helper.getLevel(), chunk, java.util.Set.of(Identifier.withDefaultNamespace("diamond")));

        helper.succeedIf(() -> helper.assertTrue(
            itemFrame.getItem().isEmpty(),
            "A hidden item must be removed from an item frame."
        ));
    }

    private static void hiddenDroppedItemStacksPurge(GameTestHelper helper) {
        BlockPos absolutePos = helper.absolutePos(new BlockPos(3, 1, 3));
        ItemEntity firstStack = new ItemEntity(
            helper.getLevel(), absolutePos.getX() + 0.5, absolutePos.getY() + 0.5, absolutePos.getZ() + 0.5,
            new ItemStack(Items.DIAMOND, 2)
        );
        ItemEntity secondStack = new ItemEntity(
            helper.getLevel(), absolutePos.getX() + 0.5, absolutePos.getY() + 0.5, absolutePos.getZ() + 0.5,
            new ItemStack(Items.DIAMOND, 3)
        );
        helper.getLevel().addFreshEntity(firstStack);
        helper.getLevel().addFreshEntity(secondStack);

        HiddenItemPurger.purgeWorldEntities(helper.getLevel(), java.util.Set.of(Identifier.withDefaultNamespace("diamond")));

        helper.succeedIf(() -> helper.assertTrue(
            firstStack.isRemoved() && secondStack.isRemoved(),
            "All hidden dropped stacks at the same position must be removed."
        ));
    }

    private static void invalidCookingSaveRejected(GameTestHelper helper) {
        RecipeEditorSavedData savedData = RecipeEditorSavedData.get(helper.getLevel());
        RecipeOverlay before = savedData.createRecipeOverlay();
        java.util.Set<Identifier> hiddenItemsBefore = savedData.hiddenItems();
        Identifier smelting = Identifier.withDefaultNamespace("smelting");
        Identifier diamond = Identifier.withDefaultNamespace("diamond");
        Identifier air = Identifier.withDefaultNamespace("air");
        Identifier stone = Identifier.withDefaultNamespace("stone");

        List<SaveCookingRecipePayload> invalidRequests = List.of(
            new SaveCookingRecipePayload(air, stone, smelting, 1, 0.0F, 200),
            new SaveCookingRecipePayload(diamond, air, smelting, 1, 0.0F, 200),
            new SaveCookingRecipePayload(diamond, stone, smelting, 0, 0.0F, 200),
            new SaveCookingRecipePayload(diamond, stone, smelting, 100, 0.0F, 200),
            new SaveCookingRecipePayload(diamond, stone, smelting, 1, -1.0F, 200),
            new SaveCookingRecipePayload(diamond, stone, smelting, 1, 0.0F, 0),
            new SaveCookingRecipePayload(diamond, stone, Identifier.withDefaultNamespace("crafting"), 1, 0.0F, 200)
        );
        invalidRequests.forEach(payload -> helper.assertTrue(
            RecipeEditorPayloads.validateCookingRecipeSave(payload).isPresent(),
            "The server must reject malformed cooking-save payloads."
        ));

        helper.succeedIf(() -> {
            RecipeOverlay after = savedData.createRecipeOverlay();
            helper.assertTrue(
                after.customRecipes().size() == before.customRecipes().size(),
                "Invalid cooking saves must not add custom recipes."
            );
            helper.assertTrue(
                after.tombstones().size() == before.tombstones().size(),
                "Invalid cooking saves must not remove default recipes."
            );
            helper.assertTrue(
                savedData.hiddenItems().equals(hiddenItemsBefore),
                "Invalid cooking saves must not change hidden-item state."
            );
        });
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
