package io.github.fisher2911.skyblocklevels.util;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;

public class Range {

    private final int min;
    private final int max;

    public Range(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public int getMin() {
        return this.min;
    }

    public int getMax() {
        return this.max;
    }

    public int getRandom() {
        return Random.nextInt(this.min, this.max);
    }

    public static Serializer serializer() {
        return Serializer.INSTANCE;
    }

    public static class Serializer implements TypeSerializer<Range> {

        private static final Serializer INSTANCE = new Serializer();

        private static final String MIN_KEY = "min";
        private static final String MAX_KEY = "max";

        @Override
        public Range deserialize(Type type, ConfigurationNode node) throws SerializationException {
            final int min = node.node(MIN_KEY).getInt();
            final int max = node.node(MAX_KEY).getInt();
            return new Range(min, max);
        }

        @Override
        public void serialize(Type type, @Nullable Range obj, ConfigurationNode node) throws SerializationException {

        }
    }

}
