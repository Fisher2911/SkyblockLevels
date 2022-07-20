package io.github.fisher2911.skyblocklevels.item.impl;

import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.item.ItemBuilder;
import io.github.fisher2911.skyblocklevels.item.ItemSerializer;
import io.github.fisher2911.skyblocklevels.item.ItemSupplier;
import io.github.fisher2911.skyblocklevels.item.SkyBlock;
import io.github.fisher2911.skyblocklevels.item.SpecialSkyItem;
import io.github.fisher2911.skyblocklevels.item.Usable;
import io.github.fisher2911.skyblocklevels.user.User;
import io.github.fisher2911.skyblocklevels.world.WorldPosition;
import net.kyori.adventure.text.Component;
import org.bukkit.block.Block;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.function.Supplier;

public class InfoItem implements Usable, SpecialSkyItem {

    private final SkyblockLevels plugin;
    private final long id;
    private final String itemId;
    private final ItemSupplier itemSupplier;


    public InfoItem(SkyblockLevels plugin, long id, String itemId, ItemSupplier itemSupplier) {
        this.plugin = plugin;
        this.id = id;
        this.itemId = itemId;
        this.itemSupplier = itemSupplier;
    }

    @Override
    public void onUse(User user, PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK || !event.getPlayer().isSneaking()) return;
        final Block block = event.getClickedBlock();
        if (block == null) return;
        final SkyBlock skyBlock = this.plugin.getWorlds().getBlockAt(WorldPosition.fromLocation(block.getLocation()));
        if (skyBlock == SkyBlock.EMPTY) return;
        event.setCancelled(true);
        final ItemBuilder itemBuilder = ItemBuilder.from(skyBlock.getItemStack());
        user.sendMessage("<green>Block Info:");
        user.sendMessage(itemBuilder.name());
        for (Component line : itemBuilder.lore()) {
            user.sendMessage(line);
        }
    }

    @Override
    public void onItemDamage(User user, PlayerItemDamageEvent event) {
    }

    @Override
    public int getDurability() {
        return 0;
    }

    @Override
    public void takeDamage(ItemStack itemStack, int damage) {

    }

    @Override
    public ItemStack getItemStack() {
        return this.itemSupplier.get();
    }

    @Override
    public String getItemId() {
        return this.itemId;
    }

    @Override
    public long getId() {
        return this.id;
    }

    @Override
    public boolean uniqueInInventory() {
        return false;
    }

    public static Serializer serializer() {
        return Serializer.INSTANCE;
    }

    public static class Serializer implements TypeSerializer<Supplier<InfoItem>> {


        private static final Serializer INSTANCE = new Serializer();

        private Serializer() {
        }

        private static final String ITEM_ID = "item-id";
        private static final String ITEM = "item";
        private static final String UNIQUE = "unique";

        @Override
        public Supplier<InfoItem> deserialize(Type type, ConfigurationNode node) {
            try {
                final String itemId = node.node(ITEM_ID).getString();
                final ItemSupplier itemSupplier = ItemSerializer.deserialize(node.node(ITEM));
                final boolean unique = node.node(UNIQUE).getBoolean();
                final SkyblockLevels plugin = SkyblockLevels.getPlugin(SkyblockLevels.class);
                return () -> new InfoItem(plugin, unique ? plugin.getDataManager().generateNextId() : -1, itemId, itemSupplier);
            } catch (SerializationException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void serialize(Type type, @Nullable Supplier<InfoItem> obj, ConfigurationNode node) throws SerializationException {

        }
    }
}
