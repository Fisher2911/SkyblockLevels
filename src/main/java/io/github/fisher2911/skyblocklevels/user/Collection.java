package io.github.fisher2911.skyblocklevels.user;

import io.github.fisher2911.skyblocklevels.database.Changeable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Collection implements Changeable<Set<String>> {

    private final Map<String, Integer> itemsCollected;
    private Set<String> changed = new HashSet<>();

    public Collection(Map<String, Integer> itemsCollected) {
        this.itemsCollected = itemsCollected;
    }

    public boolean hasAmount(String id, int amount) {
        return this.itemsCollected.getOrDefault(id, 0) >= amount;
    }

    protected void addAmount(String id, int amount) {
        this.itemsCollected.put(id, this.itemsCollected.getOrDefault(id, 0) + amount);
        this.changed.add(id);
    }

    public int getAmount(String id) {
        return this.itemsCollected.getOrDefault(id, 0);
    }

    public static Collection empty() {
        return new Collection(Map.of());
    }

    public void set(String id, int amount) {
        this.itemsCollected.put(id, amount);
        this.changed.add(id);
    }

    public Map<String, Integer> getItemsCollected() {
        return itemsCollected;
    }

    @Override
    public Set<String> getChanged() {
        return this.changed;
    }

    @Override
    public void setChanged(Set<String> changed) {
        this.changed = changed;
    }
}
