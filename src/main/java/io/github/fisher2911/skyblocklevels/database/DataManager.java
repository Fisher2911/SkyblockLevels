package io.github.fisher2911.skyblocklevels.database;

import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.entity.SkyEntity;
import io.github.fisher2911.skyblocklevels.item.SpecialSkyItem;

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
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.logging.Level;

public class DataManager {

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

    public void createTables() {
        for (CreateTableStatement statement : createTableStatements) {
            statement.execute(this.getConnection());
        }
    }

}
