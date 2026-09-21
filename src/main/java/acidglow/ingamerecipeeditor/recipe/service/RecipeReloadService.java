package acidglow.ingamerecipeeditor.recipe.service;

import java.util.concurrent.CompletableFuture;
import net.minecraft.server.MinecraftServer;

/** Requests a full data-pack reload only when applying hidden-item recipe filters. */
public final class RecipeReloadService {
    private RecipeReloadService() {
    }

    public static CompletableFuture<Void> reload(MinecraftServer server) {
        return server.reloadResources(server.getPackRepository().getSelectedIds());
    }
}
