package io.github.fisher2911.skyblocklevels.placeholder;

import com.google.common.collect.Multimap;
import net.kyori.adventure.text.Component;

import java.util.function.Function;

public class Transformer<T> {

    private final Multimap<String, Function<T, Object>> transformers;

    public Transformer(Multimap<String, Function<T, Object>> transformers) {
        this.transformers = transformers;
    }

    public Component transform(Component component, T object) {
        for (var entry : this.transformers.entries()) {
            final Object replace = entry.getValue().apply(object);
            if (replace != null) {
                component = component.replaceText(b -> b.matchLiteral(entry.getKey()).replacement(String.valueOf(replace)));
            }
        }
        return component;
    }

    public static <T> Builder<T> builder(Multimap<String, Function<T, Object>> transformers) {
        return new Builder<>(transformers);
    }

    public static class Builder<T> {

        private final Multimap<String, Function<T, Object>> transformers;

        private Builder(Multimap<String, Function<T, Object>> transformers) {
            this.transformers = transformers;
        }

        @SafeVarargs
        public final Builder<T> with(String key, Function<T, Object>... transformers) {
            for (var transformer : transformers) {
                this.transformers.put(key, transformer);
            }
            return this;
        }

        public Transformer<T> build() {
            return new Transformer<T>(this.transformers);
        }
    }
}
