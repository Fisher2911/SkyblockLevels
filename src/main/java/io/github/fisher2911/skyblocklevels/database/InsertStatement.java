package io.github.fisher2911.skyblocklevels.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InsertStatement {

    private final String tableName;
    private final List<String> fieldNames;
    private final Collection<Map<String, DatabaseEntry>> values;
    private final int batchSize;
    private final List<String> conditions;

    public InsertStatement(String tableName, List<String> fieldNames, Collection<Map<String, DatabaseEntry>> values, int batchSize, List<String> conditions) {
        this.tableName = tableName;
        this.fieldNames = fieldNames;
        this.values = values;
        this.batchSize = batchSize;
        this.conditions = conditions;
    }

    public String getTableName() {
        return tableName;
    }

    public List<String> getConditions() {
        return conditions;
    }

    public void execute(Connection connection) {
        try (final PreparedStatement statement = connection.prepareStatement(this.getStatement())) {
            if (this.batchSize <= 0) {
                for (Map<String, DatabaseEntry> value : this.values) {
                    this.setValues(statement, value);
                    statement.executeUpdate();
                }
                return;
            }
            int i = 0;
            for (Map<String, DatabaseEntry> value : this.values) {
                this.setValues(statement, value);
                statement.addBatch();
                if (i % this.batchSize == 0) {
                    statement.executeBatch();
                }
                i++;
            }
            statement.executeBatch();
        } catch (SQLException e) {
            System.out.println("Error with statement: " + this.getStatement());
            e.printStackTrace();
        }
    }

    private void setValues(PreparedStatement statement, Map<String, DatabaseEntry> values) throws SQLException {
        for (var entry : values.values()) {
            statement.setObject(entry.getIndex(), entry.getValue());
        }
    }

    public String getStatement() {
        final StringBuilder builder = new StringBuilder("INSERT OR REPLACE INTO ").
                append(tableName).
                append(" (");
        for (String field : this.fieldNames) {
            builder.append(field).
                    append(",");
        }
        builder.delete(builder.length() - 1, builder.length());
        builder.append(") VALUES (");
        builder.append("?, ".repeat(this.fieldNames.size()));
        builder.delete(builder.length() - 2, builder.length());
        builder.append(") ");
        if (this.conditions.isEmpty()) return builder.toString();
        builder.append("WHERE ");
        for (String condition : this.conditions) {
            builder.
                    append(condition).
                    append(" AND ");
        }
        builder.delete(builder.length() - 5, builder.length());
        return builder.toString();
    }

    public static Builder builder(String tableName) {
        return new Builder(tableName);
    }

    private static class DatabaseEntry {

        private final int index;
        private final Object value;

        public DatabaseEntry(int index, Object value) {
            this.index = index;
            this.value = value;
        }

        public int getIndex() {
            return index;
        }

        public Object getValue() {
            return value;
        }
    }

    public static class Builder {

        private final String tableName;
        private final List<String> fieldNames;
        private final Collection<Map<String, DatabaseEntry>> values;
        private Map<String, DatabaseEntry> currentEntries = new HashMap<>();
        private int batchSize;
        private final List<String> conditions;

        private Builder(String tableName) {
            this.tableName = tableName;
            this.fieldNames = new ArrayList<>();
            this.values = new ArrayList<>();
            this.conditions = new ArrayList<>();
        }

        public Builder addEntry(String field, Object value) {
            if (!this.fieldNames.contains(field)) this.fieldNames.add(field);
            currentEntries.put(field, new DatabaseEntry(this.fieldNames.size(), value));
            return this;
        }

        public Builder newEntry() {
            if (!this.currentEntries.isEmpty()) this.values.add(this.currentEntries);
            this.currentEntries = new HashMap<>();
            return this;
        }

        public Builder condition(DatabaseCondition condition) {
            this.conditions.add(condition.getCondition());
            return this;
        }

        public Builder condition(String field, String expected) {
            return this.condition(new DatabaseCondition(field, expected));
        }

        public Builder batchSize(int batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        public InsertStatement build() {
            if (!this.currentEntries.isEmpty()) this.values.add(this.currentEntries);
            return new InsertStatement(this.tableName, this.fieldNames, this.values, this.batchSize, this.conditions);
        }

    }
}
