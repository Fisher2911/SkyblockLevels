package io.github.fisher2911.skyblocklevels.database;

public class DatabaseCondition {

    private final String fieldName;
    final String expected;

    public DatabaseCondition(String fieldName, String expected) {
        this.fieldName = fieldName;
        this.expected = expected;
    }

    public DatabaseCondition(String fieldName, Object expected) {
        this.fieldName = fieldName;
        this.expected = String.valueOf(expected);
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getExpected() {
        return "'" + this.expected + "'";
    }

    public String getCondition() {
        return this.fieldName + " = " + this.getExpected();
    }
}
