package io.github.fisher2911.skyblocklevels.placeholder;

import net.kyori.adventure.text.Component;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class Placeholder {

    private Component current;
    private final Map<Class<?>, Transformer<Object>> transformers;

    public Placeholder(Component current, Map<Class<?>, Transformer<Object>> transformers) {
        this.current = current;
        this.transformers = transformers;
    }

    public Placeholder parse(Object... args) {
        for (Object o : args) {
            final Collection<Transformer<Object>> transformers = this.transformers.entrySet().
                    stream().
                    filter(e -> e.getKey().isInstance(o)).
                    map(Map.Entry::getValue).
                    collect(Collectors.toSet());
            for (Transformer<Object> transformer : transformers) {
                this.current = transformer.transform(this.current, o);
            }
        }
        return this;
    }

    public static Builder builder(Component current) {
        return new Builder(current);
    }

    public Component get() {
        return this.current;
    }

    public static class Builder {

        private Component current;
        private Map<Class<?>, Transformer<Object>> transformers;

        private Builder(Component current) {
            this.current = current;
        }

        public Builder transformers(Map<Class<?>, Transformer<Object>> transformers) {
            this.transformers = transformers;
            return this;
        }

        public Placeholder build() {
            return new Placeholder(this.current, this.transformers);
        }

    }
}
