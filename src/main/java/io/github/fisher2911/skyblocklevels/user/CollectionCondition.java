package io.github.fisher2911.skyblocklevels.user;

import io.github.fisher2911.skyblocklevels.util.Condition;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class CollectionCondition implements Condition<Collection> {

    private final Map<String, Integer> requiredItems;

    public CollectionCondition(Map<String, Integer> requiredItems) {
        this.requiredItems = requiredItems;
    }

    @Override
    public boolean isAllowed(Collection collection) {
        for (Map.Entry<String, Integer> entry : requiredItems.entrySet()) {
            if (!collection.hasAmount(entry.getKey(), entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    public Map<String, Integer> getRequiredItems() {
        return requiredItems;
    }

    public static Serializer serializer() {
        return Serializer.INSTANCE;
    }

    public static class Serializer implements TypeSerializer<CollectionCondition> {

        private static final Serializer INSTANCE = new Serializer();

        @Override
        public CollectionCondition deserialize(Type type, ConfigurationNode node) throws SerializationException {
            final Map<String, Integer> requirements = new HashMap<>();
            for (var entry : node.childrenMap().entrySet()) {
                if (!(entry.getKey() instanceof final String key)) continue;
                final int amount = entry.getValue().getInt();
                requirements.put(key, amount);
            }
            return new CollectionCondition(requirements);
        }

        @Override
        public void serialize(Type type, @Nullable CollectionCondition obj, ConfigurationNode node) throws SerializationException {

        }
    }
}
