package io.github.fisher2911.skyblocklevels.util.weight;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;

public class Weight<T> implements Comparable<Weight<T>> {

    private final T value;
    private final double weight;

    public Weight(T value, double weight) {
        this.value = value;
        this.weight = weight;
    }

    public T getValue() {
        return value;
    }

    public double getWeight() {
        return weight;
    }

    @Override
    public int compareTo(@NotNull Weight<T> o) {
        return Double.compare(this.weight, o.weight);
    }

    public static <T> Serializer<T> serializer(Class<T> type, @Nullable TypeSerializer<T> serializer) {
        return new Serializer<>(serializer, type);
    }

    public static class Serializer<T> implements TypeSerializer<Weight<T>> {

        @Nullable
        private final TypeSerializer<T> serializer;
        private final Class<T> clazz;

        private Serializer(@Nullable TypeSerializer<T> serializer, Class<T> clazz) {
            this.serializer = serializer;
            this.clazz = clazz;
        }

        private static final String WEIGHT = "weight";
        private static final String ITEM = "item";

        @Override
        public Weight<T> deserialize(Type type, ConfigurationNode node) throws SerializationException {
            final double weight = node.node(WEIGHT).getDouble();
            if (this.serializer == null) return new Weight(node.node(ITEM).get(this.clazz), weight);
            final T value = this.serializer.deserialize(clazz, node.node(ITEM));
            return new Weight<>(value, weight);

        }

        @Override
        public void serialize(Type type, @Nullable Weight<T> obj, ConfigurationNode node) throws SerializationException {

        }
    }
}
