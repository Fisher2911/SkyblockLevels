package io.github.fisher2911.skyblocklevels.user;

public class CollectionPermission {

    private final String id;
    private final int amount;
    private final String permission;
    private final String category;

    public CollectionPermission(String id, int amount, String permission, String category) {
        this.id = id;
        this.amount = amount;
        this.permission = permission;
        this.category = category;
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

    public String getCategory() {
        return category;
    }
}
