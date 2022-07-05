package io.github.fisher2911.skyblocklevels.item;

import io.github.fisher2911.skyblocklevels.user.User;
import org.bukkit.event.entity.EntityDamageEvent;

public interface SkyWeapon extends Usable, SpecialSkyItem {

    void onAttack(User user, EntityDamageEvent event);

}
