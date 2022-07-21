package io.github.fisher2911.skyblocklevels.database.statement;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class DatabaseClasses {

    private static final Map<Object, Function<Object, String>> FIELD_NAMES = new HashMap<>();

    static {
        FIELD_NAMES.put(Integer.class, o -> "int");
        FIELD_NAMES.put(Long.class, o -> "bigint");
        FIELD_NAMES.put(Double.class, o -> "double");
        FIELD_NAMES.put(Float.class, o -> "float");
        FIELD_NAMES.put(Boolean.class, o -> "boolean");
        FIELD_NAMES.put(String.class, o -> "varchar(255)");
        FIELD_NAMES.put(Instant.class, o -> "timestamp");
        FIELD_NAMES.put(VarChar.class, o -> {
            if (o == null) return FIELD_NAMES.get(String.class).apply(null);
            return "varchar(" + ((VarChar) o).getLength() + ")";
        });
    }

    public static String getFieldName(Class<?> clazz) {
        final Function<Object, String> function = FIELD_NAMES.get(clazz);
        if (function == null) {
            throw new IllegalArgumentException("No field name for class " + clazz.getName());
        }
        return function.apply(null);
    }

    public static String getFieldName(Object obj) {
        if (obj instanceof Class<?> clazz) return getFieldName(clazz);
        final Function<Object, String> function = FIELD_NAMES.get(obj.getClass());
        if (function == null) {
            throw new IllegalArgumentException("No field name for class " + obj.getClass().getName());
        }
        return function.apply(obj);
    }

}
