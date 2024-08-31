package com.mohistmc.banner.fabric;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface FabricHookBukkitEvent {

    Event<FabricHookBukkitEvent> EVENT = EventFactory.createArrayBacked(FabricHookBukkitEvent.class,
            (listeners) -> (bukkitEvent) -> {
                for (FabricHookBukkitEvent listener : listeners) {
                    listener.hook(bukkitEvent);
                }
            });

    void hook(org.bukkit.event.Event bukkitEvent);
}
