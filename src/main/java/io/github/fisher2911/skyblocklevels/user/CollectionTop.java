package io.github.fisher2911.skyblocklevels.user;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.components.GuiType;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.database.SelectStatement;
import io.github.fisher2911.skyblocklevels.item.SpecialSkyItem;
import io.github.fisher2911.skyblocklevels.item.impl.spawner.MobSpawner;
import io.github.fisher2911.skyblocklevels.message.Adventure;
import io.github.fisher2911.skyblocklevels.util.Pair;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class CollectionTop {

    private final SkyblockLevels plugin;
    private final Map<String, Map<UUID, Integer>> topCategories;

    public CollectionTop(SkyblockLevels plugin, Map<String, Map<UUID, Integer>> topCategories) {
        this.plugin = plugin;
        this.topCategories = topCategories;
    }

    public Map<UUID, Integer> getTopInCategory(String category) {
        return topCategories.getOrDefault(category, new HashMap<>());
    }

    public void load(List<String> categories) {
        for (String category : categories) {
            this.load(category);
        }
    }

    private void load(String itemId) {
        final SpecialSkyItem item = plugin.getItemManager().getItem(itemId);
        if (item instanceof final MobSpawner spawner) {
            final var types = spawner.getEntityTypes();
            if (!types.isEmpty()) itemId = types.get(0);
        }

        final SelectStatement statement = SelectStatement.
                builder(UserManager.TABLE).
                whereEqual(UserManager.ITEM_ID, itemId).
                limit(10).
                orderDesc(UserManager.AMOUNT).
                build();
        List<Pair<UUID, Integer>> list = statement.execute(this.plugin.getDataManager().getConnection(), resultSet -> new Pair<>(
                UUID.fromString(resultSet.getString(UserManager.UUID)),
                resultSet.getInt(UserManager.AMOUNT)
        ));
        for (Pair<UUID, Integer> pair : list) {
            final UUID uuid = pair.getFirst();
            final int amount = pair.getSecond();
            this.topCategories.computeIfAbsent(itemId, k -> new HashMap<>()).put(uuid, amount);
        }
    }

    public void update(UUID uuid, Collection collection) {
        for (Map.Entry<String, Integer> entry : collection.getItemsCollected().entrySet()) {
            final String itemId = entry.getKey();
            final int amount = entry.getValue();
            this.topCategories.computeIfAbsent(itemId, k -> new HashMap<>()).put(uuid, amount);
        }
    }

    public List<Map.Entry<UUID, Integer>> getTop(String itemId) {
        return this.topCategories.getOrDefault(itemId, new HashMap<>()).entrySet().stream().
                sorted((a, b) -> b.getValue() - a.getValue()).
                limit(10).
                collect(Collectors.toList());
    }

    private static final Map<Integer, Integer> TOP_10_SLOTS = Map.of(
            1, 4,
            2, 12,
            3, 14,
            4, 20,
            5, 22,
            6, 24,
            7, 28,
            8, 30,
            9, 32,
            10, 34
    );

    public void openDefaultMenu(BukkitUser user) {
        this.plugin.getUserManager().showMenu(
                user,
                null,
                category -> category.showMenu(
                        user,
                        builder -> builder.lore("", "<blue>Click to view the top 10 of this category.").build(),
                        itemId -> this.displayMenu(user, itemId)
                )
        );
    }

    public void displayMenu(BukkitUser user, String category) {
        final SpecialSkyItem item = this.plugin.getItemManager().getItem(category);
        if (item instanceof final MobSpawner spawner) {
            final var types = spawner.getEntityTypes();
            if (!types.isEmpty()) category = types.get(0);
        }
        final var top = this.getTop(category);
        int i = 1;
        final Gui gui = Gui.gui(GuiType.CHEST).rows(4).title(Adventure.parse("<blue>Top 10 in " + category)).disableAllInteractions().create();
        gui.setDragAction(event -> event.setCancelled(true));
        for (var entry : top) {
            final int slot = TOP_10_SLOTS.get(i++);
            final UUID uuid = entry.getKey();
            final int amount = entry.getValue();
            final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            final ItemStack itemStack = ItemBuilder.
                    skull().
                    amount(1).
                    owner(offlinePlayer).
                    name(Adventure.parse("<gold>" + offlinePlayer.getName() + " - " + amount)).
                    build();
            gui.setItem(slot, new GuiItem(itemStack));
        }
        final Player player = user.getPlayer();
        if (player == null) return;
        gui.open(player);
    }

}
