package io.github.fisher2911.skyblocklevels.database;

public class DatabaseField {

    private final Class<?> clazz;
    private final String name;
    private final KeyType keyType;

    public DatabaseField(Class<?> clazz, String name, KeyType keyType) {
        this.clazz = clazz;
        this.name = name;
        this.keyType = keyType;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public String getName() {
        return "`" + name + "`";
    }

    public KeyType getKeyType() {
        return keyType;
    }
}
