package io.github.fisher2911.skyblocklevels.command;

import io.github.fisher2911.skyblocklevels.SkyblockLevels;

public class SpawnCommand extends SkyCommand {

    public SpawnCommand(SkyblockLevels plugin) {
        super(plugin);
    }

    @Override
    public void register() {
//        this.manager.command(
//                this.manager.commandBuilder("spawn").
//                        permission(Permission.SPAWN_TP).
//                        senderType(BukkitUser.class).
//                        handler(context -> {
//                            final BukkitUser user = (BukkitUser) context.getSender();
//                            this.plugin.getTeleportManager().startTask(
//                                    user,
//                                    WorldPosition.fromLocation(this.plugin.config().getSpawn()),
//                                    3
//                            );
//                        })
//        );
    }
}
