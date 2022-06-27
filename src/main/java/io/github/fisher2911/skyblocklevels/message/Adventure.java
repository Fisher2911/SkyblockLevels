package io.github.fisher2911.skyblocklevels.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class Adventure {

    public static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();/*builder().
            tags(StandardTags.defaults()).build();*/

    public static Component parse(String message) {
        return MINI_MESSAGE.deserialize(message);
    }

}
