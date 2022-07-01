package io.github.fisher2911.skyblocklevels.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SelectStatement {

    private final String tableName;
    private final boolean selectAll;
    private final List<String> fields;
    private final List<DatabaseCondition> conditions;

    public SelectStatement(String tableName, boolean selectAll, List<String> fields, List<DatabaseCondition> conditions) {
        this.tableName = tableName;
        this.selectAll = selectAll || fields.isEmpty();
        this.fields = fields;
        this.conditions = conditions;
    }

    public <T> List<T> execute(Connection connection, DatabaseMapper<T> mapper) {
        try (final PreparedStatement statement = connection.prepareStatement(this.getStatement())) {
            final List<T> results = new ArrayList<>();
            final ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                final T object = mapper.map(resultSet);
                if (object == null) continue;
                results.add(object);
            }
            return results;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public String getStatement() {
        final StringBuilder builder = new StringBuilder("SELECT ");
        if (this.selectAll) {
            builder.append("*");
        } else {
            for (String field : this.fields) {
                builder.append(field).append(",");
            }
            builder.delete(builder.length() - 1, builder.length());
        }
        builder.append(" FROM ").append(this.tableName);
        if (!this.conditions.isEmpty()) {
            builder.append(" WHERE ");
            for (DatabaseCondition condition : this.conditions) {
                builder.append(condition.getCondition()).append(" AND ");
            }
            builder.delete(builder.length() - 5, builder.length());
        }
        return builder.toString();
    }

    public static Builder builder(String tableName) {
        return new Builder(tableName);
    }

    public static class Builder {

        private final String tableName;
        private boolean selectAll;
        private final List<String> fields = new ArrayList<>();
        private final List<DatabaseCondition> conditions = new ArrayList<>();

        private Builder(String tableName) {
            this.tableName = tableName;
        }

        public Builder field(String field) {
            this.fields.add(field);
            return this;
        }

        public Builder condition(DatabaseCondition condition) {
            this.conditions.add(condition);
            return this;
        }

        public Builder condition(String field, String expected) {
            return this.condition(new DatabaseCondition(field, expected));
        }

        public Builder condition(String field, Object expected) {
            return this.condition(field, String.valueOf(expected));
        }

        public Builder selectAll() {
            this.selectAll = true;
            return this;
        }

        public SelectStatement build() {
            return new SelectStatement(this.tableName, this.selectAll, this.fields, this.conditions);
        }

    }
}
