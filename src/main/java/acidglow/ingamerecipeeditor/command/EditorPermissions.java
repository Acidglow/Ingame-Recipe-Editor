package acidglow.ingamerecipeeditor.command;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.Permissions;
import acidglow.ingamerecipeeditor.Config;

/** Shared server-side authorization checks for the recipe editor. */
public final class EditorPermissions {
    private EditorPermissions() {
    }

    public static boolean mayUseEditor(ServerPlayer player) {
        return !Config.ONLY_ADMIN_OR_CREATIVE.getAsBoolean()
            || player.isCreative()
            || hasConfiguredOperatorPermission(player);
    }

    /** Item Book inventory grants always require Creative mode or a level-2 operator. */
    public static boolean mayReceiveItemBookItems(ServerPlayer player) {
        return player.isCreative() || player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    private static boolean hasConfiguredOperatorPermission(ServerPlayer player) {
        Permission required = switch (Config.OPERATOR_PERMISSION_LEVEL.getAsInt()) {
            case 0 -> null;
            case 1 -> Permissions.COMMANDS_MODERATOR;
            case 2 -> Permissions.COMMANDS_GAMEMASTER;
            case 3 -> Permissions.COMMANDS_ADMIN;
            case 4 -> Permissions.COMMANDS_OWNER;
            default -> throw new IllegalStateException("Invalid configured operator permission level");
        };
        return required == null || player.permissions().hasPermission(required);
    }
}
