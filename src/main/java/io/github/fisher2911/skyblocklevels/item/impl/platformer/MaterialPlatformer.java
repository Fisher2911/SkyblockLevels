package io.github.fisher2911.skyblocklevels.item.impl.platformer;

import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.item.ItemSerializer;
import io.github.fisher2911.skyblocklevels.item.ItemSupplier;
import io.github.fisher2911.skyblocklevels.user.User;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerInteractEvent;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.function.Supplier;

public class MaterialPlatformer extends Platformer {

    private final int radius;
    private final Material type;
    private int stored;

    public MaterialPlatformer(SkyblockLevels plugin, long id, String itemId, ItemSupplier itemSupplier, int radius, Material type, int stored) {
        super(plugin, id, itemId, itemSupplier);
        this.radius = radius;
        this.type = type;
        this.stored = stored;
    }

    @Override
    public void onUse(User user, PlayerInteractEvent event) {
        event.setCancelled(true);
        final int placed = this.place(event, this.type, this.radius, Math.min(this.stored, (int) Math.pow(this.radius * 2 + 1, 2)));
        if (placed <= 0) return;
        user.sendMessage("Placed " + placed + " blocks");
        this.stored -= placed;
        this.stored = Math.max(0, this.stored);
        if (this.stored == 0) {
            event.getItem().setAmount(event.getItem().getAmount() - 1);
            this.plugin.getItemManager().delete(this);
            return;
        }
        event.getPlayer().getInventory().setItemInMainHand(this.plugin.getItemManager().getItem(this));
    }

    @Override
    protected String getUsesLeft() {
        return String.valueOf(this.stored);
    }

    public static Serializer serializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public boolean uniqueInInventory() {
        return true;
    }

    public static final class Serializer implements TypeSerializer<Supplier<MaterialPlatformer>> {

        private static final MaterialPlatformer.Serializer INSTANCE = new MaterialPlatformer.Serializer();

        private Serializer() {
        }

        private static final String ITEM_ID = "item-id";
        private static final String ITEM = "item";
        private static final String RADIUS = "radius";
        private static final String TYPE = "type";
        private static final String STORED = "stored";

        @Override
        public Supplier<MaterialPlatformer> deserialize(Type type, ConfigurationNode node) {
            try {
                final String itemId = node.node(ITEM_ID).getString();
                final ItemSupplier itemSupplier = ItemSerializer.deserialize(node.node(ITEM));
                final int radius = node.node(RADIUS).getInt();
                final Material material = Material.valueOf(node.node(TYPE).getString());
                final int stored = node.node(STORED).getInt();
                final SkyblockLevels plugin = SkyblockLevels.getPlugin(SkyblockLevels.class);
                return () -> new MaterialPlatformer(plugin, plugin.getItemManager().generateNextId(), itemId, itemSupplier, radius, material, stored);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void serialize(Type type, @Nullable Supplier<MaterialPlatformer> obj, ConfigurationNode node) throws SerializationException {

        }
    }
}
