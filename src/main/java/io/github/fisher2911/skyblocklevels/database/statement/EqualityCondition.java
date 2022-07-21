package io.github.fisher2911.skyblocklevels.database.statement;

public class EqualityCondition implements DatabaseCondition {

    private final String fieldName;
    private final String expected;

    public EqualityCondition(String fieldName, String expected) {
        this.fieldName = fieldName;
        this.expected = expected;
}

    public EqualityCondition(String fieldName, Object expected) {
        this.fieldName = fieldName;
        this.expected = String.valueOf(expected);
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getExpected() {
        return "'" + this.expected + "'";
    }

    @Override
    public String getCondition() {
        return this.fieldName + " = " + this.getExpected();
    }
}
