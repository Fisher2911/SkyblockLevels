package io.github.fisher2911.skyblocklevels.command;

import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.item.SpecialSkyItem;
import io.github.fisher2911.skyblocklevels.user.BukkitUser;
import io.github.fisher2911.skyblocklevels.user.User;

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

        this.manager.command(
                this.manager.commandBuilder("skyitem").
                        permission(Permission.GIVE_ITEM).
                        literal("give").
                        senderType(BukkitUser.class).
                        argument(argument).
                        handler(context -> {
                            final SpecialSkyItem skyItem = context.get("item");
                            this.plugin.getItemManager().giveItem(context.getSender(), skyItem);
                            context.getSender().sendMessage("Gave item");
                        })
        );
    }
}
