package io.github.fisher2911.skyblocklevels.user;

import java.util.Map;

public class Collection {

    private final Map<String, Integer> itemsCollected;

    public Collection(Map<String, Integer> itemsCollected) {
        this.itemsCollected = itemsCollected;
    }

    public boolean hasAmount(String id, int amount) {
        return this.itemsCollected.getOrDefault(id, 0) >= amount;
    }

    public void addAmount(String id, int amount) {
        this.itemsCollected.put(id, this.itemsCollected.getOrDefault(id, 0) + amount);
    }

    public int getAmount(String id) {
        return this.itemsCollected.getOrDefault(id, 0);
    }

}
