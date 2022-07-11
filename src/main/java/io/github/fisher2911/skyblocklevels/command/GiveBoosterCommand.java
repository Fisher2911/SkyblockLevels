package io.github.fisher2911.skyblocklevels.command;

import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.standard.DoubleArgument;
import cloud.commandframework.arguments.standard.EnumArgument;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.bukkit.parsers.PlayerArgument;
import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.booster.Booster;
import io.github.fisher2911.skyblocklevels.booster.BoosterType;
import io.github.fisher2911.skyblocklevels.math.Operation;
import io.github.fisher2911.skyblocklevels.user.BukkitUser;
import io.github.fisher2911.skyblocklevels.user.User;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class GiveBoosterCommand extends SkyCommand {


    public GiveBoosterCommand(SkyblockLevels plugin) {
        super(plugin);
    }

    @Override
    public void register() {
        final CommandArgument.Builder<User, Operation> argument = this.manager.argumentBuilder(Operation.class, "operator").
                asRequired().withSuggestionsProvider(
                        (context, arg) -> Arrays.stream(Operation.values()).map(Operation::getSign).map(Object::toString).toList()).
        manager(this.manager).
                withParser((u, s) -> {
                    final String string = s.poll();
                    try {
                        if (string == null) throw new IllegalArgumentException();
                        final char c = string.charAt(0);
                        final Operation operation = Operation.bySign(c);
                        if (operation == null) throw new IllegalArgumentException();
                        return ArgumentParseResult.success(operation);
                    } catch (IllegalArgumentException e) {
                        return ArgumentParseResult.failure(new IllegalArgumentException("Invalid operator"));
                    }
                });

        this.manager.command(
                this.manager.commandBuilder("booster").
                        permission(Permission.GIVE_BOOSTER).
                        literal("give").
                        senderType(User.class).
                        argument(PlayerArgument.newBuilder("player")).
                        argument(EnumArgument.newBuilder(BoosterType.class, "type")).
                        argument(argument).
                        argument(DoubleArgument.newBuilder("value")).
                        argument(IntegerArgument.newBuilder("duration")).
                        handler(context -> {
                            final User sender = context.getSender();
                            final Player player = context.get("player");
                            final BoosterType type = context.get("type");
                            final double value = context.get("value");
                            final int duration = context.get("duration");
                            final Operation operation = context.get("operator");
                            final BukkitUser user = this.plugin.getUserManager().getUser(player);
                            if (user == null) {
                                sender.sendMessage("<red>Player not found");
                                return;
                            }
                            final Booster booster = new Booster(player.getUniqueId(), type, operation, value, duration);
                            user.getBoosters().addBooster(booster);
                            user.sendMessage("<blue>You have been given a " + type.toString().toLowerCase() + " " + value + operation.getSign() + " booster for " + duration + " seconds");
                        })
        );
    }

}
