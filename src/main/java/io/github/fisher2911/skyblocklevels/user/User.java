package io.github.fisher2911.skyblocklevels.user;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface User {

    ServerUser SERVER = new ServerUser();

    UUID getId();
    Collection getCollection();
    void sendMessage(String message);
    void sendMessage(Component message);
    void forceCommand(String command);
    @NotNull Audience getAudience();
    Cooldowns getCooldowns();
    String getName();

}
