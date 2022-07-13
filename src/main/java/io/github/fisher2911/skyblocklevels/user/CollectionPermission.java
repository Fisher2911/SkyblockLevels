package io.github.fisher2911.skyblocklevels.user;

public class CollectionPermission {

    private final String id;
    private final int amount;
    private final String permission;

    public CollectionPermission(String id, int amount, String permission) {
        this.id = id;
        this.amount = amount;
        this.permission = permission;
    }

    public String getId() {
        return id;
    }

    public int getAmount() {
        return amount;
    }

    public String getPermission() {
        return permission;
    }
}
