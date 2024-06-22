package com.mohistmc.banner.injection.server.players;

import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.event.player.PlayerRespawnEvent;

public interface InjectionPlayerList {

    default CraftServer getCraftServer() {
        return null;
    }

    default ServerPlayer respawn(ServerPlayer entityplayer, boolean flag, Entity.RemovalReason entity_removalreason, PlayerRespawnEvent.RespawnReason reason) {
        return null;
    }

    default ServerPlayer respawn(ServerPlayer entityplayer, ServerLevel worldserver, boolean flag, Location location, boolean avoidSuffocation, Entity.RemovalReason entity_removalreason, PlayerRespawnEvent.RespawnReason reason) {
        return null;
    }

    default void broadcastAll(Packet<?> packet, Player entityhuman) {
    }

    default void broadcastAll(Packet<?> packet, Level world) {
    }

    default void broadcastMessage(Component[] iChatBaseComponents) {
    }

    default ServerStatsCounter getPlayerStats(ServerPlayer entityhuman) {
        return null;
    }

    default ServerStatsCounter getPlayerStats(UUID uuid, String displayName) {
        return null;
    }

    default String bridge$quiltMsg() {
        return null;
    }
}
