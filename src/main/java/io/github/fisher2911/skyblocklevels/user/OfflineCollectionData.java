package io.github.fisher2911.skyblocklevels.user;

import java.util.UUID;

public class OfflineCollectionData {

    private final UUID uuid;
    private final Collection collection;

    public OfflineCollectionData(UUID uuid, Collection collection) {
        this.uuid = uuid;
        this.collection = collection;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Collection getCollection() {
        return collection;
    }
}
