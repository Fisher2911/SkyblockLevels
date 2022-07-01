package io.github.fisher2911.skyblocklevels.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateTableStatement {

    private final String tableName;
    private final List<DatabaseField> fields;
    private final Map<KeyType, List<String>> groupKeys;

    public CreateTableStatement(String tableName, List<DatabaseField> fields, Map<KeyType, List<String>> groupKeys) {
        this.tableName = tableName;
        this.fields = fields;
        this.groupKeys = groupKeys;
    }

    public String getTableName() {
        return this.tableName;
    }

    public List<DatabaseField> getFields() {
        return this.fields;
    }

    public void execute(Connection connection) {
        try (final PreparedStatement statement = connection.prepareStatement(this.getStatement())) {
            statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error with statement: " + this.getStatement());
//            e.printStackTrace();
        }
    }

    public String getStatement() {
        final StringBuilder builder = new StringBuilder("CREATE TABLE IF NOT EXISTS ").
                append(this.tableName).
                append("(");
        for (DatabaseField field : this.fields) {
            final String name = field.getName();
            final String fieldType = DatabaseClasses.getFieldName(field.getClazz());
            final String keyString = field.getKeyType().getString();
            builder.append(name).
                    append(" ").
                    append(fieldType).
                    append(" ").
                    append(keyString).
                    append(",");
        }
        builder.delete(builder.length() - 1, builder.length());

        if (!this.groupKeys.isEmpty()) builder.append(", ");

        for (var entry : this.groupKeys.entrySet()) {
            final KeyType keyType = entry.getKey();
            builder.append(" ").
                    append(keyType.getString()).
                    append(" (");
            final List<String> fields = entry.getValue();
            for (String field : fields) {
                builder.append(field).
                        append(",");
            }
            builder.delete(builder.length() - 1, builder.length());
            builder.append(")");
        }
        builder.append(")");

        return builder.toString();
    }

    public static Builder builder(String tableName) {
        return new Builder(tableName);
    }

    public static class Builder {

        private final String tableName;
        private final List<DatabaseField> fields = new ArrayList<>();
        private final Map<KeyType, List<String>> groupKeys = new HashMap<>();

        private Builder(String tableName) {
            this.tableName = tableName;
        }

        public Builder addField(Class<?> clazz, String name, KeyType keyType) {
            this.fields.add(new DatabaseField(clazz, name, keyType));
            return this;
        }

        public Builder addField(Class<?> clazz, String name) {
            return addField(clazz, name, KeyType.NONE);
        }

        public Builder groupKeys(KeyType key, String... fields) {
            this.groupKeys.put(key, List.of(fields));
            return this;
        }

        public CreateTableStatement build() {
            return new CreateTableStatement(this.tableName, this.fields, this.groupKeys);
        }
    }
}
