package io.github.fisher2911.skyblocklevels.item;

import io.github.fisher2911.skyblocklevels.math.Operation;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;

public class MineSpeedModifier {

    public static final MineSpeedModifier SELF = new MineSpeedModifier(1, Operation.MULTIPLY);

    private final double modifier;
    private final Operation operation;

    public MineSpeedModifier(double modifier, Operation operation) {
        this.modifier = modifier;
        this.operation = operation;
    }

    public double modify(double speed) {
        return this.operation.handle(speed, this.modifier);
    }

    public int modify(int speed) {
        return (int) this.operation.handle(speed, this.modifier);
    }

    public double getModifier() {
        return this.modifier;
    }

    public Operation getOperation() {
        return this.operation;
    }

    public static Serializer serializer() {
        return Serializer.INSTANCE;
    }

    public static class Serializer implements TypeSerializer<MineSpeedModifier> {

        private static final Serializer INSTANCE = new Serializer();

        private Serializer() {}

        private static final String VALUE = "value";

        @Override
        public MineSpeedModifier deserialize(Type type, ConfigurationNode node) throws SerializationException {
            final String value = node.node(VALUE).getString("");
            if (value.isEmpty()) return SELF;
            final Operation operation = Operation.bySign(value.charAt(0));
            final double modifier = Double.parseDouble(value.substring(1));
            return new MineSpeedModifier(modifier, operation);
        }

        @Override
        public void serialize(Type type, @Nullable MineSpeedModifier obj, ConfigurationNode node) throws SerializationException {

        }
    }
}
