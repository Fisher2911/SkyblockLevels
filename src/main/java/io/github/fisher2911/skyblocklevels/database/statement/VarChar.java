package io.github.fisher2911.skyblocklevels.database.statement;

public class VarChar {

    public static final VarChar UUID = VarChar.of(36);
    public static final VarChar ITEM_ID = VarChar.of(75);

    private final int length;

    private VarChar(int length) {
        this.length = length;
    }

    public static VarChar of(int length) {
        return new VarChar(length);
    }

    public int getLength() {
        return length;
    }
}
