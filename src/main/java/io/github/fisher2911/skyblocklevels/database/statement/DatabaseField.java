package io.github.fisher2911.skyblocklevels.database.statement;

public class DatabaseField {

    private final Object databaseType;
    private final String name;
    private final KeyType keyType;

    public DatabaseField(Object databaseType, String name, KeyType keyType) {
        this.databaseType = databaseType;
        this.name = name;
        this.keyType = keyType;
    }

    public Object getDatabaseType() {
        return databaseType;
    }

    public String getName() {
        return "`" + name + "`";
    }

    public KeyType getKeyType() {
        return keyType;
    }
}
