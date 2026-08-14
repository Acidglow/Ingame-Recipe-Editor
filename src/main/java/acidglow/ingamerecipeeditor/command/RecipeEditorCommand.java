package acidglow.ingamerecipeeditor.command;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import acidglow.ingamerecipeeditor.AcidglowsIngameRecipeEditor;
import acidglow.ingamerecipeeditor.ModConstants;
import acidglow.ingamerecipeeditor.data.RecipeEditorSavedData;
import acidglow.ingamerecipeeditor.menu.RecipeEditorMenu;
import acidglow.ingamerecipeeditor.network.RecipeEditorPayloads;
import acidglow.ingamerecipeeditor.recipe.service.RecipeReloadService;

/** Registers the owner-confirmed editor command. */
@EventBusSubscriber(modid = AcidglowsIngameRecipeEditor.MODID)
public final class RecipeEditorCommand {
    private RecipeEditorCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(ModConstants.EDITOR_COMMAND)
            .executes(RecipeEditorCommand::open)
            .then(Commands.literal("restore_all").executes(RecipeEditorCommand::restoreAll))
        );
    }

    private static int open(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.translatable("command.acidglows_ingame_recipe_editor.player_required"));
            return 0;
        }
        if (!EditorPermissions.mayUseEditor(player)) {
            context.getSource().sendFailure(Component.translatable("command.acidglows_ingame_recipe_editor.no_permission"));
            return 0;
        }

        player.openMenu(new SimpleMenuProvider(
            (containerId, inventory, menuPlayer) -> new RecipeEditorMenu(containerId, inventory),
            Component.translatable("screen.acidglows_ingame_recipe_editor.title")
        ));
        RecipeEditorPayloads.sendHiddenItems(player);
        return 1;
    }

    /** Clears every persisted recipe overlay entry, then reloads the server's original data-pack recipes. */
    private static int restoreAll(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.translatable("command.acidglows_ingame_recipe_editor.player_required"));
            return 0;
        }
        if (!EditorPermissions.mayUseEditor(player)) {
            context.getSource().sendFailure(Component.translatable("command.acidglows_ingame_recipe_editor.no_permission"));
            return 0;
        }

        RecipeEditorSavedData.get(player.level()).restoreAllRecipesToDefault();
        var server = player.level().getServer();
        RecipeReloadService.reload(server).whenComplete((ignored, error) -> server.execute(() -> {
            if (error != null) {
                AcidglowsIngameRecipeEditor.LOGGER.error("Could not restore all recipe-editor changes", error);
                player.sendSystemMessage(Component.translatable("command.acidglows_ingame_recipe_editor.restore_all_failed"));
            } else {
                player.sendSystemMessage(Component.translatable("command.acidglows_ingame_recipe_editor.restore_all_success"));
            }
        }));
        return 1;
    }
}
