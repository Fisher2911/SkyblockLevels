package io.github.fisher2911.skyblocklevels.command;

import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.item.SpecialSkyItem;
import io.github.fisher2911.skyblocklevels.user.BukkitUser;
import io.github.fisher2911.skyblocklevels.user.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public class GiveItemCommand extends SkyCommand {

    public GiveItemCommand(SkyblockLevels plugin) {
        super(plugin);
    }

    @Override
    public void register() {
        final CommandArgument<User, SpecialSkyItem> argument = this.manager.argumentBuilder(SpecialSkyItem.class, "item").
                asRequired().withSuggestionsProvider(
                        (context, arg) ->
                                this.plugin.getItemManager().getAllIds().
                                        stream().
                                        filter(id -> id.toLowerCase().startsWith(arg.toLowerCase())).
                                        toList()).
                manager(this.manager).
                withParser((u, s) -> {
                    if (s.isEmpty())
                        return ArgumentParseResult.failure(new NullPointerException("No item id specified"));
                    return ArgumentParseResult.success(this.plugin.getItemManager().createItem(s.poll()));
                }).build();

        final CommandArgument<User, BukkitUser> userArgument = this.manager.argumentBuilder(BukkitUser.class, "user").
                asRequired().
                manager(this.manager).
                asOptional().
                withParser((u, s) -> {
                    final String name = s.poll();
                    final Player player;
                    if (name == null) {
                        player = null;
                    } else {
                        player = Bukkit.getPlayer(name);
                    }
                    if (player == null)
                        return ArgumentParseResult.failure(new NullPointerException("No player specified"));
                    return ArgumentParseResult.success(this.plugin.getUserManager().getUser(player));
                }).build();

        this.manager.command(
                this.manager.commandBuilder("skyitem").
                        permission(Permission.GIVE_ITEM).
                        literal("give").
                        senderType(User.class).
                        argument(argument).
                        argument(userArgument).
                        handler(context -> {
                            final SpecialSkyItem skyItem = context.get("item");
                            User user = context.getSender();
                            if (context.contains("user")) user = context.get("user");
                            this.plugin.getItemManager().giveItem(user, skyItem);
                        })
        );
    }
}
