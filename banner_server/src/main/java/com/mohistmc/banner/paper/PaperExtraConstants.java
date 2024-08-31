package com.mohistmc.banner.paper;

import java.util.function.Predicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class PaperExtraConstants {

    // Paper start
    public static final Predicate<Entity> PLAYER_AFFECTS_SPAWNING = (entity) -> {
        return !entity.isSpectator() && entity.isAlive() && entity instanceof Player player && player.bridge$affectsSpawning();
    };
   // Paper end
}
