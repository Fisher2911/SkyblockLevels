package io.github.fisher2911.skyblocklevels.database;

import java.util.List;

public class DatabaseOrder {

    private final List<String> fields;
    private final String direction;

    public DatabaseOrder(List<String> fields, String direction) {
        this.fields = fields;
        this.direction = direction;
    }

    public List<String> getFields() {
        return fields;
    }

    public String getDirection() {
        return direction;
    }
}
