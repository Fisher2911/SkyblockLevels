package io.github.fisher2911.skyblocklevels.util.weight;

import io.github.fisher2911.skyblocklevels.util.Random;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class WeightedList<T> {

    private final List<Weight<T>> weightList;

    public WeightedList(List<Weight<T>> weightList) {
        this.weightList = weightList;
        Collections.sort(this.weightList);
    }

    @Nullable
    public T getRandom() {
        final double totalWeight = this.weightList.stream().mapToDouble(Weight::getWeight).sum();
        final double random = Random.nextDouble(totalWeight);
        double currentWeight = 0;
        for (Weight<T> weight : this.weightList) {
            currentWeight += weight.getWeight();
            if (currentWeight >= random) {
                return weight.getValue();
            }
        }
        return null;
    }

    public static <T> Builder<T> builder(List<Weight<T>> list) {
        return new Builder<>(list);
    }

    public List<Weight<T>> getWeightList() {
        return weightList;
    }

    public static class Builder<T> {

        private List<Weight<T>> list;

        private Builder(List<Weight<T>> list) {
            this.list = list;
        }

        public Builder<T> add(T value, double weight) {
            this.list.add(new Weight<>(value, weight));
            return this;
        }

        public WeightedList<T> build() {
            return new WeightedList<>(this.list);
        }
    }

    public static <T> Serializer<T> serializer(Class<T> type, @Nullable TypeSerializer<T> serializer) {
        return new Serializer<>(Weight.serializer(type, serializer), type);
    }

    public static class Serializer<T> implements TypeSerializer<WeightedList<T>> {

        @Nullable
        private final TypeSerializer<Weight<T>> serializer;
        private final Class<T> clazz;

        private Serializer(@Nullable TypeSerializer<Weight<T>> serializer, Class<T> clazz) {
            this.serializer = serializer;
            this.clazz = clazz;
        }

        @Override
        public WeightedList<T> deserialize(Type type, ConfigurationNode node) {
            final List<Weight<T>> list = node.childrenMap().values().stream().
                    map(child -> {
                        try {
                            if (serializer == null) return (Weight<T>) child.get(this.clazz);
                            return (Weight<T>) this.serializer.deserialize(this.clazz, child);
                        } catch (SerializationException e) {
                            e.printStackTrace();
                            return null;
                        }
                    }).
                    filter(weight -> weight != null).
                    collect(Collectors.toList());
            return new WeightedList<>(list);
        }

        @Override
        public void serialize(Type type, @org.checkerframework.checker.nullness.qual.Nullable WeightedList<T> obj, ConfigurationNode node) {

        }
    }
}
