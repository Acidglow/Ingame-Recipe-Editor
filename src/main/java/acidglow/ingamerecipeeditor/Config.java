package acidglow.ingamerecipeeditor;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ONLY_ADMIN_OR_CREATIVE = BUILDER
        .comment("When true, only a Creative-mode player or an operator with the configured permission level may use the recipe editor.")
        .define("onlyAdminOrCreative", true);

    public static final ModConfigSpec.IntValue OPERATOR_PERMISSION_LEVEL = BUILDER
        .comment("Required operator permission level when onlyAdminOrCreative is enabled.")
        .defineInRange("operatorPermissionLevel", 2, 0, 4);

    static final ModConfigSpec SPEC = BUILDER.build();
}
