package com.mohistmc.banner.mixin.core.world.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.event.entity.EntityUnleashEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PathfinderMob.class)
public abstract class MixinPathfinderMob extends Mob {

    protected MixinPathfinderMob(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
    }

    // Banner TODO
    /*
    @Inject(method = "handleLeashAtDistance", at = @At(value = "INVOKE", target = "dropLeash"))
    private void banner$unleashDistance(CallbackInfo ci) {
        Bukkit.getPluginManager().callEvent(new EntityUnleashEvent(this.getBukkitEntity(), EntityUnleashEvent.UnleashReason.DISTANCE));
    }*/
}
