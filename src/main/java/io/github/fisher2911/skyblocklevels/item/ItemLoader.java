package io.github.fisher2911.skyblocklevels.item;

import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.item.impl.DurableItem;
import io.github.fisher2911.skyblocklevels.item.impl.SkyItem;
import io.github.fisher2911.skyblocklevels.item.impl.bridger.LimitedBridger;
import io.github.fisher2911.skyblocklevels.item.impl.builder.Builder;
import io.github.fisher2911.skyblocklevels.item.impl.catcher.ItemCatcher;
import io.github.fisher2911.skyblocklevels.item.impl.crop.SkyCrop;
import io.github.fisher2911.skyblocklevels.item.impl.generator.Generator;
import io.github.fisher2911.skyblocklevels.item.impl.grower.Grower;
import io.github.fisher2911.skyblocklevels.item.impl.platformer.MaterialPlatformer;
import io.github.fisher2911.skyblocklevels.item.impl.spawner.MobSpawner;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class ItemLoader {

    private static final Path ITEM_FOLDER_PATH = SkyblockLevels.getPlugin(SkyblockLevels.class).getDataFolder().toPath().resolve(Path.of("items"));
    private static final Map<String, Function<ConfigurationNode, Supplier<? extends SpecialSkyItem>>> suppliers = new HashMap<>();

    public static void register(String id, Function<ConfigurationNode, Supplier<? extends SpecialSkyItem>> supplier) {
        suppliers.put(id, supplier);
        SkyblockLevels.getPlugin(SkyblockLevels.class).getLogger().info("Registered item loader " + id);
    }

    static {
        register(LimitedBridger.class.getSimpleName().toLowerCase(), node -> LimitedBridger.serializer().deserialize(LimitedBridger.class, node));
        register(Builder.class.getSimpleName().toLowerCase(), node -> Builder.serializer().deserialize(Builder.class, node));
        register(ItemCatcher.class.getSimpleName().toLowerCase(), node -> ItemCatcher.serializer().deserialize(ItemCatcher.class, node));
        register(Generator.class.getSimpleName().toLowerCase(), node -> Generator.serializer().deserialize(Generator.class, node));
        register(Grower.class.getSimpleName().toLowerCase(), node -> Grower.serializer().deserialize(Grower.class, node));
        register(MaterialPlatformer.class.getSimpleName().toLowerCase(), node -> MaterialPlatformer.serializer().deserialize(MaterialPlatformer.class, node));
        register(SkyItem.class.getSimpleName().toLowerCase(), node -> SkyItem.serializer().deserialize(SkyItem.class, node));
        register(DurableItem.class.getSimpleName().toLowerCase(), node -> DurableItem.serializer().deserialize(DurableItem.class, node));
        register(MobSpawner.class.getSimpleName().toLowerCase(), node -> MobSpawner.serializer().deserialize(MobSpawner.class, node));
        register(SkyCrop.class.getSimpleName().toLowerCase(), node -> SkyCrop.serializer().deserialize(SkyCrop.class, node));
    }

    private static final String CLASS = "class";
    private static final String ITEM_ID = "item-id";

    public static void load() {
        final SkyblockLevels plugin = SkyblockLevels.getPlugin(SkyblockLevels.class);
        try {
            final File folder = ITEM_FOLDER_PATH.toFile();
            if (!folder.exists()) {
                folder.getParentFile().mkdirs();
                if (!folder.exists()) folder.mkdirs();
            }
            final File itemFile = ITEM_FOLDER_PATH.resolve("items.yml").toFile();
            if (!itemFile.exists()) {
                plugin.saveResource(Path.of("items", "items.yml").toString(), false);
            }
            final File[] files = folder.listFiles();
            if (files == null) return;
            for (File file : files) {
               load(plugin, file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void load(SkyblockLevels plugin, File file) throws IOException {
        if (!file.exists()) {
            plugin.saveResource(Path.of("items", file.getName()).toString(), false);
        }
        final YamlConfigurationLoader loader = YamlConfigurationLoader.
                builder().
                path(file.toPath()).
                build();
        final ConfigurationNode source = loader.load();
        final var children = source.childrenMap();
        children.values().forEach(node -> {
            final String type = node.node(CLASS).getString("").replace("-", "");
            final Function<ConfigurationNode, Supplier<? extends SpecialSkyItem>> function = suppliers.get(type);
            if (function == null) {
                plugin.getLogger().severe("Could not load class: " + type + " from " + node.key());
                return;
            }
            final Supplier<? extends SpecialSkyItem> supplier = function.apply(node);
            if (supplier == null) return;
            final String id = node.node(ITEM_ID).getString("");
            plugin.getLogger().info("Loaded item " + id);
            plugin.getItemManager().register(id, supplier);
        });
    }

}
