package io.github.fisher2911.skyblocklevels.command;

import io.github.fisher2911.skyblocklevels.SkyblockLevels;

public class ReloadCommand extends SkyCommand {

    public ReloadCommand(SkyblockLevels plugin) {
        super(plugin);
    }

    @Override
    public void register() {
        this.manager.command(
                this.manager.commandBuilder("skyblock").
                        permission(Permission.RELOAD).
                        literal("reload").
                        handler(context -> {
                            this.plugin.getItemManager().reload();
                            this.plugin.getUserManager().load(this.plugin);
                            context.getSender().sendMessage("<green>Reloaded successfully");
                        })
        );
    }
}
