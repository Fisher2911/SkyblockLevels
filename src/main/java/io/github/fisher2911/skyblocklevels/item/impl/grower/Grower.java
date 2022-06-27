package io.github.fisher2911.skyblocklevels.item.impl.grower;

import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.item.ItemSerializer;
import io.github.fisher2911.skyblocklevels.item.ItemSupplier;
import io.github.fisher2911.skyblocklevels.item.SkyBlock;
import io.github.fisher2911.skyblocklevels.user.User;
import io.github.fisher2911.skyblocklevels.util.DirectionUtil;
import io.github.fisher2911.skyblocklevels.util.Random;
import io.github.fisher2911.skyblocklevels.util.weight.Weight;
import io.github.fisher2911.skyblocklevels.util.weight.WeightedList;
import io.github.fisher2911.skyblocklevels.world.WorldPosition;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.type.Sapling;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Grower implements SkyBlock {

    private final SkyblockLevels plugin;
    private final long id;
    private final String itemId;
    private final ItemSupplier itemSupplier;
    private final Material block;
    private final int tickDelay;
    private final Function<Material, Double> growChances;
    private WorldPosition worldPosition;

    private int tickCounter;

    public Grower(SkyblockLevels plugin, long id, String itemId, ItemSupplier itemSupplier, Material block, int tickDelay, Map<Material, Double> growChances) {
        this(plugin, id, itemId, itemSupplier, block, tickDelay, growChances::get);
    }

    public Grower(SkyblockLevels plugin, long id, String itemId, ItemSupplier itemSupplier, Material block, int tickDelay, WeightedList<Material> growChances) {
        this(plugin, id, itemId, itemSupplier, block, tickDelay, growChances.getWeightList().stream().collect(Collectors.toMap(Weight::getValue, Weight::getWeight)));
    }

    public Grower(SkyblockLevels plugin, long id, String itemId, ItemSupplier itemSupplier, Material block, int tickDelay, Function<Material, Double> growChances) {
        this.plugin = plugin;
        this.id = id;
        this.itemId = itemId;
        this.itemSupplier = itemSupplier;;
        this.block = block;
        this.tickDelay = tickDelay;
        this.growChances = growChances;
    }

    @Override
    public void onBreak(User user, BlockBreakEvent event) {
        this.plugin.getItemManager().giveItem(user, this);
        this.plugin.getWorlds().removeBlock(this.worldPosition);
    }

    @Override
    public void onPlace(User user, BlockPlaceEvent event) {
        final Block block = event.getBlock();
        this.worldPosition = WorldPosition.fromLocation(block.getLocation());
        this.plugin.getWorlds().addBlock(this, this.worldPosition);
        block.setType(this.block);
        DirectionUtil.setBlockDirection(block, BlockFace.UP);
        event.getItemInHand().setAmount(event.getItemInHand().getAmount() - 1);
    }

    @Override
    public void onClick(User user, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getPlayer().isSneaking() && event.getItem() != null && event.getItem().getType().isBlock()) return;
        event.setCancelled(true);
    }

    @Override
    public void tick(WorldPosition worldPosition) {
        if (this.tickCounter++ < this.tickDelay) return;
        this.tickCounter = 0;
        final Block above = worldPosition.toLocation().getBlock().getRelative(BlockFace.UP).getRelative(BlockFace.UP);
        final Double chance = this.growChances.apply(above.getType());
        if (chance == null) return;
        final double random = Random.nextDouble(100);
        if (random > chance) return;
        if (above.getBlockData() instanceof Sapling) {
            above.applyBoneMeal(BlockFace.UP);
            return;
        }
        if (above.getBlockData() instanceof Ageable ageable) {
            if (ageable.getAge() == ageable.getMaximumAge()) return;
            above.applyBoneMeal(BlockFace.UP);
        }
    }

    @Override
    public void onMineBlock(User user, Block block) {
        user.sendMessage("Mining");
    }

    @Override
    public void onBlockDamage(User user, BlockDamageEvent event) {

    }

    @Override
    public boolean uniqueInInventory() {
        return false;
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public long getId() {
        return this.id;
    }

    @Override
    public String getItemId() {
        return this.itemId;
    }

    @Override
    public ItemStack getItemStack() {
        return this.itemSupplier.get();
    }

    public static Serializer serializer() {
        return Serializer.INSTANCE;
    }

    public static final class Serializer implements TypeSerializer<Supplier<Grower>> {

        private static final Serializer INSTANCE = new Serializer();

        private Serializer() {
        }

        private static final String ITEM_ID = "item-id";
        private static final String ITEM = "item";
        private static final String BLOCK = "block";
        private static final String TICK_DELAY = "tick-delay";
        private static final String ITEMS = "items";

        @Override
        public Supplier<Grower> deserialize(Type type, ConfigurationNode node) {
            try {
                final String itemId = node.node(ITEM_ID).getString();
                final ItemSupplier itemSupplier = ItemSerializer.deserialize(node.node(ITEM));
                final Material block = Material.valueOf(node.node(BLOCK).getString());
                final int tickDelay = node.node(TICK_DELAY).getInt();
                final TypeSerializer<WeightedList<Material>> serializer = WeightedList.serializer(Material.class, null);
                final WeightedList<Material> items =
                        new WeightedList<>(
                                serializer.deserialize(WeightedList.class, node.node(ITEMS)).
                                        getWeightList().
                                        stream().
                                        map(w -> new Weight<>(w.getValue(), w.getWeight())).
                                        collect(Collectors.toList()));

                final SkyblockLevels plugin = SkyblockLevels.getPlugin(SkyblockLevels.class);
                return () -> new Grower(plugin, plugin.getItemManager().generateNextId(), itemId, itemSupplier, block, tickDelay, items);
            } catch (SerializationException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void serialize(Type type, @Nullable Supplier<Grower> obj, ConfigurationNode node) throws SerializationException {

        }
    }
}
