package acidglow.ingamerecipeeditor.network;

import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import acidglow.ingamerecipeeditor.ModConstants;

/** Authoritative hidden-item IDs, sent to every connected editor client. */
public record HiddenItemsPayload(Set<Identifier> itemIds) implements CustomPacketPayload {
    public static final Type<HiddenItemsPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "hidden_items")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, HiddenItemsPayload> STREAM_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()), HiddenItemsPayload::itemIdsList,
        HiddenItemsPayload::fromList
    );

    public HiddenItemsPayload {
        itemIds = Set.copyOf(itemIds);
    }

    private java.util.List<Identifier> itemIdsList() {
        return java.util.List.copyOf(itemIds);
    }

    private static HiddenItemsPayload fromList(java.util.List<Identifier> itemIds) {
        return new HiddenItemsPayload(new LinkedHashSet<>(itemIds));
    }

    @Override
    public Type<HiddenItemsPayload> type() {
        return TYPE;
    }
}
