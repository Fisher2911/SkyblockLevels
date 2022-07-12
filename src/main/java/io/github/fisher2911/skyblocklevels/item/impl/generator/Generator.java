package io.github.fisher2911.skyblocklevels.item.impl.generator;

import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.database.CreateTableStatement;
import io.github.fisher2911.skyblocklevels.database.DataManager;
import io.github.fisher2911.skyblocklevels.database.DeleteStatement;
import io.github.fisher2911.skyblocklevels.database.InsertStatement;
import io.github.fisher2911.skyblocklevels.database.KeyType;
import io.github.fisher2911.skyblocklevels.database.SelectStatement;
import io.github.fisher2911.skyblocklevels.database.VarChar;
import io.github.fisher2911.skyblocklevels.item.Delayed;
import io.github.fisher2911.skyblocklevels.item.Durable;
import io.github.fisher2911.skyblocklevels.item.ItemBuilder;
import io.github.fisher2911.skyblocklevels.item.ItemSerializer;
import io.github.fisher2911.skyblocklevels.item.ItemSupplier;
import io.github.fisher2911.skyblocklevels.item.MineSpeeds;
import io.github.fisher2911.skyblocklevels.item.SkyBlock;
import io.github.fisher2911.skyblocklevels.item.SpecialSkyItem;
import io.github.fisher2911.skyblocklevels.item.impl.DurableItem;
import io.github.fisher2911.skyblocklevels.message.Adventure;
import io.github.fisher2911.skyblocklevels.user.BukkitUser;
import io.github.fisher2911.skyblocklevels.user.CollectionCondition;
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
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Generator implements SkyBlock, Delayed, Durable {

    private static final String TABLE = Generator.class.getSimpleName().toLowerCase();
    private static final String ID = "id";
    private static final String ITEM_ID = "item_id";
    private static final String TICK_COUNTER = "tick_counter";

    static {
        final SkyblockLevels plugin = SkyblockLevels.getPlugin(SkyblockLevels.class);
        final DataManager dataManager = plugin.getDataManager();

        dataManager.addTable(CreateTableStatement.builder(TABLE).
                addField(Long.class, ID, KeyType.PRIMARY).
                addField(VarChar.ITEM_ID, ITEM_ID).
                addField(Integer.class, TICK_COUNTER).
                build());

        dataManager.registerItemSaveConsumer(Generator.class, (conn, collection) -> {
            final InsertStatement.Builder builder = InsertStatement.builder(TABLE);
            collection.forEach(item -> {
                builder.newEntry().
                        addEntry(ID, item.getId()).
                        addEntry(ITEM_ID, item.getItemId()).
                        addEntry(TICK_COUNTER, ((Generator) item).tickCounter).
                        build().
                        execute(conn);
            });
        });

        dataManager.registerItemLoadFunction(TABLE, (conn, id) -> {
            final SelectStatement.Builder builder = SelectStatement.builder(TABLE).
                    selectAll().
                    condition(ID, String.valueOf(id));
            final List<Generator> list = builder.build().execute(conn, results -> {
                final String itemId = results.getString(ITEM_ID);
                if (!(plugin.getItemManager().getItem(itemId) instanceof final Generator item)) return null;
                final int tickCounter = results.getInt(TICK_COUNTER);
                final Generator generator = new Generator(
                        plugin,
                        id,
                        itemId,
                        item.itemSupplier,
                        item.tickDelay,
                        item.resetBlock,
                        item.generatorBlock,
                        item.items,
                        item.ticksToBreak,
                        item.mineSpeeds,
                        item.collectionCondition
                );
                generator.tickCounter = tickCounter;
                generator.isGenerated = tickCounter >= item.tickDelay;
                return generator;
            });
            if (list.isEmpty()) return SpecialSkyItem.EMPTY;
            return list.get(0);
        });

        dataManager.registerItemDeleteConsumer(Generator.class, (conn, item) -> {
            DeleteStatement.builder(TABLE).
                    condition(ID, String.valueOf(item.getId())).
                    build().
                    execute(conn);
        });
    }

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
    private final CollectionCondition collectionCondition;

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
            MineSpeeds mineSpeeds,
            CollectionCondition collectionCondition
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
        this.collectionCondition = collectionCondition;
    }

    @Override
    public void onBreak(User user, BlockBreakEvent event) {
        if (event instanceof GeneratorBreakEvent) {
            event.setCancelled(true);
            return;
        }
        final Block block = event.getBlock();
        final WorldPosition position = WorldPosition.fromLocation(block.getLocation());
        final GeneratorBreakEvent generatorBreakEvent = new GeneratorBreakEvent(block, event.getPlayer());
        Bukkit.getPluginManager().callEvent(generatorBreakEvent);
        if (generatorBreakEvent.isCancelled()) return;
        if (event.getPlayer().isSneaking()) {
            this.plugin.getWorlds().removeBlock(WorldPosition.fromLocation(block.getLocation()));
            this.plugin.getItemManager().giveItem(user, this);
            block.setType(Material.AIR);
            this.running = false;
            this.plugin.getBlockBreakManager().cancel(position);
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
        if (!this.collectionCondition.isAllowed(user.getCollection())) {
            user.sendMessage("<red>You have not collected the required materials to be able to collect from this generator.");
            this.plugin.getBlockBreakManager().startMining(
                    p -> Integer.MAX_VALUE,
                    player,
                    position,
                    p -> Bukkit.getScheduler().runTask(
                            this.plugin,
                            () -> this.onBreak(user, new BlockBreakEvent(block, player))
                    )
            );
            return;
        }
        final Location location = block.getLocation().add(0, 1, 0);
        final Supplier<ItemStack> weight = this.items.getRandom();
        if (weight == null) return;
        location.getWorld().dropItem(location, weight.get());
        this.isGenerated = false;
        this.tickCounter = 0;
        block.setType(this.resetBlock);
        user.getCollection().addAmount(this.itemId, 1);
        final ItemStack brokeWith = event.getPlayer().getInventory().getItemInMainHand();
        if (brokeWith == null) return;
        if (!(this.plugin.getItemManager().getItem(brokeWith) instanceof DurableItem durableItem)) return;
        durableItem.takeDamage(brokeWith, 1);
    }

    @Override
    public void onDestroy(BlockDestroyEvent event) {
        event.setCancelled(true);
        final Block block = event.getBlock();
        block.setBlockData(event.getNewState());
        block.getWorld().dropItem(block.getLocation(), this.plugin.getItemManager().getItem(this));
        this.plugin.getWorlds().removeBlock(WorldPosition.fromLocation(block.getLocation()));
    }

    @Override
    public void onPlace(User user, BlockPlaceEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        final Block block = event.getBlock();
        this.plugin.getWorlds().addBlock(this, WorldPosition.fromLocation(block.getLocation()));
        block.setType(this.resetBlock);
    }

    @Override
    public void onClick(User user, PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getPlayer().isSneaking() && user instanceof BukkitUser bukkitUser) {
            this.showCollectionRequirements(bukkitUser);
            return;
        }
        if (!this.isGenerated) {
            user.sendMessage("<red>You must wait " + (this.getTimeLeft() / 20) + "seconds before you can mine this block");
        }
    }

    private void showCollectionRequirements(BukkitUser user) {
        final int rows = (int) Math.ceil(this.collectionCondition.getRequiredItems().size() / 5.0) + 2;
        final Gui gui = Gui.gui().title(Adventure.parse("<blue>Collection Requirements")).
                rows(rows).
                disableAllInteractions().
                create();
        gui.getFiller().fillBorder(new GuiItem(ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).amount(1).name(" ").build()));
        for (final var entry : this.collectionCondition.getRequiredItems().entrySet()) {
            final String itemType = entry.getKey();
            final SpecialSkyItem item = this.plugin.getItemManager().getItem(itemType);
            ItemBuilder itemBuilder = ItemBuilder.from(item.getItemStack());
            if (SpecialSkyItem.EMPTY == item) {
                itemBuilder = ItemBuilder.from(Material.valueOf(itemType));
            }
            itemBuilder.name("<!i>" + itemType);
            itemBuilder.amount(1);
            itemBuilder.lore("").
                    lore(user.getCollection().getAmount(itemType) + "/" + entry.getValue());
            gui.addItem(new GuiItem(itemBuilder.build()));
        }
        final Player player = user.getPlayer();
        if (player == null) return;
        gui.open(player);
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
        this.isGenerated = true;
        if (block.getType() == this.generatorBlock) return;
        this.tickCounter = 0;
        block.setType(this.generatorBlock);
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
        if (!this.collectionCondition.isAllowed(user.getCollection())) {
            player.sendActionBar(Adventure.parse("<red>You have not collected the required materials to be able to collect from this generator."));
            this.plugin.getBlockBreakManager().startMining(
                    p -> Integer.MAX_VALUE,
                    player,
                    WorldPosition.fromLocation(block.getLocation()),
                    p -> Bukkit.getScheduler().runTask(
                            this.plugin,
                            () -> this.onBreak(user, new BlockBreakEvent(block, player))
                    )
            );
            return;
        }
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
    public int getTickToMine() {
        return this.ticksToBreak;
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

    @Override
    public String getTableName() {
        return TABLE;
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
        private static final String COLLECTION_REQUIREMENTS = "collection-requirements";

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
                final CollectionCondition requirements = CollectionCondition.serializer().deserialize(CollectionCondition.class, node.node(COLLECTION_REQUIREMENTS));
                return () -> new Generator(
                        plugin,
                        plugin.getDataManager().generateNextId(),
                        itemId,
                        itemSupplier,
                        tickDelay,
                        resetBlock,
                        generatorBlock,
                        items,
                        ticksToBreak,
                        speeds,
                        requirements
                );
            } catch (SerializationException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void serialize(Type type, @Nullable Supplier<Generator> obj, ConfigurationNode node) throws SerializationException {

        }
    }
}
