package io.github.fisher2911.skyblocklevels.shopguiplus;

import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.item.SpecialSkyItem;
import io.github.fisher2911.skyblocklevels.item.impl.SkyItem;
import net.brcdev.shopgui.provider.item.ItemProvider;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public class SkyItemProvider extends ItemProvider {

    private final SkyblockLevels plugin;

    public SkyItemProvider(SkyblockLevels plugin) {
        super("skyitem");
        this.plugin = plugin;
    }

    @Override
    public boolean isValidItem(ItemStack itemStack) {
        return this.plugin.getItemManager().getItem(itemStack) != SkyItem.EMPTY;
    }

    @Override
    public ItemStack loadItem(ConfigurationSection configurationSection) {
        final String id = configurationSection.getString("id");
        if (id == null) return null;
        final SpecialSkyItem item = this.plugin.getItemManager().getItem(id);
        if (item == SkyItem.EMPTY) return null;
        final ItemStack itemStack = item.getItemStack();
        if (item.uniqueInInventory()) return itemStack;
        final int amount = configurationSection.getInt("amount", 1);
        itemStack.setAmount(amount);
        return itemStack;
    }

    @Override
    public boolean compare(ItemStack itemStack1, ItemStack itemStack2) {
        return Objects.equals(itemStack1, itemStack2);
    }
}
