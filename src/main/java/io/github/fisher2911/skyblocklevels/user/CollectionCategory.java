package io.github.fisher2911.skyblocklevels.user;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.item.ItemBuilder;
import io.github.fisher2911.skyblocklevels.item.ItemManager;
import io.github.fisher2911.skyblocklevels.item.ItemSupplier;
import io.github.fisher2911.skyblocklevels.item.Spawner;
import io.github.fisher2911.skyblocklevels.item.SpecialSkyItem;
import io.github.fisher2911.skyblocklevels.message.Adventure;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class CollectionCategory {

    private final SkyblockLevels plugin;
    private final String id;
    private final ItemSupplier menuItem;
    private final List<String> types;

    public CollectionCategory(SkyblockLevels plugin, String id, ItemSupplier menuItem, List<String> types) {
        this.plugin = plugin;
        this.id = id;
        this.menuItem = menuItem;
        this.types = types;
    }

    public void showMenu(BukkitUser user) {
        final ItemManager itemManager = this.plugin.getItemManager();
        final UserManager userManager = this.plugin.getUserManager();
        final int rows = (int) Math.ceil((float) this.types.size() / 7f) + 2;
        final Gui gui = Gui.gui().
                rows(rows).
                disableAllInteractions().
                title(Adventure.parse("<green>Collection requirements")).
                create();
        gui.getFiller().fillBorder(new GuiItem(ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).name(" ").build()));
        for (String type : types) {
            ItemBuilder builder;
            int amount = user.getCollection().getAmount(type);
            try {
                builder = ItemBuilder.from(new ItemStack(Material.valueOf(type)));
            } catch (IllegalArgumentException e) {
                final SpecialSkyItem item = itemManager.getItem(type);
                if (item instanceof final Spawner spawner) {
                    amount = spawner.getCollectionAmount(user);
                }
                builder = ItemBuilder.from(itemManager.getItem(item));
            }
            builder.lore("").lore("<green>Collected: " + amount + " / " + userManager.getCollectionRequirement(type));
            gui.addItem(new GuiItem(builder.build()));
        }
        gui.setItem(rows * 9 - 5, new GuiItem(ItemBuilder.from(Material.ARROW).name("Back").build(), event -> userManager.showMenu(user)));
        final Player player = user.getPlayer();
        if (player == null) return;
        gui.open(player);
    }

    public String getId() {
        return id;
    }

    public ItemStack getMenuItem() {
        return menuItem.get();
    }

    public List<String> getTypes() {
        return types;
    }
}
