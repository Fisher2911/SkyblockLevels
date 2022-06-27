package io.github.fisher2911.skyblocklevels.item;

import com.google.common.collect.Multimaps;
import io.github.fisher2911.skyblocklevels.placeholder.Transformer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public interface Delayed {

    Map<Class<?>, Transformer<Object>> PLACEHOLDERS = Map.of(Delayed.class,
            Transformer.builder(Multimaps.newSetMultimap(new HashMap<>(), HashSet::new)).
                    with("%delay%", d -> ((Delayed) d).getTickDelay() / 20).
                    build()
    );

    int getTickDelay();

}
