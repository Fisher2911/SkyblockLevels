package io.github.fisher2911.skyblocklevels.item;

import io.github.fisher2911.skyblocklevels.item.impl.ExplosionTool;
import io.github.fisher2911.skyblocklevels.user.User;
import io.github.fisher2911.skyblocklevels.util.Keys;
import io.github.fisher2911.skyblocklevels.util.TriConsumer;
import io.github.fisher2911.skyblocklevels.util.WorldUtil;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ItemManager {

    private final Map<String, SpecialSkyItem> items;

    private final Map<Class<? extends Event>, Collection<Class<?>>> classMap = Map.of(
            BlockBreakEvent.class, List.of(SkyTool.class, SkyBlock.class),
            BlockPlaceEvent.class, List.of(SkyBlock.class),
            EntityDamageEvent.class, List.of(SkyWeapon.class),
            PlayerInteractEvent.class, List.of(Usable.class)
    );
    private final Map<Class<?>, TriConsumer<Object, Object, Object>> itemActions = Map.of(
            SkyTool.class, (u, s, e) -> {
                if (e instanceof BlockBreakEvent event) {
                    ((SkyTool) s).onBreak((User) u, event);
                    return;
                }
                ((SkyTool) s).onUse((User) u, (PlayerInteractEvent) e);
            },
            SkyBlock.class, (u, s, e) -> {
                if (!(s instanceof SkyBlock skyBlock)) return;
                if (e instanceof BlockBreakEvent event) {
                    (skyBlock).onBreak((User) u, event);
                    return;
                }
                skyBlock.onPlace((User) u, (BlockPlaceEvent) e);
            },
            SkyWeapon.class, (u, s, e) -> ((SkyWeapon) s).onAttack((User) u, (EntityDamageEvent) e),
            Usable.class, (u, s, e) -> ((Usable) s).onUse((User) u, (PlayerInteractEvent) e)
    );

    public ItemManager(Map<String, SpecialSkyItem> items) {
        this.items = items;
        this.register(new ExplosionTool());
    }

    public void register(SpecialSkyItem item) {
        items.put(item.getSkyItem().getId(), item);
    }

    public SpecialSkyItem getItem(String id) {
        return this.items.getOrDefault(id, SpecialSkyItem.EMPTY);
    }

    public void handle(User user, SpecialSkyItem item, Event event) {
        user.sendMessage("Events: " + this.classMap.get(event.getClass()).size());
        for (Class<?> clazz : this.classMap.get(event.getClass())) {
            final TriConsumer<Object, Object, Object> consumer = this.itemActions.get(clazz);
            if (consumer == null) continue;
            consumer.accept(user, item, event);
        }
    }

    public void handle(User user, ItemStack itemStack, Event event) {
        final String id = Keys.getSkyItem(itemStack);
        if (id.isEmpty()) {
            user.sendMessage("Not sky item: " + id);
            return;
        }
        user.sendMessage("Is sky item: " + id);
        this.handle(user, this.getItem(id), event);
    }

    public void giveItem(User user, String id) {
        final ItemStack item = this.getItem(id).getSkyItem().getItem();
        Keys.setSkyItem(item, id);
        WorldUtil.addItemToInventory(item, user);
    }
}
