package io.github.fisher2911.skyblocklevels.database;

import java.util.HashMap;
import java.util.Map;

public class DatabaseClasses {

    private static final Map<Class<?>, String> FIELD_NAMES = new HashMap<>();

    static {
        FIELD_NAMES.put(Integer.class, "int");
        FIELD_NAMES.put(Long.class, "bigint");
        FIELD_NAMES.put(Double.class, "double");
        FIELD_NAMES.put(Float.class, "float");
        FIELD_NAMES.put(Boolean.class, "boolean");
        FIELD_NAMES.put(String.class, "varchar(255)");
    }

    public static String getFieldName(Class<?> clazz) {
        return FIELD_NAMES.get(clazz);
    }

}
