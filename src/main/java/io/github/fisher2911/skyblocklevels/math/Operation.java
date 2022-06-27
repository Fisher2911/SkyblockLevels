package io.github.fisher2911.skyblocklevels.math;

import org.jetbrains.annotations.Nullable;

public enum Operation {

    ADD('+'),
    SUBTRACT('-'),
    MULTIPLY('*'),
    DIVIDE('/');

    private final char sign;

    Operation(char sign) {
        this.sign = sign;
    }

    public char getSign() {
        return sign;
    }

    @Nullable
    public static Operation bySign(char sign) {
        for (Operation operation : values()) {
            if (operation.getSign() == sign) {
                return operation;
            }
        }
        return null;
    }

    public double handle(double a, double b) {
        return switch (this) {
            case ADD -> a + b;
            case SUBTRACT -> a - b;
            case MULTIPLY -> a * b;
            case DIVIDE -> a / b;
        };
    }

    public int handle(int a, int b) {
        return switch (this) {
            case ADD -> a + b;
            case SUBTRACT -> a - b;
            case MULTIPLY -> a * b;
            case DIVIDE -> a / b;
        };
    }

    public float handle(float a, float b) {
        return switch (this) {
            case ADD -> a + b;
            case SUBTRACT -> a - b;
            case MULTIPLY -> a * b;
            case DIVIDE -> a / b;
        };
    }

}
