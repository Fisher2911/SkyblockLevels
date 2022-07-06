package io.github.fisher2911.skyblocklevels.item;

import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.item.impl.SkyItem;
import io.github.fisher2911.skyblocklevels.user.BukkitUser;
import io.github.fisher2911.skyblocklevels.user.User;
import io.github.fisher2911.skyblocklevels.util.Keys;
import io.github.fisher2911.skyblocklevels.util.TriConsumer;
import io.github.fisher2911.skyblocklevels.util.WorldUtil;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class ItemManager {

    private final SkyblockLevels plugin;
    //    private final Map<String, SpecialSkyItem> items;
    private final Map<Long, SpecialSkyItem> items;
    private final Map<String, Supplier<? extends SpecialSkyItem>> itemSuppliers;
    private final AtomicLong IDS = new AtomicLong();

    private final Map<Class<? extends Event>, Collection<Class<?>>> classMap = Map.of(
            BlockBreakEvent.class, List.of(SkyTool.class, SkyBlock.class),
            BlockPlaceEvent.class, List.of(SkyBlock.class, SkyItem.class),
            EntityDamageEvent.class, List.of(SkyWeapon.class, Spawner.class),
            PlayerInteractEvent.class, List.of(Usable.class, SkyBlock.class),
            BlockRedstoneEvent.class, List.of(RedstoneBlock.class),
            BlockDamageEvent.class, List.of(SkyBlock.class),
            PlayerItemDamageEvent.class, List.of(Usable.class),
            EntitySpawnEvent.class, List.of(Spawner.class),
            SpawnerSpawnEvent.class, List.of(Spawner.class)
    );
    private final Map<Class<?>, TriConsumer<Object, Object, Object>> itemActions = Map.of(
            SkyTool.class, (u, s, e) -> {
                if (!(s instanceof SkyTool skyTool)) return;
                if (e instanceof BlockBreakEvent event) {
                    skyTool.onBreak((User) u, event);
                    return;
                }
                skyTool.onUse((User) u, (PlayerInteractEvent) e);
            },
            SkyBlock.class, (u, s, e) -> {
                if (!(s instanceof SkyBlock skyBlock)) return;
                if (e instanceof BlockBreakEvent event) {
                    (skyBlock).onBreak((User) u, event);
                    return;
                }
                if (e instanceof PlayerInteractEvent event) {
                    (skyBlock).onClick((User) u, event);
                    return;
                }
                if (e instanceof BlockDamageEvent event) {
                    (skyBlock).onBlockDamage((User) u, event);
                    return;
                }
                skyBlock.onPlace((User) u, (BlockPlaceEvent) e);
            },
            SkyWeapon.class, (u, s, e) -> {
                if (!(s instanceof SkyWeapon skyWeapon)) return;
                skyWeapon.onAttack((User) u, (EntityDamageEvent) e);
            },
            Usable.class, (u, s, e) -> {
                if (!(s instanceof Usable usable)) return;
                if (e instanceof final PlayerItemDamageEvent event) {
                    usable.onItemDamage((User) u, event);
                    return;
                }
                if (e instanceof final PlayerInteractEvent event) {
                    usable.onUse((User) u, event);
                }
            },
            RedstoneBlock.class, (u, s, e) -> {
                if (!(s instanceof RedstoneBlock redstoneBlock)) return;
                redstoneBlock.onActivate((User) u, (BlockRedstoneEvent) e);
            },
            Spawner.class, (u, s, e) -> {
                if (!(s instanceof Spawner spawner)) return;
                if (e instanceof final EntitySpawnEvent event) {
                    spawner.onSpawn(event.getEntity());
                    return;
                }
                if (e instanceof final EntityDamageEvent event) {
                    spawner.onDamage(event);
                }
            },
            SkyItem.class, (u, s, e) -> {
                if (!(s instanceof SkyItem)) return;
                Bukkit.broadcastMessage("Is Sky Item");
                if (e instanceof Cancellable cancellable) cancellable.setCancelled(true);
            }
    );

    public ItemManager(SkyblockLevels plugin, Map<Long, SpecialSkyItem> items, Map<String, Supplier<? extends SpecialSkyItem>> itemSuppliers) {
        this.plugin = plugin;
        this.items = items;
        this.itemSuppliers = itemSuppliers;
    }

    public void reload() {
        this.itemSuppliers.clear();
        this.registerAll();
    }

    public void register(String type, Supplier<? extends SpecialSkyItem> supplier) {
        this.itemSuppliers.put(type, supplier);
    }

    public SpecialSkyItem createItem(String type) {
        final Supplier<? extends SpecialSkyItem> supplier = this.itemSuppliers.get(type);
        if (supplier == null) return SpecialSkyItem.EMPTY;
        return supplier.get();
    }

    public SpecialSkyItem getItem(long id) {
        return this.items.getOrDefault(id, SpecialSkyItem.EMPTY);
    }

    public SpecialSkyItem getItem(String id) {
        return this.createItem(id);
    }

    public SpecialSkyItem getItem(ItemStack itemStack) {
        final SpecialSkyItem item = this.getItem(Keys.getSkyItem(itemStack));
        if (item == SpecialSkyItem.EMPTY) return this.createItem(Keys.getSkyItemId(itemStack));
        return this.getItem(Keys.getSkyItem(itemStack));
    }

    public void handle(User user, SpecialSkyItem item, Event event) {
        final var classes = this.classMap.get(event.getClass());
        if (classes == null) return;
        for (Class<?> clazz : classes) {
            final TriConsumer<Object, Object, Object> consumer = this.itemActions.get(clazz);
            if (consumer == null) continue;
            consumer.accept(user, item, event);
        }
    }

    public void handle(User user, ItemStack itemStack, Event event) {
        final SpecialSkyItem item = this.getItem(itemStack);
        this.handle(user, item, event);
    }

    public void handle(BukkitUser user, long id, Event event) {
        this.handle(user, this.getItem(id), event);
    }

    public void giveItem(User user, SpecialSkyItem item) {
//        final ItemStack item = this.getItem(id).getSkyItem().getItem();
//        Keys.setSkyItem(item, id);
        // todo
        if (!(user instanceof BukkitUser bukkitUser)) return;
        WorldUtil.addItemToInventory(this.getItem(item), bukkitUser);
    }

    public ItemStack getItem(SpecialSkyItem item) {
        Bukkit.broadcastMessage("Got item with id: " + item.getItemId() + " " + item.getClass());
        final ItemStack itemStack = item.getItemStack();
        if (item.uniqueInInventory()) {
            Keys.setSkyItem(itemStack, item.getItemId(), item.getId());
            this.items.put(item.getId(), item);
        } else {
            Keys.setSkyItem(itemStack, item.getItemId());
        }
        return itemStack;
    }

    public long generateNextId() {
        return this.IDS.getAndAdd(1);
    }

    public Set<String> getAllIds() {
        return this.itemSuppliers.keySet();
    }

    public void delete(SpecialSkyItem item) {
        this.items.remove(item.getId());
    }

    public void registerAll() {
        ItemLoader.load();
    }


}
