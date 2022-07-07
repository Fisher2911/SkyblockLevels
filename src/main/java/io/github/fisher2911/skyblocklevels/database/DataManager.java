package io.github.fisher2911.skyblocklevels.database;

import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.entity.SkyEntity;
import io.github.fisher2911.skyblocklevels.item.SpecialSkyItem;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.logging.Level;

public class DataManager {

    private final AtomicLong IDS = new AtomicLong();

    private final Path FILE_PATH = SkyblockLevels.getPlugin(SkyblockLevels.class).getDataFolder().toPath().resolve("data.db");
    private final SkyblockLevels plugin;
    private final List<CreateTableStatement> createTableStatements = new ArrayList<>();

    private final Map<Class<?>, BiConsumer<Connection, Collection<? extends SpecialSkyItem>>> itemSaveConsumer = new HashMap<>();
    private final Map<String, BiFunction<Connection, Long, ? extends SpecialSkyItem>> itemLoadFunctions = new HashMap<>();
    private final Map<Class<?>, BiConsumer<Connection, SpecialSkyItem>> itemDeleteConsumer = new HashMap<>();

    private final Map<Class<?>, BiConsumer<Connection, Collection<? extends SkyEntity>>> entitySaveConsumer = new HashMap<>();
    private final Map<String, BiFunction<Connection, UUID, ? extends SkyEntity>> entityLoadFunctions = new HashMap<>();
    private final Map<Class<?>, BiConsumer<Connection, SkyEntity>> entityDeleteConsumer = new HashMap<>();


    private Connection connection;

    public DataManager(SkyblockLevels plugin) {
        this.plugin = plugin;
    }

    public Connection getConnection() {
        final File file = FILE_PATH.toFile();
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                this.plugin.getLogger().log(Level.SEVERE, "Failed to create database file", e);
            }
        }
        try {
            if (this.connection != null) return this.connection;
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + FILE_PATH.toString());
            return this.connection;
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void addTable(CreateTableStatement statement) {
        createTableStatements.add(statement);
    }

    public void registerItemSaveConsumer(Class<?> clazz, BiConsumer<Connection, Collection<? extends SpecialSkyItem>> consumer) {
        itemSaveConsumer.put(clazz, consumer);
    }

    public void registerItemLoadFunction(String tableName, BiFunction<Connection, Long, ? extends SpecialSkyItem> function) {
        itemLoadFunctions.put(tableName, function);
    }

    public void registerItemDeleteConsumer(Class<?> clazz, BiConsumer<Connection, SpecialSkyItem> consumer) {
        itemDeleteConsumer.put(clazz, consumer);
    }

    public void registerEntitySaveConsumer(Class<?> clazz, BiConsumer<Connection, Collection<? extends SkyEntity>> consumer) {
        entitySaveConsumer.put(clazz, consumer);
    }

    public void registerEntityLoadFunction(String tableName, BiFunction<Connection, UUID, ? extends SkyEntity> function) {
        entityLoadFunctions.put(tableName, function);
    }

    public void saveItems(Collection<? extends SpecialSkyItem> items, Class<?> clazz) {
        final BiConsumer<Connection, Collection<? extends SpecialSkyItem>> consumer = itemSaveConsumer.get(clazz);
        if (consumer != null) consumer.accept(this.getConnection(), items);
    }

    public SpecialSkyItem loadItem(String tableName, String itemId, long id) {
        final BiFunction<Connection, Long, ? extends SpecialSkyItem> function = itemLoadFunctions.get(tableName);
        final SpecialSkyItem item = this.plugin.getItemManager().getItem(id);
        if (item != SpecialSkyItem.EMPTY) return item;
        if (function == null) return this.plugin.getItemManager().createItem(itemId);
        return function.apply(this.getConnection(), id);
    }

    public void deleteItem(SpecialSkyItem item, Class<?> clazz) {
        final BiConsumer<Connection, SpecialSkyItem> consumer = itemDeleteConsumer.get(clazz);
        if (consumer != null) consumer.accept(this.getConnection(), item);
    }

    public void saveEntities(Collection<? extends SkyEntity> entities, Class<?> clazz) {
        final BiConsumer<Connection, Collection<? extends SkyEntity>> consumer = entitySaveConsumer.get(clazz);
        if (consumer != null) consumer.accept(this.getConnection(), entities);
    }

    public SkyEntity loadEntity(String tableName, UUID id) {
        final BiFunction<Connection, UUID, ? extends SkyEntity> function = entityLoadFunctions.get(tableName);
        if (function == null) return null;
        return function.apply(this.getConnection(), id);
    }

    public void deleteEntity(SkyEntity entity, Class<?> clazz) {
        final BiConsumer<Connection, SkyEntity> consumer = entityDeleteConsumer.get(clazz);
        if (consumer != null) consumer.accept(this.getConnection(), entity);
    }

    private static final String ID_TABLE = "id_table";
    private static final String ID_COLUMN = "id";
    private static final int ROW_ID = 0;
    private static final String LATEST_ID = "latest_id";

    private static final CreateTableStatement ID_TABLE_STATEMENT = CreateTableStatement.builder(ID_TABLE).
            addField(Integer.class, ID_COLUMN, KeyType.PRIMARY).
            addField(Long.class, LATEST_ID).
            build();

    private static final SelectStatement SELECT_LATEST_ID_STATEMENT = SelectStatement.builder(ID_TABLE).
            selectAll().
            build();

    public void createTables() {
        addTable(ID_TABLE_STATEMENT);
        for (CreateTableStatement statement : createTableStatements) {
            statement.execute(this.getConnection());
        }
        SELECT_LATEST_ID_STATEMENT.execute(this.getConnection(), result -> {
            if (result.next()) {
                IDS.set(result.getLong(LATEST_ID));
            }
            return null;
        });
    }

    private static void saveId(Connection connection, long id) {
        InsertStatement.builder(ID_TABLE).
                addEntry(ID_COLUMN, ROW_ID).
                addEntry(LATEST_ID, id).
                build().execute(connection);
    }

    public long generateNextId() {
        final long id = this.IDS.incrementAndGet();
        this.plugin.getLogger().info("Generating next ID: " + id);
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> saveId(this.getConnection(), id));
        return id;
    }
}
