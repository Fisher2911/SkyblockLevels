package io.github.fisher2911.skyblocklevels.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DeleteStatement {

    private final String tableName;
    private final List<DatabaseCondition> conditions;

    public DeleteStatement(String tableName, List<DatabaseCondition> conditions) {
        this.tableName = tableName;
        this.conditions = conditions;
    }

    public String getTableName() {
        return tableName;
    }

    public List<DatabaseCondition> getConditions() {
        return conditions;
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
        final StringBuilder builder = new StringBuilder("DELETE FROM ");
        builder.append(this.tableName);
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
        private final List<DatabaseCondition> conditions;

        private Builder(String tableName) {
            this.tableName = tableName;
            this.conditions = new ArrayList<>();
        }

        public Builder condition(DatabaseCondition condition) {
            this.conditions.add(condition);
            return this;
        }

        public Builder condition(String field, String expected) {
            return this.condition(new DatabaseCondition(field, expected));
        }

        public Builder condition(String field, Object expected) {
            return this.condition(new DatabaseCondition(field, expected));
        }

        public DeleteStatement build() {
            return new DeleteStatement(this.tableName, this.conditions);
        }

    }
}
