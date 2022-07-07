package io.github.fisher2911.skyblocklevels.teleport;

import io.github.fisher2911.skyblocklevels.user.BukkitUser;
import io.github.fisher2911.skyblocklevels.world.WorldPosition;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class TeleportData {

    private final BukkitUser user;
    private final WorldPosition startPosition;
    private final WorldPosition endPosition;
    private final int seconds;
    @Nullable
    private final Consumer<TeleportData> onSuccess;
    private int currentSeconds;
    private boolean cancelled;

    public TeleportData(BukkitUser user, WorldPosition startPosition, WorldPosition endPosition, int seconds, @Nullable Consumer<TeleportData> onSuccess) {
        this.user = user;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.seconds = seconds;
        this.currentSeconds = seconds;
        this.onSuccess = onSuccess;
    }

    public BukkitUser getUser() {
        return user;
    }

    public WorldPosition getStartPosition() {
        return startPosition;
    }

    public WorldPosition getEndPosition() {
        return endPosition;
    }

    public int getSeconds() {
        return seconds;
    }

    @Nullable
    public Consumer<TeleportData> getOnSuccess() {
        return onSuccess;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public int getCurrentSeconds() {
        return currentSeconds;
    }

    public void decSecond() {
        currentSeconds--;
    }
}
