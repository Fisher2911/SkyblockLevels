package io.github.fisher2911.skyblocklevels.database;

public enum KeyType {

    NONE,
    FOREIGN,
    UNIQUE,
    PRIMARY;

    String getString() {
        return switch (this) {
            case FOREIGN -> "FOREIGN KEY";
            case UNIQUE -> "UNIQUE";
            case PRIMARY -> "PRIMARY KEY";
            default -> "";
        };
    }
}
