package io.github.fisher2911.skyblocklevels.user;

import io.github.fisher2911.skyblocklevels.util.Condition;

import java.util.Map;

public class CollectionCondition implements Condition<Collection> {

    private final Map<String, Integer> requiredItems;

    public CollectionCondition(Map<String, Integer> requiredItems) {
        this.requiredItems = requiredItems;
    }

    @Override
    public boolean isAllowed(Collection collection) {
        for (Map.Entry<String, Integer> entry : requiredItems.entrySet()) {
            if (!collection.hasAmount(entry.getKey(), entry.getValue())) {
                return false;
            }
        }
        return true;
    }
}
