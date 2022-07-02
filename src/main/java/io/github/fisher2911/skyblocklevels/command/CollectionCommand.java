package io.github.fisher2911.skyblocklevels.command;

import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.user.BukkitUser;

public class CollectionCommand extends SkyCommand {

    public CollectionCommand(SkyblockLevels plugin) {
        super(plugin);
    }

    @Override
    public void register() {
        this.manager.command(
                this.manager.commandBuilder("collection").
                        permission(Permission.VIEW_COLLECTION).
                        senderType(BukkitUser.class).
                        handler(context -> this.manager.taskRecipe().begin(context).synchronous(c -> {
                            final BukkitUser user = (BukkitUser) context.getSender();
                            this.plugin.getUserManager().showMenu(user);
                        }).execute())
        );
    }


}
