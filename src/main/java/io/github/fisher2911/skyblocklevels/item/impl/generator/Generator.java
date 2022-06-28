package io.github.fisher2911.skyblocklevels.item.impl.generator;

import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.item.Delayed;
import io.github.fisher2911.skyblocklevels.item.ItemSerializer;
import io.github.fisher2911.skyblocklevels.item.ItemSupplier;
import io.github.fisher2911.skyblocklevels.item.MineSpeeds;
import io.github.fisher2911.skyblocklevels.item.SkyBlock;
import io.github.fisher2911.skyblocklevels.user.BukkitUser;
import io.github.fisher2911.skyblocklevels.user.User;
import io.github.fisher2911.skyblocklevels.util.weight.Weight;
import io.github.fisher2911.skyblocklevels.util.weight.WeightedList;
import io.github.fisher2911.skyblocklevels.world.WorldPosition;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Generator implements SkyBlock, Delayed {

    private final SkyblockLevels plugin;
    private final long id;
    private final String itemId;
    private final ItemSupplier itemSupplier;
    private final int tickDelay;
    private final Material resetBlock;
    private final Material generatorBlock;
    private final WeightedList<Supplier<ItemStack>> items;
    private final int ticksToBreak;
    private final MineSpeeds mineSpeeds;

    private int tickCounter;
    private boolean isGenerated;
    private boolean running = true;
    private final Function<Player, Integer> mineSpeedFunction;

    public Generator(
            SkyblockLevels plugin,
            long id,
            String itemId,
            ItemSupplier itemSupplier,
            int tickDelay,
            Material resetBlock,
            Material generatorBlock,
            WeightedList<Supplier<ItemStack>> items,
            int ticksToBreak,
            MineSpeeds mineSpeeds
    ) {
        this.plugin = plugin;
        this.id = id;
        this.itemId = itemId;
        this.tickDelay = tickDelay;
        this.itemSupplier = itemSupplier;
        this.resetBlock = resetBlock;
        this.generatorBlock = generatorBlock;
        this.items = items;
        this.ticksToBreak = ticksToBreak;
        this.mineSpeeds = mineSpeeds;
        this.mineSpeedFunction = player -> {
            final ItemStack inHand = player.getInventory().getItemInMainHand();
            return this.mineSpeeds.getModifier(this.plugin.getItemManager(), inHand).modify(this.ticksToBreak);
        };
    }

    @Override
    public void onBreak(User user, BlockBreakEvent event) {
        final Block block = event.getBlock();
        final WorldPosition position = WorldPosition.fromLocation(block.getLocation());
        if (event.getPlayer().isSneaking()) {
            this.plugin.getWorlds().removeBlock(WorldPosition.fromLocation(block.getLocation()));
            this.plugin.getItemManager().giveItem(user, this);
            block.setType(Material.AIR);
            this.running = false;
            this.plugin.getBlockBreakManager().cancel(position);
            user.sendMessage("Broke generator shifting");
            return;
        }
        this.plugin.getBlockBreakManager().reset(position);
        event.setCancelled(true);
        final Player player = event.getPlayer();
        this.plugin.getBlockBreakManager().startMining(
                p -> Integer.MAX_VALUE,
                player,
                position,
                p -> Bukkit.getScheduler().runTask(
                        this.plugin,
                        () -> this.onBreak(user, new BlockBreakEvent(block, player))
                )
        );
        if (!this.isGenerated || block.getDrops(event.getPlayer().getInventory().getItemInMainHand()).isEmpty()) {
            this.isGenerated = false;
            block.setType(this.resetBlock);
            return;
        }
        final Location location = block.getLocation().add(0, 1, 0);
        final Supplier<ItemStack> weight = this.items.getRandom();
        if (weight == null) return;
        location.getWorld().dropItem(location, weight.get());
        this.isGenerated = false;
        this.tickCounter = 0;
        block.setType(this.resetBlock);
    }

    @Override
    public void onPlace(User user, BlockPlaceEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            event.getPlayer().sendMessage("Slot: " + event.getHand());
            return;
        }
        final Block block = event.getBlock();
        this.plugin.getWorlds().addBlock(this, WorldPosition.fromLocation(block.getLocation()));
        block.setType(this.resetBlock);
        event.getPlayer().sendMessage("§aGenerator placed: " + this.resetBlock + " " + event.isCancelled());
    }

    @Override
    public void onClick(User user, PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!this.isGenerated) {
            user.sendMessage("<red>You must wait " + (this.getTimeLeft() / 20) + "seconds before you can mine this block");
            return;
        }
        user.sendMessage("<red>Mine this block to collect it");
    }

    @Override
    public void tick(WorldPosition worldPosition) {
        if (!this.running) return;
        final Block block = worldPosition.toLocation().getBlock();
        if (this.tickCounter++ < this.tickDelay) return;
        if (this.isGenerated) {
            if (block.getType() != this.generatorBlock) block.setType(this.generatorBlock);
            return;
        }
        this.tickCounter = 0;
        if (block.getType() == this.generatorBlock) return;
        block.setType(this.generatorBlock);
        this.isGenerated = true;
        this.plugin.getBlockBreakManager().updateTicks(
                this.mineSpeedFunction,
                WorldPosition.fromLocation(block.getLocation())
        );
    }

    @Override
    public void onMineBlock(User user, Block block) {
        if (block == null) return;
        if (!(user instanceof BukkitUser bukkitUser)) return;
        final Player player = bukkitUser.getPlayer();
        if (!this.isGenerated) {
            this.plugin.getBlockBreakManager().startMining(
                    p -> Integer.MAX_VALUE,
                    player,
                    WorldPosition.fromLocation(block.getLocation()),
                    (position) -> Bukkit.getScheduler().runTask(
                            this.plugin,
                            () -> this.onBreak(user, new BlockBreakEvent(block, player))
                    )
            );
            return;
        }
        if (player == null) return;
        this.plugin.getBlockBreakManager().startMining(
                this.mineSpeedFunction,
                player,
                WorldPosition.fromLocation(block.getLocation()),
                (position) -> Bukkit.getScheduler().runTask(
                        this.plugin,
                        () -> this.onBreak(user, new BlockBreakEvent(block, player))
                )
        );
    }

    @Override
    public void onBlockDamage(User user, BlockDamageEvent event) {
        event.setCancelled(true);
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

    private int getTimeLeft() {
        return this.tickDelay - this.tickCounter;
    }

    @Override
    public int getTickDelay() {
        return this.tickDelay;
    }

    @Override
    public boolean uniqueInInventory() {
        return false;
    }

    public static Generator.Serializer serializer() {
        return Generator.Serializer.INSTANCE;
    }

    public static final class Serializer implements TypeSerializer<Supplier<Generator>> {

        private static final Generator.Serializer INSTANCE = new Generator.Serializer();

        private Serializer() {
        }

        private static final String ITEM_ID = "item-id";
        private static final String ITEM = "item";
        private static final String TICK_DELAY = "tick-delay";
        private static final String RESET_BLOCK = "reset-block";
        private static final String GENERATOR_BLOCK = "generator-block";
        private static final String ITEMS = "items";
        private static final String TICKS_TO_BREAK = "ticks-to-break";
        private static final String SPEED_MODIFIERS = "speed-modifiers";

        @Override
        public Supplier<Generator> deserialize(Type type, ConfigurationNode node) {
            try {
                final String itemId = node.node(ITEM_ID).getString();
                final ItemSupplier itemSupplier = ItemSerializer.deserialize(node.node(ITEM));
                final int tickDelay = node.node(TICK_DELAY).getInt();
                final Material resetBlock = Material.valueOf(node.node(RESET_BLOCK).getString());
                final Material generatorBlock = Material.valueOf(node.node(GENERATOR_BLOCK).getString());
                final TypeSerializer<WeightedList<ItemSupplier>> serializer = WeightedList.serializer(ItemSupplier.class, ItemSerializer.INSTANCE);
                final WeightedList<Supplier<ItemStack>> items =
                        new WeightedList<>(
                                serializer.deserialize(WeightedList.class, node.node(ITEMS)).
                                        getWeightList().
                                        stream().
                                        map(w -> new Weight<>((Supplier<ItemStack>) () -> w.getValue().get(), w.getWeight())).
                                        collect(Collectors.toList()));
                final int ticksToBreak = node.node(TICKS_TO_BREAK).getInt();
                final SkyblockLevels plugin = SkyblockLevels.getPlugin(SkyblockLevels.class);
                final MineSpeeds speeds = MineSpeeds.serializer().deserialize(MineSpeeds.class, node.node(SPEED_MODIFIERS));
                return () -> new Generator(plugin, plugin.getItemManager().generateNextId(), itemId, itemSupplier, tickDelay, resetBlock, generatorBlock, items, ticksToBreak, speeds);
            } catch (SerializationException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void serialize(Type type, @Nullable Supplier<Generator> obj, ConfigurationNode node) throws SerializationException {

        }
    }
}
