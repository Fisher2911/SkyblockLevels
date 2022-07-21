package io.github.fisher2911.skyblocklevels.item.impl.bridger;

import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.database.statement.CreateTableStatement;
import io.github.fisher2911.skyblocklevels.database.DataManager;
import io.github.fisher2911.skyblocklevels.database.statement.DeleteStatement;
import io.github.fisher2911.skyblocklevels.database.statement.InsertStatement;
import io.github.fisher2911.skyblocklevels.database.statement.KeyType;
import io.github.fisher2911.skyblocklevels.database.statement.SelectStatement;
import io.github.fisher2911.skyblocklevels.database.statement.VarChar;
import io.github.fisher2911.skyblocklevels.item.ItemSerializer;
import io.github.fisher2911.skyblocklevels.item.ItemSupplier;
import io.github.fisher2911.skyblocklevels.item.SpecialSkyItem;
import io.github.fisher2911.skyblocklevels.user.User;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerInteractEvent;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.List;
import java.util.function.Supplier;

public class LimitedBridger extends Bridger {

    private static final String TABLE = LimitedBridger.class.getSimpleName().toLowerCase();
    private static final String ID = "id";
    private static final String ITEM_ID = "item_id";
    private static final String STORED_BLOCKS = "stored_blocks";

    static {
        final SkyblockLevels plugin = SkyblockLevels.getPlugin(SkyblockLevels.class);
        final DataManager dataManager = plugin.getDataManager();

        dataManager.addTable(CreateTableStatement.builder(TABLE).
                addField(Long.class, ID, KeyType.PRIMARY).
                addField(VarChar.ITEM_ID, ITEM_ID).
                addField(Integer.class, STORED_BLOCKS).
                build());

        dataManager.registerItemSaveConsumer(LimitedBridger.class, (conn, collection) -> {
            collection.forEach(item -> {
                InsertStatement.builder(TABLE).
                        newEntry().
                        addEntry(ID, item.getId()).
                        addEntry(ITEM_ID, item.getItemId()).
                        addEntry(STORED_BLOCKS, ((LimitedBridger) item).storedBlocks).
                        build().
                        execute(conn);
            });
        });

        dataManager.registerItemLoadFunction(TABLE, (conn, id) -> {
            final SelectStatement.Builder builder = SelectStatement.builder(TABLE).
                    selectAll().
                    whereEqual(ID, String.valueOf(id));
            final List<LimitedBridger> list = builder.build().execute(conn, results -> {
                final String itemId = results.getString(ITEM_ID);
                if (!(plugin.getItemManager().getItem(itemId) instanceof final LimitedBridger item)) return null;
                final int storedBlocks = results.getInt(STORED_BLOCKS);
                return new LimitedBridger(plugin, id, itemId, item.itemSupplier, item.type, item.size, storedBlocks);
            });
            if (list.isEmpty()) return SpecialSkyItem.EMPTY;
            return list.get(0);
        });

        dataManager.registerItemDeleteConsumer(LimitedBridger.class, (conn, item) -> {
            DeleteStatement.builder(TABLE).
                    condition(ID, String.valueOf(item.getId())).
                    build().
                    execute(conn);
        });
    }

    private final Material type;
    private final int size;
    private int storedBlocks;

    public LimitedBridger(SkyblockLevels plugin, long id, String itemId, ItemSupplier itemSupplier, Material type, int size, int storedBlocks) {
        super(plugin, id, itemId, itemSupplier);
        this.type = type;
        this.size = size;
        this.storedBlocks = storedBlocks;
    }

    @Override
    public void onUse(User user, PlayerInteractEvent event) {
        event.setCancelled(true);
        final int placed = this.place(event, this.type, Math.min(this.storedBlocks, this.size));
        if (placed <= 0) return;
        user.sendMessage("<blue>Placed " + placed + " blocks");
        this.storedBlocks -= placed;
        this.storedBlocks = Math.max(0, this.storedBlocks);
        if (this.storedBlocks <= 0) {
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
    protected String getUsesLeft() {
        return String.valueOf(this.storedBlocks);
    }

    @Override
    public boolean uniqueInInventory() {
        return true;
    }

    public static Serializer serializer() {
        return Serializer.INSTANCE;
    }

    public static final class Serializer implements TypeSerializer<Supplier<LimitedBridger>> {

        private static final Serializer INSTANCE = new Serializer();

        private Serializer() {
        }

        private static final String ITEM_ID = "item-id";
        private static final String ITEM = "item";
        private static final String TYPE = "type";
        private static final String SIZE = "size";
        private static final String STORED_BLOCKS = "stored-blocks";

        @Override
        public Supplier<LimitedBridger> deserialize(Type type, ConfigurationNode node) {
            try {
                final String itemId = node.node(ITEM_ID).getString();
                final ItemSupplier itemSupplier = ItemSerializer.deserialize(node.node(ITEM));
                final Material material = Material.valueOf(node.node(TYPE).getString());
                final int size = node.node(SIZE).getInt();
                final int storedBlocks = node.node(STORED_BLOCKS).getInt();
                final SkyblockLevels plugin = SkyblockLevels.getPlugin(SkyblockLevels.class);
                return () -> new LimitedBridger(plugin, plugin.getDataManager().generateNextId(), itemId, itemSupplier, material, size, storedBlocks);
            } catch (SerializationException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void serialize(Type type, @Nullable Supplier<LimitedBridger> obj, ConfigurationNode node) throws SerializationException {

        }
    }
}
