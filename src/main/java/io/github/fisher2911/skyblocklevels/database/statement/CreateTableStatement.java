package io.github.fisher2911.skyblocklevels.database.statement;

import io.github.fisher2911.skyblocklevels.util.Pair;

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
    private final Map<String, Pair<String, String>> foreignKeys;
    private final Map<KeyType, List<String>> groupKeys;

    public CreateTableStatement(String tableName, List<DatabaseField> fields, Map<String, Pair<String, String>> foreignKeys, Map<KeyType, List<String>> groupKeys) {
        this.tableName = tableName;
        this.fields = fields;
        this.groupKeys = groupKeys;
        this.foreignKeys = foreignKeys;
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
            e.printStackTrace();
        }
    }

    public String getStatement() {
        final StringBuilder builder = new StringBuilder("CREATE TABLE IF NOT EXISTS ").
                append(this.tableName).
                append("(");
        for (DatabaseField field : this.fields) {
            final String name = field.getName();
            final String fieldType = DatabaseClasses.getFieldName(field.getDatabaseType());
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

        if (!this.foreignKeys.isEmpty()) {
            for (var entry : this.foreignKeys.entrySet()) {
                final String key = entry.getKey();
                final Pair<String, String> value = entry.getValue();
                final String tableName = value.getFirst();
                final String columnName = value.getSecond();
                builder.append(", FOREIGN KEY (").
                        append(key).
                        append(") REFERENCES ").
                        append(tableName).
                        append("(").
                        append(columnName).
                        append(")").
                        append(" ON DELETE CASCADE");
            }
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
        private final Map<String, Pair<String, String>> foreignKeys = new HashMap<>();
        private final Map<KeyType, List<String>> groupKeys = new HashMap<>();

        private Builder(String tableName) {
            this.tableName = tableName;
        }

        public Builder addField(Object databaseType, String name, KeyType keyType) {
            this.fields.add(new DatabaseField(databaseType, name, keyType));
            return this;
        }

        public Builder addField(Object databaseType, String name) {
            return addField(databaseType, name, KeyType.NONE);
        }

        public Builder foreignKey(String name, String tableName, String columnName) {
            this.foreignKeys.put(name, new Pair<>(tableName, columnName));
            return this;
        }

        public Builder groupKeys(KeyType key, String... fields) {
            this.groupKeys.put(key, List.of(fields));
            return this;
        }

        public CreateTableStatement build() {
            return new CreateTableStatement(this.tableName, this.fields, this.foreignKeys, this.groupKeys);
        }
    }
}
