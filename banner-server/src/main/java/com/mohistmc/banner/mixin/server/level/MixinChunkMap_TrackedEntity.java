package com.mohistmc.banner.mixin.server.level;

import com.mohistmc.banner.injection.server.level.InjectionServerEntity;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(ChunkMap.TrackedEntity.class)
public class MixinChunkMap_TrackedEntity {


    @Shadow @Final
    ServerEntity serverEntity;

    @Shadow @Final public Set<ServerPlayerConnection> seenBy;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void banner$setTrackedPlayers(ChunkMap outer, Entity entity, int range, int updateFrequency, boolean sendVelocityUpdates, CallbackInfo ci) {
        ((InjectionServerEntity) this.serverEntity).setTrackedPlayers(this.seenBy);
    }

    @Inject(method = "updatePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerEntity;addPairing(Lnet/minecraft/server/level/ServerPlayer;)V"))
    private void banner$fixPlayerDesync(ServerPlayer serverPlayer, CallbackInfo ci) {
        ((InjectionServerEntity) this.serverEntity).onPlayerAdd();
    }
}
