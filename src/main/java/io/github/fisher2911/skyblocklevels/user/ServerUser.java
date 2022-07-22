package io.github.fisher2911.skyblocklevels.user;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.UUID;

public class ServerUser implements User {

    public static final UUID ID = UUID.randomUUID();

    private final Cooldowns cooldowns = new Cooldowns(new HashMap<>());

    ServerUser() {}

    @Override
    public UUID getId() {
        return ID;
    }

    @Override
    public Collection getCollection() {
        return Collection.empty();
    }

    @Override
    public void sendMessage(String message) {
        Bukkit.getServer().getConsoleSender().sendMessage(message);
    }

    @Override
    public void sendMessage(Component message) {
        Bukkit.getServer().getConsoleSender().sendMessage(message);
    }

    @Override
    public void forceCommand(String command) {
        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    @Override
    @NotNull
    public Audience getAudience() {
        return Bukkit.getConsoleSender();
    }

    @Override
    public Cooldowns getCooldowns() {
        return this.cooldowns;
    }

    @Override
    public String getName() {
        return "Server";
    }
}
