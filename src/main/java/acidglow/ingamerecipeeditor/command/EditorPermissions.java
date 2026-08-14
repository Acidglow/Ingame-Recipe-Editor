package acidglow.ingamerecipeeditor.command;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.Permissions;
import acidglow.ingamerecipeeditor.Config;

/** Shared server-side authorization check for opening and mutating the editor. */
public final class EditorPermissions {
    private EditorPermissions() {
    }

    public static boolean mayUseEditor(ServerPlayer player) {
        return !Config.ONLY_ADMIN_OR_CREATIVE.getAsBoolean()
            || player.isCreative()
            || hasConfiguredOperatorPermission(player);
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
