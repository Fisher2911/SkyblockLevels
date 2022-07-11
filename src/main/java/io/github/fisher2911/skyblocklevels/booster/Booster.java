package io.github.fisher2911.skyblocklevels.booster;

import io.github.fisher2911.skyblocklevels.math.Operation;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class Booster {

    private final UUID owner;;
    private final BoosterType boosterType;
    private final Operation operation;
    private final double value;
    private Instant startTime;
    private final int seconds;

    public Booster(UUID owner, BoosterType boosterType, Operation operation, double value, int seconds) {
        this.owner = owner;
        this.boosterType = boosterType;
        this.operation = operation;
        this.value = value;
        this.seconds = seconds;
    }

    public Booster(UUID owner, BoosterType boosterType, Operation operation, double value, int seconds, Instant startTime) {
        this.owner = owner;
        this.boosterType = boosterType;
        this.operation = operation;
        this.value = value;
        this.seconds = seconds;
        this.startTime = startTime;
    }


    public UUID getOwner() {
        return owner;
    }

    public BoosterType getBoosterType() {
        return this.boosterType;
    }

    public Operation getOperation() {
        return this.operation;
    }

    public double getValue() {
        return this.value;
    }

    @Nullable
    public Instant getStartTime() {
        return startTime;
    }

    public void start() {
        this.startTime = Instant.now();
    }

    public int getSeconds() {
        return seconds;
    }

    public int getTimeLeft() {
        return (int) (this.getSeconds() - this.getStartTime().until(Instant.now(), ChronoUnit.SECONDS));
    }

    public double apply(double value) {
        return operation.handle(value, this.value);
    }

    public boolean isStarted() {
        return this.startTime != null;
    }

    public boolean isExpired() {
        if (this.startTime == null) return false;
        return Instant.now().isAfter(this.startTime.plusSeconds(this.seconds));
    }
}
