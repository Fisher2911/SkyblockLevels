package io.github.fisher2911.skyblocklevels.item;

import io.github.fisher2911.skyblocklevels.user.User;
import io.github.fisher2911.skyblocklevels.util.Condition;

public class LockedItem {

    private final Condition<User> condition;
    private final Usable lockedItem;

    public LockedItem(Condition<User> condition, Usable lockedItem) {
        this.condition = condition;
        this.lockedItem = lockedItem;
    }

    public Condition<User> getCondition() {
        return this.condition;
    }

    public Usable getLockedItem() {
        return this.lockedItem;
    }

    public boolean isAllowed(User user) {
        return this.condition.isAllowed(user);
    }
}
