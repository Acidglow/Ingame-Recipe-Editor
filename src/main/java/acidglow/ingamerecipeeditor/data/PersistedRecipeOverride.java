package acidglow.ingamerecipeeditor.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Persists both the original snapshot and its same-key replacement for restore. */
public record PersistedRecipeOverride(PersistedRecipeSnapshot defaultSnapshot, PersistedRecipeSnapshot replacement) {
    public static final Codec<PersistedRecipeOverride> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        PersistedRecipeSnapshot.CODEC.fieldOf("default").forGetter(PersistedRecipeOverride::defaultSnapshot),
        PersistedRecipeSnapshot.CODEC.fieldOf("replacement").forGetter(PersistedRecipeOverride::replacement)
    ).apply(instance, PersistedRecipeOverride::new));
}
