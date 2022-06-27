package io.github.fisher2911.skyblocklevels.command;

import cloud.commandframework.annotations.AnnotationParser;
import cloud.commandframework.minecraft.extras.MinecraftHelp;
import cloud.commandframework.paper.PaperCommandManager;
import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.user.User;

public abstract class SkyCommand {

    protected final SkyblockLevels plugin;
    protected final PaperCommandManager<User> manager;
    protected final MinecraftHelp<User> minecraftHelp;
    protected final AnnotationParser<User> annotationParser;

    public SkyCommand(SkyblockLevels plugin) {
        this.plugin = plugin;
        this.manager = this.plugin.getCommandManager();
        this.minecraftHelp = this.plugin.getMinecraftHelp();
        this.annotationParser = this.plugin.getAnnotationParser();
    }

    public abstract void register();
}
