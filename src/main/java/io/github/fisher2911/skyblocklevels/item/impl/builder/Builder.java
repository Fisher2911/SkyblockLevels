package io.github.fisher2911.skyblocklevels.item.impl.builder;

import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.item.Delayed;
import io.github.fisher2911.skyblocklevels.item.ItemSerializer;
import io.github.fisher2911.skyblocklevels.item.ItemSupplier;
import io.github.fisher2911.skyblocklevels.item.RedstoneBlock;
import io.github.fisher2911.skyblocklevels.item.SkyBlock;
import io.github.fisher2911.skyblocklevels.user.User;
import io.github.fisher2911.skyblocklevels.util.DirectionUtil;
import io.github.fisher2911.skyblocklevels.world.WorldPosition;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.data.Directional;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.function.Supplier;

public class Builder implements SkyBlock, RedstoneBlock, Delayed {

    private static final String TABLE = Builder.class.getSimpleName().toLowerCase();

    private final SkyblockLevels plugin;
    private final long id;
    private final String itemId;
    private final ItemSupplier itemSupplier;

    private final int tickDelay;
    private int tickCounter = 0;

    public Builder(SkyblockLevels plugin, long id, String itemId, ItemSupplier itemSupplier, int tickDelay) {
        this.plugin = plugin;
        this.id = id;
        this.itemId = itemId;
        this.itemSupplier = itemSupplier;;
        this.tickDelay = tickDelay;
    }

    @Override
    public void onBreak(User user, BlockBreakEvent event) {
        this.plugin.getWorlds().removeBlock(WorldPosition.fromLocation(event.getBlock().getLocation()));
        this.plugin.getItemManager().giveItem(user, this);
    }

    @Override
    public void onBlockDamage(User user, BlockDamageEvent event) {

    }

    @Override
    public void onPlace(User user, BlockPlaceEvent event) {
        final Block block = event.getBlock();
        this.plugin.getWorlds().addBlock(this, WorldPosition.fromLocation(block.getLocation()));
        block.setType(Material.DISPENSER);
        DirectionUtil.setBlockDirection(block, event.getPlayer());
        event.getItemInHand().setAmount(event.getItemInHand().getAmount() - 1);
    }

    @Override
    public void onClick(User user, PlayerInteractEvent event) {

    }

    @Override
    public void tick(WorldPosition worldPosition) {
        if (!worldPosition.toLocation().getBlock().isBlockPowered()) return;
        this.doPlace(worldPosition);
    }

    @Override
    public void onActivate(User user, BlockRedstoneEvent event) {
        if (!event.getBlock().isBlockPowered()) return;
        this.doPlace(WorldPosition.fromLocation(event.getBlock().getLocation()));
    }

    private void doPlace(WorldPosition worldPosition) {
        if (this.tickCounter++ < this.tickDelay) return;
        this.tickCounter = 0;
        final Block block = worldPosition.toLocation().getBlock();
        if (!(block.getState() instanceof final Container container)) return;
        final Inventory inventory = container.getInventory();
        if (inventory.isEmpty()) return;
        if (!(block.getBlockData() instanceof final Directional directional)) return;
        final Block toPlace = block.getRelative(directional.getFacing());
        if (toPlace.getType().isSolid()) return;
        ItemStack place = new ItemStack(Material.AIR);
        for (ItemStack itemStack : inventory) {
            if (itemStack == null) continue;
            if (place.getType().isBlock()) {
                place = itemStack;
                break;
            }
        }
        if (place.getType() == Material.AIR) return;
        place.setAmount(place.getAmount() - 1);
        toPlace.setType(place.getType());
    }

    @Override
    public void onMineBlock(User user, Block block) {
        user.sendMessage("Mining");
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public ItemStack getItemStack() {
        return this.itemSupplier.get(PLACEHOLDERS, this);
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
    public int getTickDelay() {
        return tickDelay;
    }

    @Override
    public boolean uniqueInInventory() {
        return false;
    }

    @Override
    public String getTableName() {
        return TABLE;
    }

    public static Serializer serializer() {
        return Serializer.INSTANCE;
    }

    public static final class Serializer implements TypeSerializer<Supplier<Builder>> {

        private static final Builder.Serializer INSTANCE = new Builder.Serializer();

        private Serializer() {
        }

        private static final String ITEM_ID = "item-id";
        private static final String ITEM = "item";
        private static final String TICK_DELAY = "tick-delay";

        @Override
        public Supplier<Builder> deserialize(Type type, ConfigurationNode node) {
            try {
                final String itemId = node.node(ITEM_ID).getString();
                final ItemSupplier itemSupplier = ItemSerializer.deserialize(node.node(ITEM));
                final int tickDelay = node.node(TICK_DELAY).getInt();
                final SkyblockLevels plugin = SkyblockLevels.getPlugin(SkyblockLevels.class);
                return () -> new Builder(plugin, plugin.getDataManager().generateNextId(), itemId, itemSupplier, tickDelay);
            } catch (SerializationException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void serialize(Type type, @Nullable Supplier<Builder> obj, ConfigurationNode node) throws SerializationException {

        }
    }
}
