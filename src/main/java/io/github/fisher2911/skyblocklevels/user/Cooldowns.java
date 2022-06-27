package io.github.fisher2911.skyblocklevels.user;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public class Cooldowns {

    private final Map<String, Cooldown> cooldowns;

    public Cooldowns(Map<String, Cooldown> cooldowns) {
        this.cooldowns = cooldowns;
    }

    public void addCooldown(String key, Duration duration) {
        cooldowns.put(key, new Cooldown(Instant.now(), duration));
    }

    public Duration getCooldownDuration(String key) {
        final Cooldown cooldown = this.cooldowns.get(key);
        if (cooldown == null) return Duration.ZERO;
        return cooldown.duration;
    }

    public Duration getTimePassed(String key) {
        final Cooldown cooldown = this.cooldowns.get(key);
        if (cooldown == null) return Duration.ZERO;
        return Duration.between(cooldown.start, Instant.now());
    }

    public boolean isOnCooldown(String key) {
        final Cooldown cooldown = cooldowns.get(key);
        if (cooldown == null) return false;
        final boolean expired = cooldown.isExpired();
        if (expired) this.cooldowns.remove(key);
        return !expired;
    }

    private class Cooldown {

        private final Instant start;
        private final Duration duration;

        public Cooldown(Instant start, Duration duration) {
            this.start = start;
            this.duration = duration;
        }

        public boolean isExpired() {
            return Instant.now().isAfter(this.start.plus(this.duration));
        }
    }

}
