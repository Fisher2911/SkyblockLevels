package io.github.fisher2911.skyblocklevels.item;

public abstract class SpecialSkyItem implements Usable {

    private final SkyItem skyItem;

    public SpecialSkyItem(SkyItem skyItem) {
        this.skyItem = skyItem;
    }

    public SkyItem getSkyItem() {
        return skyItem;
    }

}
