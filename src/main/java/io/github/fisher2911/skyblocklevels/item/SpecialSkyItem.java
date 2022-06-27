package io.github.fisher2911.skyblocklevels.item;

import org.bukkit.inventory.ItemStack;

public interface SpecialSkyItem {

    SpecialSkyItem EMPTY = new SpecialSkyItem() {
        @Override
        public ItemStack getItemStack() { return ItemBuilder.EMPTY.build(); }
        @Override
        public String getItemId() { return ""; }
        @Override
        public long getId() { return -1; }
        @Override
        public boolean uniqueInInventory() { return false; }
    };

    ItemStack getItemStack();
    String getItemId();
    long getId();
    boolean uniqueInInventory();

}
