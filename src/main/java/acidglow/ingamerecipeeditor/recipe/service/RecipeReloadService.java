package acidglow.ingamerecipeeditor.recipe.service;

import java.util.concurrent.CompletableFuture;
import net.minecraft.server.MinecraftServer;

/** Requests the public server resource reload required after an editor mutation. */
public final class RecipeReloadService {
    private RecipeReloadService() {
    }

    public static CompletableFuture<Void> reload(MinecraftServer server) {
        return server.reloadResources(server.getPackRepository().getSelectedIds());
    }
}
