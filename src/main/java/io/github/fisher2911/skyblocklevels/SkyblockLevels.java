package io.github.fisher2911.skyblocklevels;

import cloud.commandframework.CommandTree;
import cloud.commandframework.annotations.AnnotationParser;
import cloud.commandframework.arguments.parser.ParserParameters;
import cloud.commandframework.arguments.parser.StandardParameters;
import cloud.commandframework.bukkit.CloudBukkitCapabilities;
import cloud.commandframework.execution.AsynchronousCommandExecutionCoordinator;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.extra.confirmation.CommandConfirmationManager;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.minecraft.extras.MinecraftHelp;
import cloud.commandframework.paper.PaperCommandManager;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import io.github.fisher2911.skyblocklevels.command.CollectionCommand;
import io.github.fisher2911.skyblocklevels.command.GiveItemCommand;
import io.github.fisher2911.skyblocklevels.command.RTPCommand;
import io.github.fisher2911.skyblocklevels.command.ReloadCommand;
import io.github.fisher2911.skyblocklevels.database.DataManager;
import io.github.fisher2911.skyblocklevels.entity.EntityManager;
import io.github.fisher2911.skyblocklevels.item.ItemManager;
import io.github.fisher2911.skyblocklevels.listener.BlockBreakListener;
import io.github.fisher2911.skyblocklevels.listener.BlockPlaceListener;
import io.github.fisher2911.skyblocklevels.listener.BlockPowerListener;
import io.github.fisher2911.skyblocklevels.listener.EntityListener;
import io.github.fisher2911.skyblocklevels.listener.PlayerInteractListener;
import io.github.fisher2911.skyblocklevels.listener.PlayerJoinListener;
import io.github.fisher2911.skyblocklevels.packet.PacketHelper;
import io.github.fisher2911.skyblocklevels.user.BukkitUser;
import io.github.fisher2911.skyblocklevels.user.User;
import io.github.fisher2911.skyblocklevels.user.UserManager;
import io.github.fisher2911.skyblocklevels.world.BlockBreakManager;
import io.github.fisher2911.skyblocklevels.world.SkyWorld;
import io.github.fisher2911.skyblocklevels.world.Worlds;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import me.angeschossen.lands.api.integration.LandsIntegration;
import me.wolfyscript.customcrafting.CustomCrafting;
import me.wolfyscript.customcrafting.registry.RegistryRecipes;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public final class SkyblockLevels extends JavaPlugin {

    private DataManager dataManager;
    private SkyWorld world;
    private Worlds worlds;
    private BlockBreakManager blockBreakManager;
    private ItemManager itemManager;
    private EntityManager entityManager;
    private UserManager userManager;
    private PaperCommandManager<User> commandManager;
    private CommandConfirmationManager<User> confirmationManager;
    private MinecraftHelp<User> minecraftHelp;
    private AnnotationParser<User> annotationParser;
    private LandsIntegration lands;

    @Override
    public void onLoad() {
        final PacketEventsAPI<Plugin> api = SpigotPacketEventsBuilder.build(this);
        PacketEvents.setAPI(api);
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        PacketEvents.getAPI().init();
        this.dataManager = new DataManager(this);
        this.lands = new LandsIntegration(this);
        this.world = new SkyWorld();
        this.blockBreakManager = new BlockBreakManager(this);
        Bukkit.getScheduler().runTaskLater(this, this.world::createWorld, 1);
        this.worlds = new Worlds(this, new HashMap<>());
        this.itemManager = new ItemManager(this, new HashMap<>(), new HashMap<>());
        this.entityManager = new EntityManager(this, new ConcurrentHashMap<>(), new HashMap<>());
        this.itemManager.registerAll();
        this.entityManager.loadTypes();
        this.userManager = new UserManager(this, new HashMap<>(), new HashSet<>());
        this.userManager.load(this);
        this.registerListeners();
        this.initCommands();
        PacketHelper.registerListeners(this);
        this.dataManager.createTables();
    }

    private void initCommands() {
        final Function<CommandTree<User>, CommandExecutionCoordinator<User>> executionCoordinatorFunction =
                AsynchronousCommandExecutionCoordinator.<User>newBuilder().build();
        try {
            this.commandManager = new PaperCommandManager<>(
                    this,
                    executionCoordinatorFunction,
                    u -> {
                        if (u instanceof Player p) return this.userManager.getUser(p);
                        return User.SERVER;
                    },
                    u -> (u instanceof BukkitUser) ? ((BukkitUser) u).getPlayer() : Bukkit.getConsoleSender());
            this.commandManager.registerAsynchronousCompletions();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (this.commandManager.hasCapability(CloudBukkitCapabilities.BRIGADIER)) {
            this.commandManager.registerBrigadier();
        }

        this.confirmationManager = new CommandConfirmationManager<>(
                /* Timeout */ 30L,
                /* Timeout unit */ TimeUnit.SECONDS,
                /* Action when confirmation is required */ context -> context.getCommandContext().getSender().sendMessage(
                ChatColor.RED + "Confirmation required. Confirm using /example confirm."),
                /* Action when no confirmation is pending */ sender -> sender.sendMessage(
                ChatColor.RED + "You don't have any pending commands.")
        );

        this.confirmationManager.registerConfirmationProcessor(this.commandManager);

        final Function<ParserParameters, CommandMeta> commandMetaFunction = p ->
                CommandMeta.simple()
                        // This will allow you to decorate commands with descriptions
                        .with(CommandMeta.DESCRIPTION, p.get(StandardParameters.DESCRIPTION, "No description"))
                        .build();

        this.minecraftHelp = new MinecraftHelp<>(
                "/example help",
                User::getAudience,
                this.commandManager
        );

        this.annotationParser = new AnnotationParser<>(
                /* Manager */ this.commandManager,
                /* Command sender type */ User.class,
                /* Mapper for command meta instances */ commandMetaFunction
        );

        this.registerCommands();
    }

    private void registerCommands() {
        new GiveItemCommand(this).register();
        final RTPCommand rtpCommand = new RTPCommand(this);
        rtpCommand.register();
        new ReloadCommand(this).register();
        new CollectionCommand(this).register();
        this.registerListener(rtpCommand);
        try {
            this.annotationParser.parseContainers();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
    }

    private void registerListeners() {
        List.of(
                new PlayerJoinListener(this.userManager),
                new PlayerInteractListener(this),
                new BlockBreakListener(this),
                new BlockPlaceListener(this),
                new BlockPowerListener(this),
                new EntityListener(this.entityManager),
                this.worlds,
                this.blockBreakManager,
                this.entityManager
        ).forEach(this::registerListener);
    }

    public void registerListener(Listener listener) {
        this.getServer().getPluginManager().registerEvents(listener, this);
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public LandsIntegration getLands() {
        return lands;
    }

    public SkyWorld getWorld() {
        return world;
    }

    public ItemManager getItemManager() {
        return this.itemManager;
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public UserManager getUserManager() {
        return this.userManager;
    }

    public Worlds getWorlds() {
        return this.worlds;
    }

    public BlockBreakManager getBlockBreakManager() {
        return blockBreakManager;
    }

    public PaperCommandManager<User> getCommandManager() {
        return commandManager;
    }

    public CommandConfirmationManager<User> getConfirmationManager() {
        return confirmationManager;
    }

    public MinecraftHelp<User> getMinecraftHelp() {
        return minecraftHelp;
    }

    public AnnotationParser<User> getAnnotationParser() {
        return annotationParser;
    }

    public RegistryRecipes getRecipes() {
        return CustomCrafting.inst().getRegistries().getRecipes();
    }
}
