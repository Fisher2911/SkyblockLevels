package io.github.fisher2911.skyblocklevels.user;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.database.CreateTableStatement;
import io.github.fisher2911.skyblocklevels.database.DataManager;
import io.github.fisher2911.skyblocklevels.database.InsertStatement;
import io.github.fisher2911.skyblocklevels.database.KeyType;
import io.github.fisher2911.skyblocklevels.database.SelectStatement;
import io.github.fisher2911.skyblocklevels.item.ItemBuilder;
import io.github.fisher2911.skyblocklevels.item.ItemSerializer;
import io.github.fisher2911.skyblocklevels.item.ItemSupplier;
import io.github.fisher2911.skyblocklevels.message.Adventure;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class UserManager {

    private static final String TABLE = "users";
    private static final String UUID = "uuid";
    private static final String ITEM_ID = "item_id";
    private static final String AMOUNT = "amount";

    static {
        final SkyblockLevels plugin = SkyblockLevels.getPlugin(SkyblockLevels.class);
        final DataManager dataManager = plugin.getDataManager();

        dataManager.addTable(CreateTableStatement.builder(TABLE).
                addField(String.class, UUID, KeyType.PRIMARY).
                addField(String.class, ITEM_ID).
                addField(Integer.class, AMOUNT).
                groupKeys(KeyType.UNIQUE, UUID, ITEM_ID).
                build());
    }

    private static final String FILE_NAME = "user-settings.yml";
    private static final Path FILE_PATH = SkyblockLevels.getPlugin(SkyblockLevels.class).getDataFolder().toPath().resolve(FILE_NAME);

    private final SkyblockLevels plugin;
    private BukkitTask saveTask;
    private final Map<UUID, BukkitUser> users;
    private final Set<String> shouldStore;
    private final Map<String, CollectionCategory> collectionCategories;

    public UserManager(SkyblockLevels plugin, Map<UUID, BukkitUser> users, Set<String> shouldStore) {
        this.plugin = plugin;
        this.users = users;
        this.shouldStore = shouldStore;
        this.collectionCategories = new HashMap<>();
    }

    @Nullable
    public BukkitUser getUser(UUID uuid) {
        return this.users.get(uuid);
    }

    @Nullable
    public BukkitUser getUser(Player player) {
        return this.users.get(player.getUniqueId());
    }

    public void addUser(BukkitUser user) {
        this.users.put(user.getId(), user);
    }

    public BukkitUser removeUser(UUID uuid) {
        return this.users.remove(uuid);
    }

    public void startSaveTask() {
        this.saveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this.plugin, this::saveAll, 20 * 60, 20 * 60);
    }

    public void endSaveTask() {
        if (this.saveTask == null) return;
        this.saveTask.cancel();
    }

    public void showMenu(BukkitUser user) {
        final Player player = user.getPlayer();
        if (player == null) return;
        final Gui gui = Gui.gui().
                disableAllInteractions().
                title(Adventure.parse("<green>Collection requirements")).
                create();
        gui.getFiller().fillBorder(new GuiItem(ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).amount(1).name(" ").build()));
        for (CollectionCategory category : this.collectionCategories.values()) {
            gui.addItem(new GuiItem(category.getMenuItem(), event -> category.showMenu(user)));
        }
        gui.open(player);
    }

    public void saveUser(User user) {
        final Collection collection = user.getCollection();
        final Set<String> changed = collection.getChanged();
        collection.setChanged(new HashSet<>());
        if (changed.isEmpty()) return;
        final InsertStatement.Builder builder = InsertStatement.
                builder(TABLE);
        for (String collectionType : changed) {
            builder.newEntry().
                    addEntry(UUID, user.getId().toString()).
                    addEntry(ITEM_ID, collectionType).
                    addEntry(AMOUNT, collection.getAmount(collectionType)).
                    batchSize(changed.size());
            this.plugin.getLogger().severe("Saving: " + collectionType + " : " + collection.getAmount(collectionType));
        }
        builder.build().execute(this.plugin.getDataManager().getConnection());
    }

    public void saveAll() {
        for (User user : this.users.values()) {
            this.saveUser(user);
        }
    }

    public void loadUser(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            final SelectStatement statement = SelectStatement.
                    builder(TABLE).
                    condition(UUID, uuid.toString()).
                    selectAll().
                    build();
            final List<BukkitUser> users = statement.execute(this.plugin.getDataManager().getConnection(), resultSet -> {
                final Map<String, Integer> collection = new HashMap<>();
                while (resultSet.next()) {
                    final String itemId = resultSet.getString(ITEM_ID);
                    final int amount = resultSet.getInt(AMOUNT);
                    collection.put(itemId, amount);
                }
                return new BukkitUser(uuid, new Collection(collection), new Cooldowns(new HashMap<>()));
            });
            BukkitUser user = users.isEmpty() ? new BukkitUser(uuid, new Collection(new HashMap<>()), new Cooldowns(new HashMap<>())): users.get(0);
            this.addUser(user);
        });
    }

    private static final String SHOULD_STORE_PATH = "should-store";
    private static final String COLLECTION_CATEGORIES_PATH = "collection-categories";
    private static final String ITEM = "item";
    private static final String COLLECTIONS = "collections";

    public void load(SkyblockLevels plugin) {
        try {
            final File file = FILE_PATH.toFile();
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                plugin.saveResource(FILE_NAME, false);
            }
            final YamlConfigurationLoader loader = YamlConfigurationLoader.
                    builder().
                    path(FILE_PATH).
                    build();
            final ConfigurationNode source = loader.load();
            final ConfigurationNode shouldStore = source.node(SHOULD_STORE_PATH);
            final ConfigurationNode collectionCategories = source.node(COLLECTION_CATEGORIES_PATH);
            this.collectionCategories.clear();
            for (var entry : collectionCategories.childrenMap().entrySet()) {
                if (!(entry.getKey() instanceof final String category)) continue;
                final ConfigurationNode node = entry.getValue();
                final ItemSupplier supplier = ItemSerializer.deserialize(node.node(ITEM));
                final List<String> collections = node.node(COLLECTIONS).getList(String.class, new ArrayList<>());
                final CollectionCategory collectionCategory = new CollectionCategory(this.plugin, category, supplier, collections);
                this.collectionCategories.put(category, collectionCategory);
            }
            this.shouldStore.clear();
            this.shouldStore.addAll(shouldStore.getList(String.class, new ArrayList<>()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

