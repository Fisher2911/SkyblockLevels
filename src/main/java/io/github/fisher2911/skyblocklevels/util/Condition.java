package io.github.fisher2911.skyblocklevels.util;

@FunctionalInterface
public interface Condition<T> {

    boolean isAllowed(T t);

}
