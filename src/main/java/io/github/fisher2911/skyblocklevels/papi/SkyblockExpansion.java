package io.github.fisher2911.skyblocklevels.papi;

import io.github.fisher2911.skyblocklevels.SkyblockLevels;
import io.github.fisher2911.skyblocklevels.booster.BoosterType;
import io.github.fisher2911.skyblocklevels.user.BukkitUser;
import io.github.fisher2911.skyblocklevels.user.UserManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

import java.text.DecimalFormat;

public class SkyblockExpansion extends PlaceholderExpansion {

    private final SkyblockLevels plugin;
    private final UserManager userManager;

    public SkyblockExpansion(SkyblockLevels plugin) {
        this.plugin = plugin;
        this.userManager = this.plugin.getUserManager();
    }

    @Override
    public String getAuthor() {
        return "Fisher2911";
    }

    @Override
    public String getIdentifier() {
        return "skyblocklevels";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true; // This is required or else PlaceholderAPI will unregister the Expansion on reload
    }

    private static final String COLLECTION = "collection";
    private static final String BOOSTER = "booster";
    private static final String TIME = "time";
    private static final String VALUE = "value";

    private static final DecimalFormat FORMATTER = new DecimalFormat("#.##");

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        final String[] parts = params.split("_");
        if (parts.length == 0) return null;
        final String arg = parts[0];
        final BukkitUser user;
        if (player != null) {
            user = this.userManager.getUser(player.getUniqueId());
        } else {
            user = null;
        }
        if (arg.equalsIgnoreCase(COLLECTION) && user != null) {
            final String type = parts[1];
            return String.valueOf(user.getCollection().getAmount(type));
        }
        if (arg.equalsIgnoreCase(BOOSTER) && user != null) {
            if (parts.length < 3) return null;
            final String type = parts[1];
            final String param = parts[2];
            try {
                final BoosterType boosterType = BoosterType.valueOf(type.toUpperCase());
                if (param.equalsIgnoreCase(VALUE)) {
                    return FORMATTER.format(user.getBoosters().getBoosterValue(boosterType));
                }
                if (param.equalsIgnoreCase(TIME)) {
                    return String.valueOf(user.getBoosters().getBoosterTimeLeft(boosterType));
                }
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null; // Placeholder is unknown by the Expansion
    }
}
