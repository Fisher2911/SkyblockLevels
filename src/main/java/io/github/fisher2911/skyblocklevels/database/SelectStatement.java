package io.github.fisher2911.skyblocklevels.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SelectStatement {

    private final String tableName;
    private final boolean selectAll;
    private final List<String> fields;
    private final List<EqualityCondition> conditions;
    private final int limit;
    private final DatabaseOrder order;

    public SelectStatement(String tableName, boolean selectAll, List<String> fields, List<EqualityCondition> conditions, int limit, DatabaseOrder order) {
        this.tableName = tableName;
        this.selectAll = selectAll || fields.isEmpty();
        this.fields = fields;
        this.conditions = conditions;
        this.limit = limit;
        this.order = order;
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
            for (EqualityCondition condition : this.conditions) {
                builder.append(condition.getCondition()).append(" AND ");
            }
            builder.delete(builder.length() - 5, builder.length());
        }
        if (this.order != null && !this.order.getFields().isEmpty()) {
            builder.append(" ORDER BY");
            for (String field : this.order.getFields()) {
                builder.append(" ").append(field).append(",");
            }
            builder.delete(builder.length() - 1, builder.length());
            builder.append(" ").append(this.order.getDirection());
        }
        if (this.limit > 0) {
            builder.append(" LIMIT ").append(this.limit);
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
        private final List<EqualityCondition> conditions = new ArrayList<>();
        private int limit = -1;
        private DatabaseOrder order;

        private Builder(String tableName) {
            this.tableName = tableName;
        }

        public Builder field(String field) {
            this.fields.add(field);
            return this;
        }

        public Builder whereEqual(EqualityCondition condition) {
            this.conditions.add(condition);
            return this;
        }

        public Builder whereEqual(String field, String expected) {
            return this.whereEqual(new EqualityCondition(field, expected));
        }

        public Builder whereEqual(String field, Object expected) {
            return this.whereEqual(field, String.valueOf(expected));
        }

        public Builder selectAll() {
            this.selectAll = true;
            return this;
        }

        public Builder limit(int limit) {
            this.limit = limit;
            return this;
        }

        public Builder orderBy(DatabaseOrder order) {
            this.order = order;
            return this;
        }

        private Builder orderBy(String operator, String... fields) {
            return this.orderBy(new DatabaseOrder(Arrays.asList(fields), operator));
        }

        public Builder orderAsc(String... fields) {
            return this.orderBy("ASC", fields);
        }

        public Builder orderDesc(String... fields) {
            return this.orderBy("DESC", fields);
        }

        public SelectStatement build() {
            return new SelectStatement(this.tableName, this.selectAll, this.fields, this.conditions, this.limit, this.order);
        }

    }
}
