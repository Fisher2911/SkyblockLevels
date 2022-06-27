package io.github.fisher2911.skyblocklevels.item.impl.bridger;

import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.item.ItemSupplier;
import io.github.fisher2911.skyblocklevels.user.User;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerInteractEvent;

public class UnlimitedBridger extends Bridger {

    private final Material type;
    private final int size;

    public UnlimitedBridger(SkyblockLevels plugin, long id, String itemId, ItemSupplier itemSupplier, Material type, int size) {
        super(plugin, id, itemId, itemSupplier);
        this.type = type;
        this.size = size;
    }

    @Override
    public void onUse(User user, PlayerInteractEvent event) {
        user.sendMessage("Placed " + this.place(event, this.type, this.size) + " blocks");
    }

    @Override
    public String getItemId() {
        return this.itemId;
    }

    @Override
    protected String getUsesLeft() {
        return "Unlimited";
    }

    @Override
    public boolean uniqueInInventory() {
        return true;
    }
}
