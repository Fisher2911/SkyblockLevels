package io.github.fisher2911.skyblocklevels.database;

public interface Changeable<T> {

    T getChanged();
    void setChanged(T t);

}
