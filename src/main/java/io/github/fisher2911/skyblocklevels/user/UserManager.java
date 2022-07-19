package io.github.fisher2911.skyblocklevels.user;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.booster.Boosters;
import io.github.fisher2911.skyblocklevels.database.CreateTableStatement;
import io.github.fisher2911.skyblocklevels.database.DataManager;
import io.github.fisher2911.skyblocklevels.database.InsertStatement;
import io.github.fisher2911.skyblocklevels.database.KeyType;
import io.github.fisher2911.skyblocklevels.database.SelectStatement;
import io.github.fisher2911.skyblocklevels.database.VarChar;
import io.github.fisher2911.skyblocklevels.item.ItemBuilder;
import io.github.fisher2911.skyblocklevels.item.ItemSerializer;
import io.github.fisher2911.skyblocklevels.item.ItemSupplier;
import io.github.fisher2911.skyblocklevels.message.Adventure;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.node.Node;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class UserManager {

    public static final String TABLE = "users";
    public static final String UUID = "uuid";
    public static final String ITEM_ID = "item_id";
    public static final String AMOUNT = "amount";

    static {
        final SkyblockLevels plugin = SkyblockLevels.getPlugin(SkyblockLevels.class);
        final DataManager dataManager = plugin.getDataManager();

        dataManager.addTable(CreateTableStatement.builder(TABLE).
                addField(VarChar.UUID, UUID).
                addField(VarChar.ITEM_ID, ITEM_ID).
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
    private final Map<String, CollectionPermission> collectionPermissions;
    private final List<String> startItems;
    private final CollectionTop collectionTop;

    public UserManager(SkyblockLevels plugin, Map<UUID, BukkitUser> users, Set<String> shouldStore) {
        this.plugin = plugin;
        this.users = users;
        this.shouldStore = shouldStore;
        this.collectionCategories = new HashMap<>();
        this.collectionPermissions = new HashMap<>();
        this.startItems = new ArrayList<>();
        this.collectionTop = new CollectionTop(this.plugin, new ConcurrentHashMap<>());
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

    public List<String> getStartItems() {
        return startItems;
    }

    public int getCollectionRequirement(String id) {
        final CollectionPermission permission = this.collectionPermissions.get(id);
        if (permission == null) return 0;
        return permission.getAmount();
    }

    public void showMenu(BukkitUser user, @Nullable Consumer<ItemBuilder> itemEditor, @Nullable Consumer<CollectionCategory> onClick) {
        final Player player = user.getPlayer();
        if (player == null) return;
        final Gui gui = Gui.gui().
                disableAllInteractions().
                title(Adventure.parse("<green>Collection requirements")).
                create();
        gui.getFiller().fillBorder(new GuiItem(ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).amount(1).name(" ").build()));
        for (CollectionCategory category : this.collectionCategories.values()) {
            final ItemBuilder itemBuilder = ItemBuilder.from(category.getMenuItem());
            if (itemEditor != null) itemEditor.accept(itemBuilder);
            gui.addItem(new GuiItem(itemBuilder.build(), event -> {
                if (onClick == null) return;
                onClick.accept(category);
            }));
        }
        gui.open(player);
    }

    public void showMenu(BukkitUser user) {
        this.showMenu(user, null, category -> category.showMenu(user));
    }

    public CollectionTop getCollectionTop() {
        return collectionTop;
    }

    public void saveUser(User user) {
        final Collection collection = user.getCollection();
        this.collectionTop.update(user.getId(), collection);
        final Set<String> changed = collection.getChanged();
        collection.setChanged(new HashSet<>());
        if (user instanceof BukkitUser bukkitUser) {
            bukkitUser.getBoosters().save(this.plugin.getDataManager().getConnection());
        }
        if (changed.isEmpty()) return;
        for (String collectionType : changed) {
            InsertStatement.builder(TABLE).newEntry().
                    addEntry(UUID, user.getId().toString()).
                    addEntry(ITEM_ID, collectionType).
                    addEntry(AMOUNT, collection.getAmount(collectionType)).
                    build().execute(this.plugin.getDataManager().getConnection());
        }
    }

    public void saveAll() {
        for (User user : this.users.values()) {
            this.saveUser(user);
        }
    }

    public void addCollectionAmount(User user, String id, int amount, boolean requiresShouldStore) {
        if (requiresShouldStore && !this.shouldStore.contains(id)) return;
        this.addCollectionAmount(user, id, amount);
    }

    public void addCollectionAmount(User user, String id, int amount) {
        user.getCollection().addAmount(id, amount);
        final int currentAmount = user.getCollection().getAmount(id);
        final CollectionPermission collectionPermission = this.collectionPermissions.get(id);
        if (collectionPermission == null) return;
        if (collectionPermission.getAmount() > currentAmount) return;
        final var luckPerms = this.plugin.getLuckPermsProvider();
        if (luckPerms == null) return;
        final LuckPerms api = luckPerms.getProvider();
        api.getUserManager().modifyUser(user.getId(), luckPermsUser -> {
            final String permission = collectionPermission.getPermission();
            if (luckPermsUser.getCachedData().getPermissionData().checkPermission(permission).asBoolean()) return;
            luckPermsUser.data().add(Node.builder(permission).build());
        });
    }

    public boolean hasPermissionForCollection(User user, String id) {
        final CollectionPermission collectionPermission = this.collectionPermissions.get(id);
        if (collectionPermission == null) return true;
        return user.getCollection().getAmount(id) >= collectionPermission.getAmount();
    }

    public void loadUser(UUID uuid, Consumer<User> onLoad) {
        final Boosters boosters = Boosters.load(this.plugin.getDataManager().getConnection(), uuid);
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            final SelectStatement statement = SelectStatement.
                    builder(TABLE).
                    whereEqual(UUID, uuid.toString()).
                    selectAll().
                    build();
            final Map<String, Integer> collection = new HashMap<>();
            final List<BukkitUser> users = statement.execute(this.plugin.getDataManager().getConnection(), resultSet -> {
                final String itemId = resultSet.getString(ITEM_ID);
                final int amount = resultSet.getInt(AMOUNT);
                collection.put(itemId, amount);
                return new BukkitUser(uuid, new Collection(collection), new Cooldowns(new HashMap<>()), boosters);
            });
            BukkitUser user = users.isEmpty() ? new BukkitUser(uuid, new Collection(new HashMap<>()), new Cooldowns(new HashMap<>()), boosters) : users.get(0);
            this.addUser(user);
            onLoad.accept(user);
        });
    }

    private static final String SHOULD_STORE_PATH = "should-store";
    private static final String START_ITEMS_PATH = "start-items";
    private static final String COLLECTION_CATEGORIES_PATH = "collection-categories";
    private static final String ITEM = "item";
    private static final String COLLECTIONS = "collections";
    private static final String AMOUNT_PATH = "amount";
    private static final String COLLECTION_PERMISSIONS_PATH = "collection-permissions";
    private static final String PERMISSION_PATH = "permission";
    private static final String CATEGORY_PATH = "category";


    public void load(SkyblockLevels plugin) {
        this.shouldStore.clear();
        this.collectionCategories.clear();
        this.collectionPermissions.clear();
        this.startItems.clear();
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
            final ConfigurationNode startItemsNode = source.node(START_ITEMS_PATH);
            this.startItems.addAll(startItemsNode.getList(String.class, new ArrayList<>()));
            final ConfigurationNode shouldStore = source.node(SHOULD_STORE_PATH);
            final ConfigurationNode collectionCategories = source.node(COLLECTION_CATEGORIES_PATH);
            for (var entry : collectionCategories.childrenMap().entrySet()) {
                if (!(entry.getKey() instanceof final String category)) continue;
                final ConfigurationNode node = entry.getValue();
                final ItemSupplier supplier = ItemSerializer.deserialize(node.node(ITEM));
                final List<String> collections = node.node(COLLECTIONS).getList(String.class, new ArrayList<>());
                final CollectionCategory collectionCategory = new CollectionCategory(this.plugin, category, supplier, collections);
                this.collectionCategories.put(category, collectionCategory);
            }
            this.shouldStore.addAll(shouldStore.getList(String.class, new ArrayList<>()));
            this.collectionPermissions.clear();
            final ConfigurationNode collectionPermissions = source.node(COLLECTION_PERMISSIONS_PATH);
            for (var entry : collectionPermissions.childrenMap().entrySet()) {
                if (!(entry.getKey() instanceof final String id)) continue;
                final ConfigurationNode node = entry.getValue();
                final String permission = node.node(PERMISSION_PATH).getString();
                final int amount = node.node(AMOUNT_PATH).getInt(0);
                final String category = node.node(CATEGORY_PATH).getString("");
                final CollectionCategory collectionCategory = this.collectionCategories.get(category);
                if (collectionCategory != null) {
                    collectionCategory.getTypes().add(id);
                }
                this.collectionPermissions.put(id, new CollectionPermission(id, amount, permission, category));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.collectionTop.load(this.collectionCategories.values().
                stream().
                flatMap(category -> category.getTypes().stream()).
                collect(Collectors.toList()));
    }

}

