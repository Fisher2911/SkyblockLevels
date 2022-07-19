package io.github.fisher2911.skyblocklevels.command;

import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.bukkit.parsers.PlayerArgument;
import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.user.BukkitUser;
import io.github.fisher2911.skyblocklevels.user.User;
import org.bukkit.entity.Player;

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

        this.manager.command(
                this.manager.commandBuilder("collection").
                        literal("set").
                        argument(StringArgument.of("type")).
                        argument(IntegerArgument.newBuilder("amount")).
                        argument(PlayerArgument.newBuilder("player")).
                        permission(Permission.SET_COLLECTION).
                        senderType(User.class).
                        handler(context -> this.manager.taskRecipe().begin(context).synchronous(c -> {
                            final Player player = context.get("player");
                            final User user = this.plugin.getUserManager().getUser(player);
                            if (user == null) return;
                            final int amount = context.get("amount");
                            final String type = context.get("type");
                            this.plugin.getUserManager().addCollectionAmount(user, type, amount);
                            context.getSender().sendMessage("<green>Successfully set collection amount for " + type + " to " + amount);
                        }).execute())
        );

        final CommandArgument<User, String> collectionArgument = this.manager.argumentBuilder(String.class, "collection").
                asRequired().
                manager(this.manager).
                asOptional().
                withParser((u, s) -> ArgumentParseResult.success(s.poll())).build();

        this.manager.command(
                this.manager.commandBuilder("collection").
                        literal("top").
                        argument(collectionArgument).
                        permission(Permission.VIEW_COLLECTION).
                        senderType(BukkitUser.class).
                        handler(context -> this.manager.taskRecipe().begin(context).synchronous(c -> {
                            final BukkitUser user = (BukkitUser) context.getSender();
                            if (!context.contains("collection")) {
                                this.plugin.getUserManager().getCollectionTop().openDefaultMenu(user);
                                return;
                            }
                            final String type = context.get("collection");
                            this.plugin.getUserManager().getCollectionTop().displayMenu(user, type);
                        }).execute())
        );
    }


}
