package io.github.fisher2911.skyblocklevels.item.impl.platformer;

import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.database.CreateTableStatement;
import io.github.fisher2911.skyblocklevels.database.DataManager;
import io.github.fisher2911.skyblocklevels.database.DeleteStatement;
import io.github.fisher2911.skyblocklevels.database.InsertStatement;
import io.github.fisher2911.skyblocklevels.database.KeyType;
import io.github.fisher2911.skyblocklevels.database.SelectStatement;
import io.github.fisher2911.skyblocklevels.database.VarChar;
import io.github.fisher2911.skyblocklevels.item.ItemSerializer;
import io.github.fisher2911.skyblocklevels.item.ItemSupplier;
import io.github.fisher2911.skyblocklevels.item.SpecialSkyItem;
import io.github.fisher2911.skyblocklevels.user.User;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.function.Supplier;

public class MaterialPlatformer extends Platformer {

    private static final String TABLE = MaterialPlatformer.class.getSimpleName().toLowerCase();
    private static final String ID = "id";
    private static final String ITEM_ID = "item_id";
    private static final String STORED = "stored";

    static {
        final SkyblockLevels plugin = SkyblockLevels.getPlugin(SkyblockLevels.class);
        final DataManager dataManager = plugin.getDataManager();

        dataManager.addTable(CreateTableStatement.builder(TABLE).
                addField(Long.class, ID, KeyType.PRIMARY).
                addField(VarChar.ITEM_ID, ITEM_ID).
                addField(Integer.class, STORED).
                build());

        dataManager.registerItemSaveConsumer(MaterialPlatformer.class, (conn, collection) -> {
            collection.forEach(item ->
                    InsertStatement.builder(TABLE).
                            newEntry().
                            addEntry(ID, item.getId()).
                            addEntry(ITEM_ID, item.getItemId()).
                            addEntry(STORED, ((MaterialPlatformer) item).stored).
                            build().
                            execute(conn));
        });

        dataManager.registerItemLoadFunction(TABLE, (conn, id) -> {
            final SelectStatement.Builder builder = SelectStatement.builder(TABLE).
                    selectAll().
                    condition(ID, String.valueOf(id));
            final List<MaterialPlatformer> list = builder.build().execute(conn, results -> {
                final String itemId = results.getString(ITEM_ID);
                if (!(plugin.getItemManager().getItem(itemId) instanceof final MaterialPlatformer item)) return null;
                final int stored = results.getInt(STORED);
                final MaterialPlatformer platformer = new MaterialPlatformer(plugin, id, itemId, item.itemSupplier, item.radius, item.type, item.stored);
                platformer.stored = stored;
                return platformer;
            });
            if (list.isEmpty()) return SpecialSkyItem.EMPTY;
            return list.get(0);
        });

        dataManager.registerItemDeleteConsumer(MaterialPlatformer.class, (conn, item) -> {
            DeleteStatement.builder(TABLE).
                    condition(ID, String.valueOf(item.getId())).
                    build().
                    execute(conn);
        });
    }

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
        this.stored -= placed;
        this.stored = Math.max(0, this.stored);
        if (this.stored == 0) {
            event.getItem().setAmount(event.getItem().getAmount() - 1);
            this.plugin.getItemManager().delete(this);
            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
                DeleteStatement.builder(TABLE).
                        condition(ID, String.valueOf(this.id)).
                        build().
                        execute(this.plugin.getDataManager().getConnection());
            });
            return;
        }
        event.getPlayer().getInventory().setItemInMainHand(this.plugin.getItemManager().getItem(this));
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () ->
                this.plugin.getDataManager().saveItems(List.of(this), this.getClass())
        );
    }

    @Override
    public void onItemDamage(User user, PlayerItemDamageEvent event) {
        event.setCancelled(true);
    }

    @Override
    public int getDurability() {
        return 1;
    }

    @Override
    protected String getUsesLeft() {
        return String.valueOf(this.stored);
    }

    @Override
    public boolean uniqueInInventory() {
        return true;
    }

    public static Serializer serializer() {
        return Serializer.INSTANCE;
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
                return () -> new MaterialPlatformer(plugin, plugin.getDataManager().generateNextId(), itemId, itemSupplier, radius, material, stored);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void serialize(Type type, @Nullable Supplier<MaterialPlatformer> obj, ConfigurationNode node) throws SerializationException {

        }
    }
}
