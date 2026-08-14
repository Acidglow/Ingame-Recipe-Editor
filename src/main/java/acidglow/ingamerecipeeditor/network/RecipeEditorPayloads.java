package acidglow.ingamerecipeeditor.network;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.item.crafting.display.FurnaceRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import acidglow.ingamerecipeeditor.AcidglowsIngameRecipeEditor;
import acidglow.ingamerecipeeditor.ModConstants;
import acidglow.ingamerecipeeditor.command.EditorPermissions;
import acidglow.ingamerecipeeditor.recipe.adapter.BuiltinRecipeEditorAdapters;
import acidglow.ingamerecipeeditor.recipe.model.RecipeKey;
import acidglow.ingamerecipeeditor.recipe.model.CustomRecipeIdFactory;
import acidglow.ingamerecipeeditor.recipe.model.RecipeJsonCodec;
import acidglow.ingamerecipeeditor.recipe.model.RecipeSnapshot;
import acidglow.ingamerecipeeditor.recipe.model.RecipeState;
import acidglow.ingamerecipeeditor.recipe.service.ServerRecipeEditorService;
import acidglow.ingamerecipeeditor.menu.RecipeEditorMenu;

/** Registers the server-authoritative payloads used by the recipe editor menu. */
@EventBusSubscriber(modid = AcidglowsIngameRecipeEditor.MODID)
public final class RecipeEditorPayloads {
    private RecipeEditorPayloads() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(ModConstants.NETWORK_VERSION)
            .playToClient(EditorOperationResultPayload.TYPE, EditorOperationResultPayload.STREAM_CODEC)
            .playToClient(RecipePreviewTagsPayload.TYPE, RecipePreviewTagsPayload.STREAM_CODEC)
            .playToClient(RecipeEditorSelectionPayload.TYPE, RecipeEditorSelectionPayload.STREAM_CODEC)
            .playToClient(RemovedRecipeListPayload.TYPE, RemovedRecipeListPayload.STREAM_CODEC)
            .playToClient(HiddenItemsPayload.TYPE, HiddenItemsPayload.STREAM_CODEC)
            .playToServer(MutateRecipePayload.TYPE, MutateRecipePayload.STREAM_CODEC,
                (payload, context) -> handleRecipeMutation(context.player() instanceof ServerPlayer player ? player : null, payload))
            .playToServer(SaveCookingRecipePayload.TYPE, SaveCookingRecipePayload.STREAM_CODEC,
                (payload, context) -> handleSaveCookingRecipe(context.player() instanceof ServerPlayer player ? player : null, payload))
            .playToServer(SaveCraftingRecipePayload.TYPE, SaveCraftingRecipePayload.STREAM_CODEC,
                (payload, context) -> handleSaveCraftingRecipe(context.player() instanceof ServerPlayer player ? player : null))
            .playToServer(PlaceEditorItemPayload.TYPE, PlaceEditorItemPayload.STREAM_CODEC,
                (payload, context) -> handleEditorItemPlacement(context.player() instanceof ServerPlayer player ? player : null, payload))
            .playToServer(ClearEditorItemPayload.TYPE, ClearEditorItemPayload.STREAM_CODEC,
                (payload, context) -> handleEditorItemClear(context.player() instanceof ServerPlayer player ? player : null, payload))
            .playToServer(AdjustEditorItemCountPayload.TYPE, AdjustEditorItemCountPayload.STREAM_CODEC,
                (payload, context) -> handleInputItemCountAdjustment(context.player() instanceof ServerPlayer player ? player : null, payload))
            .playToServer(DiscardCarriedItemPayload.TYPE, DiscardCarriedItemPayload.STREAM_CODEC,
                (payload, context) -> handleCarriedItemDiscard(context.player() instanceof ServerPlayer player ? player : null))
            .playToServer(ChangeRecipeTypePayload.TYPE, ChangeRecipeTypePayload.STREAM_CODEC,
                (payload, context) -> handleRecipeTypeChange(context.player() instanceof ServerPlayer player ? player : null))
            .playToServer(NavigateEditorRecipePayload.TYPE, NavigateEditorRecipePayload.STREAM_CODEC,
                (payload, context) -> handleRecipeNavigation(context.player() instanceof ServerPlayer player ? player : null, payload.direction()))
            .playToServer(SelectRemovedRecipePayload.TYPE, SelectRemovedRecipePayload.STREAM_CODEC,
                (payload, context) -> handleRemovedRecipeSelection(context.player() instanceof ServerPlayer player ? player : null, payload))
            .playToServer(SelectRecipePreviewTagPayload.TYPE, SelectRecipePreviewTagPayload.STREAM_CODEC,
                (payload, context) -> handleRecipePreviewTagSelection(context.player() instanceof ServerPlayer player ? player : null, payload))
            .playToServer(SetItemHiddenPayload.TYPE, SetItemHiddenPayload.STREAM_CODEC,
                (payload, context) -> handleItemHiddenChange(context.player() instanceof ServerPlayer player ? player : null, payload))
            .playToServer(ToggleCraftingRecipeShapePayload.TYPE, ToggleCraftingRecipeShapePayload.STREAM_CODEC,
                (payload, context) -> handleCraftingRecipeShapeToggle(context.player() instanceof ServerPlayer player ? player : null));
    }

    private static void handleRecipeMutation(ServerPlayer player, MutateRecipePayload payload) {
        if (player == null) {
            return;
        }
        if (!EditorPermissions.mayUseEditor(player)) {
            rejectMutation(player, "You do not have permission to modify recipes.");
            return;
        }
        if (!BuiltInRegistries.RECIPE_TYPE.containsKey(payload.recipeTypeId())) {
            rejectMutation(player, "The requested recipe type is not registered.");
            return;
        }

        RecipeKey key = new RecipeKey(ResourceKey.create(Registries.RECIPE, payload.recipeId()), payload.recipeTypeId());
        ServerRecipeEditorService service = new ServerRecipeEditorService(BuiltinRecipeEditorAdapters.create());
        Optional<net.minecraft.resources.Identifier> previewOutputId = previewOutputId(player);
        if (previewOutputId.isEmpty()) {
            rejectMutation(player, "The recipe editor no longer has an output item.");
            return;
        }
        var overlay = acidglow.ingamerecipeeditor.data.RecipeEditorSavedData.get(player.level()).createRecipeOverlay();
        if (payload.restoreDefault()) {
            Optional<RecipeState.DefaultRecipe> storedDefault = overlay.defaultRecipes().stream()
                .filter(state -> state.snapshot().key().equals(key))
                .findFirst();
            if (storedDefault.isEmpty()) {
                rejectMutation(player, "No stored default exists for that recipe.");
                return;
            }
            if (!storedDefault.get().snapshot().outputItemId().equals(previewOutputId.get())) {
                rejectMutation(player, "That recipe is no longer the active preview.");
                return;
            }
            try {
                completeMutation(player, service.restoreDefault(player.level(), key), "Default recipe restored.",
                    () -> refreshRecipePreview(player, key));
            } catch (IllegalArgumentException exception) {
                rejectMutation(player, exception.getMessage());
            }
            return;
        }

        if (!isActiveRecipePreview(player, key)) {
            rejectMutation(player, "That recipe is no longer the active preview.");
            return;
        }

        var holder = player.level().recipeAccess().byKey(key.recipeId());
        if (holder.isEmpty() || !RecipeKey.from(holder.get()).recipeTypeId().equals(key.recipeTypeId())) {
            rejectMutation(player, "The requested recipe no longer exists with that type.");
            return;
        }
        if (service.capture(player.level(), key).isEmpty()) {
            rejectMutation(player, "That recipe type is not safely editable yet.");
            return;
        }
        boolean hasOriginalDefault = !(overlay.state(key).orElse(null) instanceof RecipeState.NewCustomRecipe);
        try {
            completeMutation(player, service.remove(player.level(), key), "Recipe removed.",
                () -> {
                    sendSelection(player, key, previewOutputId.get(), hasOriginalDefault
                        ? RecipeEditorSelectionPayload.Action.RESTORE_DEFAULT
                        : RecipeEditorSelectionPayload.Action.NO_DEFAULT);
                    refreshRecipePreviewKeepingSelection(player);
                });
        } catch (IllegalArgumentException exception) {
            rejectMutation(player, exception.getMessage());
        }
    }

    private static void completeMutation(ServerPlayer player, java.util.concurrent.CompletableFuture<Void> reload, String successMessage) {
        completeMutation(player, reload, successMessage, () -> { });
    }

    private static void completeMutation(
        ServerPlayer player,
        java.util.concurrent.CompletableFuture<Void> reload,
        String successMessage,
        Runnable successAction
    ) {
        reload.whenComplete((ignored, error) -> player.level().getServer().execute(() -> {
            if (error != null) {
                AcidglowsIngameRecipeEditor.LOGGER.error("Recipe editor reload failed after a mutation requested by {}", player.getPlainTextName(), error);
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                    new EditorOperationResultPayload(false, "The reload failed. The saved change will be retried on the next reload."));
            } else {
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                    new EditorOperationResultPayload(true, successMessage));
                successAction.run();
            }
        }));
    }

    private static void rejectMutation(ServerPlayer player, String message) {
        AcidglowsIngameRecipeEditor.LOGGER.warn("Rejected recipe editor mutation from {}: {}", player.getPlainTextName(), message);
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new EditorOperationResultPayload(false, message));
    }

    private static void handleItemHiddenChange(ServerPlayer player, SetItemHiddenPayload payload) {
        if (!mayUseOpenEditor(player)) {
            return;
        }
        RecipeEditorMenu menu = (RecipeEditorMenu)player.containerMenu;
        ItemStack input = menu.getSlot(0).getItem();
        if (input.isEmpty() || !BuiltInRegistries.ITEM.getKey(input.getItem()).equals(payload.itemId())) {
            rejectMutation(player, "The requested item is no longer in the editor input slot.");
            return;
        }
        var savedData = acidglow.ingamerecipeeditor.data.RecipeEditorSavedData.get(player.level());
        savedData.setHidden(payload.itemId(), payload.hidden());
        if (payload.hidden()) {
            acidglow.ingamerecipeeditor.recipe.service.HiddenItemPurger.purgeLoadedItems(
                player.level().getServer(), savedData.hiddenItems()
            );
        }
        broadcastHiddenItems(player.level().getServer(), savedData.hiddenItems());
        try {
            completeMutation(player, acidglow.ingamerecipeeditor.recipe.service.RecipeReloadService.reload(player.level().getServer()),
                payload.hidden() ? "Item hidden." : "Item revealed.",
                () -> {
                    if (payload.hidden()) {
                        clearRecipePreview(player);
                    } else {
                        refreshRecipePreview(player);
                    }
                });
        } catch (IllegalArgumentException exception) {
            rejectMutation(player, exception.getMessage());
        }
    }

    private static void handleCraftingRecipeShapeToggle(ServerPlayer player) {
        if (!mayUseOpenEditor(player)) {
            return;
        }
        RecipeEditorMenu menu = (RecipeEditorMenu)player.containerMenu;
        if (menu.recipeType() != RecipeEditorMenu.TYPE_CRAFTING) {
            return;
        }
        menu.toggleCraftingRecipeKind();
        menu.broadcastChanges();
    }

    public static void sendHiddenItems(ServerPlayer player) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
            new HiddenItemsPayload(acidglow.ingamerecipeeditor.data.RecipeEditorSavedData.get(player.level()).hiddenItems()));
    }

    private static void broadcastHiddenItems(net.minecraft.server.MinecraftServer server, java.util.Set<net.minecraft.resources.Identifier> itemIds) {
        HiddenItemsPayload payload = new HiddenItemsPayload(itemIds);
        server.getPlayerList().getPlayers().forEach(player ->
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, payload)
        );
    }

    private static void handleSaveCookingRecipe(ServerPlayer player, SaveCookingRecipePayload payload) {
        if (player == null) {
            return;
        }
        if (!EditorPermissions.mayUseEditor(player)) {
            rejectMutation(player, "You do not have permission to modify recipes.");
            return;
        }
        if (!isSupportedCookingType(payload.recipeTypeId())) {
            rejectMutation(player, "Only the supported vanilla cooking recipe types can be created here.");
            return;
        }
        if (!isUsableItem(payload.outputItemId()) || !isUsableItem(payload.inputItemId())) {
            rejectMutation(player, "Input and output must be registered non-air items.");
            return;
        }
        if (payload.outputCount() < 1 || payload.outputCount() > 99) {
            rejectMutation(player, "Output count must be between 1 and 99.");
            return;
        }
        if (!Float.isFinite(payload.experience()) || payload.experience() < 0.0F || payload.experience() > 100.0F) {
            rejectMutation(player, "Experience must be between 0 and 100.");
            return;
        }
        if (payload.cookingTime() < 1 || payload.cookingTime() > 72_000) {
            rejectMutation(player, "Cooking time must be between 1 and 72000 ticks.");
            return;
        }

        Item input = BuiltInRegistries.ITEM.getValue(payload.inputItemId());
        Item output = BuiltInRegistries.ITEM.getValue(payload.outputItemId());
        Recipe<?> recipe = createCookingRecipe(payload, input, output);
        RecipeKey key = CustomRecipeIdFactory.create(payload.recipeTypeId());
        RecipeHolder<?> holder = new RecipeHolder<>(key.recipeId(), recipe);
        java.util.Optional<RecipeSnapshot> snapshot = RecipeJsonCodec.encode(holder, payload.outputItemId(), player.level().registryAccess())
            .resultOrPartial(error -> AcidglowsIngameRecipeEditor.LOGGER.warn("Could not encode new cooking recipe {}: {}", key.identifier(), error));
        if (snapshot.isEmpty()) {
            rejectMutation(player, "The new cooking recipe could not be encoded.");
            return;
        }

        try {
            ServerRecipeEditorService service = new ServerRecipeEditorService(BuiltinRecipeEditorAdapters.create());
            completeMutation(player, service.addCustom(player.level(), snapshot.get()), "New cooking recipe saved.",
                () -> refreshRecipePreview(player));
        } catch (IllegalArgumentException exception) {
            rejectMutation(player, exception.getMessage());
        }
    }

    private static void handleSaveCraftingRecipe(ServerPlayer player) {
        if (!mayUseOpenEditor(player)) {
            return;
        }
        RecipeEditorMenu menu = (RecipeEditorMenu)player.containerMenu;
        if (menu.recipeType() != RecipeEditorMenu.TYPE_CRAFTING) {
            rejectMutation(player, "Crafting recipes can only be saved while Crafting Table is selected.");
            return;
        }
        ItemStack outputStack = menu.getSlot(0).getItem();
        if (outputStack.isEmpty() || outputStack.getCount() > 99) {
            rejectMutation(player, "The output item must be a non-empty stack of at most 99 items.");
            return;
        }
        List<ItemStack> ingredients = java.util.stream.IntStream.rangeClosed(1, 9)
            .mapToObj(slotIndex -> menu.getSlot(slotIndex).getItem().copyWithCount(1))
            .toList();
        if (ingredients.stream().allMatch(ItemStack::isEmpty)) {
            rejectMutation(player, "A crafting recipe needs at least one ingredient.");
            return;
        }

        Recipe<?> recipe;
        try {
            recipe = createCraftingRecipe(ingredients, outputStack, menu.craftingRecipeKind());
        } catch (IllegalArgumentException exception) {
            rejectMutation(player, exception.getMessage());
            return;
        }
        net.minecraft.resources.Identifier craftingType = net.minecraft.resources.Identifier.withDefaultNamespace("crafting");
        RecipeKey key = CustomRecipeIdFactory.create(craftingType);
        RecipeHolder<?> holder = new RecipeHolder<>(key.recipeId(), recipe);
        net.minecraft.resources.Identifier outputItemId = BuiltInRegistries.ITEM.getKey(outputStack.getItem());
        Optional<RecipeSnapshot> snapshot = RecipeJsonCodec.encode(holder, outputItemId, player.level().registryAccess())
            .resultOrPartial(error -> AcidglowsIngameRecipeEditor.LOGGER.warn("Could not encode new crafting recipe {}: {}", key.identifier(), error));
        if (snapshot.isEmpty()) {
            rejectMutation(player, "The new crafting recipe could not be encoded.");
            return;
        }
        try {
            ServerRecipeEditorService service = new ServerRecipeEditorService(BuiltinRecipeEditorAdapters.create());
            completeMutation(player, service.addCustom(player.level(), snapshot.get()), "New crafting recipe saved.",
                () -> refreshRecipePreview(player));
        } catch (IllegalArgumentException exception) {
            rejectMutation(player, exception.getMessage());
        }
    }

    private static void handleEditorItemPlacement(ServerPlayer player, PlaceEditorItemPayload payload) {
        if (player == null) {
            return;
        }
        if (!EditorPermissions.mayUseEditor(player)) {
            rejectMutation(player, "You do not have permission to use the recipe editor.");
            return;
        }
        if (payload.slotIndex() < 0 || payload.slotIndex() >= 46 || !isUsableItem(payload.itemId())) {
            rejectMutation(player, "That item or recipe-editor slot is invalid.");
            return;
        }
        if (!(player.containerMenu instanceof acidglow.ingamerecipeeditor.menu.RecipeEditorMenu menu)) {
            rejectMutation(player, "The recipe editor is no longer open.");
            return;
        }
        if (menu.isCookingRecipeType() && payload.slotIndex() >= 1 && payload.slotIndex() < 10 && payload.slotIndex() != 5) {
            rejectMutation(player, "Only the centre ingredient slot is active for this recipe type.");
            return;
        }

        Item item = BuiltInRegistries.ITEM.getValue(payload.itemId());
        if (payload.slotIndex() < 10
            ? menu.placeEditorItem(payload.slotIndex(), new net.minecraft.world.item.ItemStack(item))
            : menu.placePlayerInventoryItem(payload.slotIndex(), new net.minecraft.world.item.ItemStack(item), payload.fullStack())) {
            if (payload.slotIndex() == 0) {
                populateRecipePreview(player, menu, item);
            }
            menu.broadcastChanges();
        }
    }

    private static void populateRecipePreview(ServerPlayer player, RecipeEditorMenu menu, Item output) {
        populateRecipePreview(player, menu, output, true);
    }

    /** Rebuilds the ghost grid, optionally retaining an already selected remove/restore action. */
    private static void populateRecipePreview(ServerPlayer player, RecipeEditorMenu menu, Item output, boolean updateSelection) {
        List<RecipeHolder<?>> recipes = player.level().recipeAccess().getRecipes().stream()
            .filter(recipeHolder -> isRecipeOfType(recipeHolder, menu.recipeType()))
            .filter(recipeHolder -> recipeOutputIs(recipeHolder.value(), output))
            .sorted(Comparator.comparing(recipeHolder -> recipeHolder.id().toString()))
            .toList();
        if (recipes.isEmpty()) {
            menu.setCraftingPreview(List.of(), 0);
            menu.setRecipeNavigation(0, 0);
            sendRemovedRecipeList(player, menu, output);
            removedDefaultForOutput(player, menu, output).ifPresent(key ->
                sendSelection(player, key, BuiltInRegistries.ITEM.getKey(output), RecipeEditorSelectionPayload.Action.RESTORE_DEFAULT)
            );
            sendPreviewTags(player, menu, emptyPreviewTags());
            return;
        }

        int position = Math.clamp(menu.recipePosition(), 0, recipes.size() - 1);
        RecipeHolder<?> holder = recipes.get(position);
        populateRecipePreview(player, menu, holder, position, recipes.size(), updateSelection);
        sendRemovedRecipeList(player, menu, output);
    }

    /** Renders a live recipe or a decoded deleted-default snapshot into the editor's ghost grid. */
    private static void populateRecipePreview(
        ServerPlayer player,
        RecipeEditorMenu menu,
        RecipeHolder<?> holder,
        int position,
        int recipeCount,
        boolean updateSelection
    ) {
        if (updateSelection) {
            ItemStack output = recipeOutput(holder.value());
            if (!output.isEmpty()) {
                sendSelection(player, RecipeKey.from(holder), BuiltInRegistries.ITEM.getKey(output.getItem()), RecipeEditorSelectionPayload.Action.REMOVE);
            }
        }
        ItemStack recipeOutput = recipeOutput(holder.value());
        if (!recipeOutput.isEmpty()) {
            menu.setPreviewOutput(recipeOutput);
        }

        var display = holder.value().display().getFirst();
        var context = SlotDisplayContext.fromLevel(player.level());
        List<Optional<net.minecraft.resources.Identifier>> previewTags = emptyPreviewTags();
        if (display instanceof ShapedCraftingRecipeDisplay shaped) {
            List<List<ItemStack>> preview = new java.util.ArrayList<>(java.util.Collections.nCopies(9, List.of()));
            for (int row = 0; row < shaped.height(); row++) {
                for (int column = 0; column < shaped.width(); column++) {
                    int sourceIndex = row * shaped.width() + column;
                    preview.set(row * 3 + column, shaped.ingredients().get(sourceIndex).resolveForStacks(context));
                    previewTags.set(row * 3 + column, recipeIngredientTag(shaped.ingredients().get(sourceIndex)));
                }
            }
            menu.setCraftingPreview(preview, 1);
        } else if (display instanceof ShapelessCraftingRecipeDisplay shapeless) {
            List<List<ItemStack>> preview = shapeless.ingredients().stream()
                .map(ingredient -> ingredient.resolveForStacks(context))
                .toList();
            for (int index = 0; index < preview.size(); index++) {
                previewTags.set(index, recipeIngredientTag(shapeless.ingredients().get(index)));
            }
            menu.setCraftingPreview(preview, 2);
        } else if (display instanceof FurnaceRecipeDisplay cooking) {
            menu.setCraftingPreview(List.of(List.of(), List.of(), List.of(), List.of(), cooking.ingredient().resolveForStacks(context),
                List.of(), List.of(), List.of(), List.of()), 0);
            previewTags.set(4, recipeIngredientTag(cooking.ingredient()));
        } else {
            menu.setCraftingPreview(List.of(), 0);
        }
        menu.setRecipeNavigation(position, recipeCount);
        sendPreviewTags(player, menu, previewTags);
    }

    private static List<Optional<net.minecraft.resources.Identifier>> emptyPreviewTags() {
        return new java.util.ArrayList<>(java.util.Collections.nCopies(RecipePreviewTagsPayload.PREVIEW_SLOT_COUNT, Optional.empty()));
    }

    private static Optional<net.minecraft.resources.Identifier> recipeIngredientTag(SlotDisplay ingredient) {
        if (ingredient instanceof SlotDisplay.TagSlotDisplay tag) {
            return Optional.of(tag.tag().location());
        }
        if (ingredient instanceof SlotDisplay.Composite composite) {
            return composite.contents().stream().map(RecipeEditorPayloads::recipeIngredientTag).flatMap(Optional::stream).findFirst();
        }
        return Optional.empty();
    }

    private static void sendPreviewTags(ServerPlayer player, RecipeEditorMenu menu, List<Optional<net.minecraft.resources.Identifier>> tags) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new RecipePreviewTagsPayload(menu.containerId, tags));
    }

    private static void sendSelection(
        ServerPlayer player,
        RecipeKey key,
        net.minecraft.resources.Identifier outputItemId,
        RecipeEditorSelectionPayload.Action action
    ) {
        if (player.containerMenu instanceof RecipeEditorMenu menu) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                new RecipeEditorSelectionPayload(menu.containerId, key.identifier(), key.recipeTypeId(), outputItemId, action));
        }
    }

    private static Optional<net.minecraft.resources.Identifier> previewOutputId(ServerPlayer player) {
        if (player.containerMenu instanceof RecipeEditorMenu menu) {
            ItemStack output = menu.getSlot(0).getItem();
            if (!output.isEmpty()) {
                return Optional.ofNullable(BuiltInRegistries.ITEM.getKey(output.getItem()));
            }
        }
        return Optional.empty();
    }

    private static void clearRecipePreview(ServerPlayer player) {
        if (player.containerMenu instanceof RecipeEditorMenu menu) {
            menu.setCraftingPreview(List.of(), 0);
            menu.setRecipeNavigation(0, 0);
            menu.broadcastChanges();
        }
    }

    /** Rebuilds the shown recipe from the newly reloaded server recipe list. */
    private static void refreshRecipePreview(ServerPlayer player) {
        if (player.containerMenu instanceof RecipeEditorMenu menu) {
            ItemStack output = menu.getSlot(0).getItem();
            if (!output.isEmpty()) {
                populateRecipePreview(player, menu, output.getItem());
                menu.broadcastChanges();
            }
        }
    }

    /** Shows the next surviving recipe without replacing the button's restore state. */
    private static void refreshRecipePreviewKeepingSelection(ServerPlayer player) {
        if (player.containerMenu instanceof RecipeEditorMenu menu) {
            ItemStack output = menu.getSlot(0).getItem();
            if (!output.isEmpty()) {
                populateRecipePreview(player, menu, output.getItem(), false);
                menu.broadcastChanges();
            }
        }
    }

    /** Rebuilds the grid with one exact restored recipe selected. */
    private static void refreshRecipePreview(ServerPlayer player, RecipeKey restoredKey) {
        if (!(player.containerMenu instanceof RecipeEditorMenu menu)) {
            return;
        }
        ItemStack output = menu.getSlot(0).getItem();
        if (output.isEmpty()) {
            return;
        }
        List<RecipeHolder<?>> recipes = player.level().recipeAccess().getRecipes().stream()
            .filter(recipeHolder -> isRecipeOfType(recipeHolder, menu.recipeType()))
            .filter(recipeHolder -> recipeOutputIs(recipeHolder.value(), output.getItem()))
            .sorted(Comparator.comparing(recipeHolder -> recipeHolder.id().toString()))
            .toList();
        int restoredPosition = java.util.stream.IntStream.range(0, recipes.size())
            .filter(index -> RecipeKey.from(recipes.get(index)).equals(restoredKey))
            .findFirst()
            .orElse(0);
        menu.setRecipeNavigation(restoredPosition, recipes.size());
        populateRecipePreview(player, menu, output.getItem());
        menu.broadcastChanges();
    }

    /** Finds a persisted deleted default recipe when no live recipe currently produces this output. */
    private static Optional<RecipeKey> removedDefaultForOutput(ServerPlayer player, RecipeEditorMenu menu, Item output) {
        net.minecraft.resources.Identifier outputItemId = BuiltInRegistries.ITEM.getKey(output);
        return acidglow.ingamerecipeeditor.data.RecipeEditorSavedData.get(player.level()).createRecipeOverlay()
            .tombstones().stream()
            .map(RecipeState.RemovedDefaultRecipe::defaultSnapshot)
            .filter(snapshot -> snapshot.outputItemId().equals(outputItemId))
            .filter(snapshot -> isRecipeTypeIdForEditor(snapshot.key().recipeTypeId(), menu.recipeType()))
            .sorted(Comparator.comparing(snapshot -> snapshot.key().identifier().toString()))
            .map(RecipeSnapshot::key)
            .findFirst();
    }

    /** Updates the client-only selector with every deleted default matching this output and editor type. */
    private static void sendRemovedRecipeList(ServerPlayer player, RecipeEditorMenu menu, Item output) {
        net.minecraft.resources.Identifier outputItemId = BuiltInRegistries.ITEM.getKey(output);
        List<net.minecraft.resources.Identifier> removedRecipeIds = acidglow.ingamerecipeeditor.data.RecipeEditorSavedData.get(player.level())
            .createRecipeOverlay().tombstones().stream()
            .map(RecipeState.RemovedDefaultRecipe::defaultSnapshot)
            .filter(snapshot -> snapshot.outputItemId().equals(outputItemId))
            .filter(snapshot -> isRecipeTypeIdForEditor(snapshot.key().recipeTypeId(), menu.recipeType()))
            .map(snapshot -> snapshot.key().identifier())
            .sorted()
            .toList();
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
            new RemovedRecipeListPayload(menu.containerId, outputItemId, editorRecipeTypeId(menu.recipeType()), removedRecipeIds));
    }

    private static void handleRemovedRecipeSelection(ServerPlayer player, SelectRemovedRecipePayload payload) {
        if (!mayUseOpenEditor(player)) {
            return;
        }
        RecipeEditorMenu menu = (RecipeEditorMenu)player.containerMenu;
        ItemStack selectedOutput = menu.getSlot(0).getItem();
        if (selectedOutput.isEmpty()
            || !BuiltInRegistries.ITEM.getKey(selectedOutput.getItem()).equals(payload.outputItemId())
            || !editorRecipeTypeId(menu.recipeType()).equals(payload.recipeTypeId())) {
            rejectMutation(player, "That removed recipe is not for the current item and recipe type.");
            return;
        }
        RecipeKey key = new RecipeKey(ResourceKey.create(Registries.RECIPE, payload.recipeId()), payload.recipeTypeId());
        Optional<RecipeSnapshot> removedSnapshot = acidglow.ingamerecipeeditor.data.RecipeEditorSavedData.get(player.level())
            .createRecipeOverlay().tombstones().stream()
            .map(RecipeState.RemovedDefaultRecipe::defaultSnapshot)
            .filter(snapshot -> snapshot.key().equals(key))
            .filter(snapshot -> snapshot.outputItemId().equals(payload.outputItemId()))
            .findFirst();
        if (removedSnapshot.isEmpty()) {
            rejectMutation(player, "That default recipe is no longer deleted.");
            return;
        }
        Optional<Recipe<?>> decodedRecipe = RecipeJsonCodec.decode(removedSnapshot.get(), player.level().registryAccess())
            .resultOrPartial(error -> AcidglowsIngameRecipeEditor.LOGGER.warn("Could not preview removed recipe {}: {}", payload.recipeId(), error));
        if (decodedRecipe.isEmpty()) {
            rejectMutation(player, "That saved default recipe can no longer be read.");
            return;
        }
        populateRecipePreview(player, menu, new RecipeHolder<>(key.recipeId(), decodedRecipe.get()), 0, 0, false);
        sendSelection(player, key, payload.outputItemId(), RecipeEditorSelectionPayload.Action.RESTORE_DEFAULT);
        menu.broadcastChanges();
    }

    private static net.minecraft.resources.Identifier editorRecipeTypeId(int editorType) {
        return switch (editorType) {
            case RecipeEditorMenu.TYPE_CRAFTING -> net.minecraft.resources.Identifier.withDefaultNamespace("crafting");
            case RecipeEditorMenu.TYPE_FURNACE -> net.minecraft.resources.Identifier.withDefaultNamespace("smelting");
            case RecipeEditorMenu.TYPE_BLAST_FURNACE -> net.minecraft.resources.Identifier.withDefaultNamespace("blasting");
            case RecipeEditorMenu.TYPE_CAMPFIRE -> net.minecraft.resources.Identifier.withDefaultNamespace("campfire_cooking");
            default -> throw new IllegalArgumentException("Unknown recipe editor type: " + editorType);
        };
    }

    private static boolean isRecipeTypeIdForEditor(net.minecraft.resources.Identifier recipeTypeId, int editorType) {
        try {
            return recipeTypeId.equals(editorRecipeTypeId(editorType));
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static boolean isActiveRecipePreview(ServerPlayer player, RecipeKey key) {
        if (!(player.containerMenu instanceof RecipeEditorMenu menu)) {
            return false;
        }
        ItemStack output = menu.getSlot(0).getItem();
        if (output.isEmpty()) {
            return false;
        }
        List<RecipeHolder<?>> recipes = player.level().recipeAccess().getRecipes().stream()
            .filter(recipeHolder -> isRecipeOfType(recipeHolder, menu.recipeType()))
            .filter(recipeHolder -> recipeOutputIs(recipeHolder.value(), output.getItem()))
            .sorted(Comparator.comparing(recipeHolder -> recipeHolder.id().toString()))
            .toList();
        if (recipes.isEmpty()) {
            return false;
        }
        RecipeHolder<?> active = recipes.get(Math.clamp(menu.recipePosition(), 0, recipes.size() - 1));
        return RecipeKey.from(active).equals(key);
    }

    private static boolean isRecipeOfType(RecipeHolder<?> holder, int editorType) {
        net.minecraft.resources.Identifier typeId = RecipeKey.from(holder).recipeTypeId();
        return switch (editorType) {
            case RecipeEditorMenu.TYPE_CRAFTING -> typeId.equals(net.minecraft.resources.Identifier.withDefaultNamespace("crafting"));
            case RecipeEditorMenu.TYPE_FURNACE -> typeId.equals(net.minecraft.resources.Identifier.withDefaultNamespace("smelting"));
            case RecipeEditorMenu.TYPE_BLAST_FURNACE -> typeId.equals(net.minecraft.resources.Identifier.withDefaultNamespace("blasting"));
            case RecipeEditorMenu.TYPE_CAMPFIRE -> typeId.equals(net.minecraft.resources.Identifier.withDefaultNamespace("campfire_cooking"));
            default -> false;
        };
    }

    private static boolean recipeOutputIs(Recipe<?> recipe, Item output) {
        ItemStack result = recipeOutput(recipe);
        return !result.isEmpty() && result.getItem() == output;
    }

    private static ItemStack recipeOutput(Recipe<?> recipe) {
        if (recipe instanceof ShapedRecipe shaped) {
            return shaped.assemble(net.minecraft.world.item.crafting.CraftingInput.EMPTY);
        } else if (recipe instanceof ShapelessRecipe shapeless) {
            return shapeless.assemble(net.minecraft.world.item.crafting.CraftingInput.EMPTY);
        } else if (recipe instanceof AbstractCookingRecipe cooking) {
            return cooking.assemble(new net.minecraft.world.item.crafting.SingleRecipeInput(ItemStack.EMPTY));
        } else {
            return ItemStack.EMPTY;
        }
    }

    private static void handleRecipeTypeChange(ServerPlayer player) {
        if (!mayUseOpenEditor(player)) {
            return;
        }
        RecipeEditorMenu menu = (RecipeEditorMenu)player.containerMenu;
        int nextType = menu.recipeType() == RecipeEditorMenu.TYPE_CAMPFIRE
            ? RecipeEditorMenu.TYPE_CRAFTING
            : menu.recipeType() + 1;
        menu.setRecipeType(nextType);
        ItemStack output = menu.getSlot(0).getItem();
        if (!output.isEmpty()) {
            populateRecipePreview(player, menu, output.getItem());
        }
        menu.broadcastChanges();
    }

    private static void handleRecipeNavigation(ServerPlayer player, int direction) {
        if (!mayUseOpenEditor(player) || (direction != -1 && direction != 1)) {
            return;
        }
        RecipeEditorMenu menu = (RecipeEditorMenu)player.containerMenu;
        if (menu.recipeCount() < 2) {
            return;
        }
        menu.setRecipeNavigation(Math.floorMod(menu.recipePosition() + direction, menu.recipeCount()), menu.recipeCount());
        ItemStack output = menu.getSlot(0).getItem();
        if (!output.isEmpty()) {
            populateRecipePreview(player, menu, output.getItem());
            menu.broadcastChanges();
        }
    }

    private static boolean mayUseOpenEditor(ServerPlayer player) {
        if (player == null || !EditorPermissions.mayUseEditor(player)) {
            return false;
        }
        return player.containerMenu instanceof RecipeEditorMenu;
    }

    private static void handleRecipePreviewTagSelection(ServerPlayer player, SelectRecipePreviewTagPayload payload) {
        if (!mayUseOpenEditor(player) || payload.slotIndex() < 1 || payload.slotIndex() >= 10) {
            return;
        }
        RecipeEditorMenu menu = (RecipeEditorMenu)player.containerMenu;
        TagKey<Item> tag = TagKey.create(Registries.ITEM, payload.tagId());
        ItemStack current = menu.getSlot(payload.slotIndex()).getItem();
        if (current.isEmpty() || !current.is(tag)) {
            return;
        }
        List<ItemStack> options = new java.util.ArrayList<>();
        for (net.minecraft.core.Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
            options.add(new ItemStack(holder.value()));
        }
        if (menu.selectPreviewTag(payload.slotIndex(), options)) {
            menu.broadcastChanges();
        }
    }

    private static void handleCarriedItemDiscard(ServerPlayer player) {
        if (player == null) {
            return;
        }
        if (!EditorPermissions.mayUseEditor(player)) {
            rejectMutation(player, "You do not have permission to use the recipe editor.");
            return;
        }
        if (player.containerMenu instanceof acidglow.ingamerecipeeditor.menu.RecipeEditorMenu) {
            player.containerMenu.setCarried(net.minecraft.world.item.ItemStack.EMPTY);
            player.containerMenu.broadcastChanges();
        }
    }

    private static void handleInputItemCountAdjustment(ServerPlayer player, AdjustEditorItemCountPayload payload) {
        if (player == null || payload.delta() != 1 && payload.delta() != -1) {
            return;
        }
        if (!EditorPermissions.mayUseEditor(player)) {
            rejectMutation(player, "You do not have permission to use the recipe editor.");
            return;
        }
        if (!(player.containerMenu instanceof acidglow.ingamerecipeeditor.menu.RecipeEditorMenu menu)) {
            rejectMutation(player, "The recipe editor is no longer open.");
            return;
        }
        if (menu.adjustInputItemCount(payload.delta())) {
            menu.broadcastChanges();
        }
    }

    private static void handleEditorItemClear(ServerPlayer player, ClearEditorItemPayload payload) {
        if (player == null) {
            return;
        }
        if (!EditorPermissions.mayUseEditor(player)) {
            rejectMutation(player, "You do not have permission to use the recipe editor.");
            return;
        }
        if (payload.slotIndex() < 0 || payload.slotIndex() >= 10) {
            rejectMutation(player, "That recipe-editor slot is invalid.");
            return;
        }
        if (!(player.containerMenu instanceof acidglow.ingamerecipeeditor.menu.RecipeEditorMenu menu)) {
            rejectMutation(player, "The recipe editor is no longer open.");
            return;
        }
        if (menu.isCookingRecipeType() && payload.slotIndex() >= 1 && payload.slotIndex() < 10 && payload.slotIndex() != 5) {
            return;
        }
        if (menu.clearEditorItem(payload.slotIndex())) {
            if (payload.slotIndex() == 0) {
                sendPreviewTags(player, menu, emptyPreviewTags());
            }
            menu.broadcastChanges();
        }
    }

    private static Recipe<?> createCookingRecipe(SaveCookingRecipePayload payload, Item input, Item output) {
        Recipe.CommonInfo commonInfo = new Recipe.CommonInfo(true);
        AbstractCookingRecipe.CookingBookInfo bookInfo = new AbstractCookingRecipe.CookingBookInfo(CookingBookCategory.MISC, "");
        Ingredient ingredient = Ingredient.of(input);
        ItemStackTemplate result = new ItemStackTemplate(output, payload.outputCount());
        return switch (payload.recipeTypeId().getPath()) {
            case "smelting" -> new SmeltingRecipe(commonInfo, bookInfo, ingredient, result, payload.experience(), payload.cookingTime());
            case "blasting" -> new BlastingRecipe(commonInfo, bookInfo, ingredient, result, payload.experience(), payload.cookingTime());
            case "smoking" -> new SmokingRecipe(commonInfo, bookInfo, ingredient, result, payload.experience(), payload.cookingTime());
            case "campfire_cooking" -> new CampfireCookingRecipe(commonInfo, bookInfo, ingredient, result, payload.experience(), payload.cookingTime());
            default -> throw new IllegalArgumentException("Unsupported cooking recipe type");
        };
    }

    private static Recipe<?> createCraftingRecipe(List<ItemStack> ingredients, ItemStack output, int recipeKind) {
        Recipe.CommonInfo commonInfo = new Recipe.CommonInfo(true);
        CraftingRecipe.CraftingBookInfo bookInfo = new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.MISC, "");
        ItemStackTemplate result = new ItemStackTemplate(output.getItem(), output.getCount());
        if (recipeKind == 2) {
            List<Ingredient> shapelessIngredients = ingredients.stream()
                .filter(stack -> !stack.isEmpty())
                .map(stack -> Ingredient.of(stack.getItem()))
                .toList();
            return new ShapelessRecipe(commonInfo, bookInfo, result, shapelessIngredients);
        }

        int minColumn = 3;
        int maxColumn = -1;
        int minRow = 3;
        int maxRow = -1;
        for (int index = 0; index < ingredients.size(); index++) {
            if (!ingredients.get(index).isEmpty()) {
                int row = index / 3;
                int column = index % 3;
                minColumn = Math.min(minColumn, column);
                maxColumn = Math.max(maxColumn, column);
                minRow = Math.min(minRow, row);
                maxRow = Math.max(maxRow, row);
            }
        }
        if (maxColumn < 0) {
            throw new IllegalArgumentException("A crafting recipe needs at least one ingredient.");
        }
        java.util.Map<Item, Character> characters = new java.util.LinkedHashMap<>();
        java.util.Map<Character, Ingredient> key = new java.util.LinkedHashMap<>();
        List<String> pattern = new java.util.ArrayList<>();
        for (int row = minRow; row <= maxRow; row++) {
            StringBuilder line = new StringBuilder();
            for (int column = minColumn; column <= maxColumn; column++) {
                ItemStack stack = ingredients.get(row * 3 + column);
                if (stack.isEmpty()) {
                    line.append(' ');
                } else {
                    char character = characters.computeIfAbsent(stack.getItem(), item -> (char)('A' + characters.size()));
                    key.putIfAbsent(character, Ingredient.of(stack.getItem()));
                    line.append(character);
                }
            }
            pattern.add(line.toString());
        }
        return new ShapedRecipe(commonInfo, bookInfo, ShapedRecipePattern.of(key, pattern), result);
    }

    private static boolean isSupportedCookingType(net.minecraft.resources.Identifier typeId) {
        return typeId.getNamespace().equals("minecraft") && switch (typeId.getPath()) {
            case "smelting", "blasting", "smoking", "campfire_cooking" -> true;
            default -> false;
        };
    }

    private static boolean isUsableItem(net.minecraft.resources.Identifier itemId) {
        Item item = BuiltInRegistries.ITEM.getValue(itemId);
        return item != null && item != net.minecraft.world.item.Items.AIR;
    }
}
