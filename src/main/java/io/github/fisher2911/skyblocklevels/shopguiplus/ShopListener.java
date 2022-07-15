package io.github.fisher2911.skyblocklevels.shopguiplus;

import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.item.SpecialSkyItem;
import io.github.fisher2911.skyblocklevels.item.impl.SkyItem;
import io.github.fisher2911.skyblocklevels.user.User;
import io.github.fisher2911.skyblocklevels.user.UserManager;
import net.brcdev.shopgui.ShopGuiPlusApi;
import net.brcdev.shopgui.event.ShopGUIPlusPostEnableEvent;
import net.brcdev.shopgui.event.ShopPreTransactionEvent;
import net.brcdev.shopgui.shop.ShopItem;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public class ShopListener implements Listener {

    private final SkyblockLevels plugin;
    private final UserManager userManager;

    public ShopListener(SkyblockLevels plugin) {
        this.plugin = plugin;
        this.userManager = plugin.getUserManager();
    }

    @EventHandler
    public void onShopEnable(ShopGUIPlusPostEnableEvent event) {
        this.plugin.getLogger().severe("ShopGUIPlus is now enabling!");
        ShopGuiPlusApi.registerItemProvider(new SkyItemProvider(this.plugin));
        this.plugin.getLogger().severe("ShopGUIPlus is now enabled!");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPurchase(ShopPreTransactionEvent event) {
        final ShopItem item = event.getShopItem();
        final ItemStack itemStack = item.getItem();
        final SpecialSkyItem skyItem = this.plugin.getItemManager().getItem(itemStack);
        if (skyItem == SkyItem.EMPTY) return;
        final User user = this.userManager.getUser(event.getPlayer());
        if (user == null) return;
        if (this.userManager.hasPermissionForCollection(user, skyItem.getItemId())) return;
        event.setCancelled(true);
        user.sendMessage(ChatColor.RED + "You don't have permission to purchase this item.");
    }

}
