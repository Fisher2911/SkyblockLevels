package io.github.fisher2911.skyblocklevels.item;

public interface SpecialSkyItem {

    SpecialSkyItem EMPTY = () -> SkyItem.EMPTY;

    SkyItem getSkyItem();

}
