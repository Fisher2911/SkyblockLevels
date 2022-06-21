package io.github.fisher2911.skyblocklevels.item;

import io.github.fisher2911.skyblocklevels.user.User;
import org.bukkit.event.entity.EntityDamageEvent;

public interface SkyWeapon {

    void onAttack(User user, EntityDamageEvent event);

}
