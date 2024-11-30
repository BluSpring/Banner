package com.mohistmc.banner.mixin.server.level;

import com.google.common.collect.Lists;
import com.llamalad7.mixinextras.sugar.Local;
import com.mohistmc.banner.asm.annotation.CreateConstructor;
import com.mohistmc.banner.asm.annotation.ShadowConstructor;
import com.mohistmc.banner.bukkit.BukkitFieldHooks;
import com.mohistmc.banner.injection.server.level.InjectionServerEntity;
import com.mojang.datafixers.util.Pair;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Mixin(ServerEntity.class)
public abstract class MixinServerEntity implements InjectionServerEntity {


    // @formatter:off
    @Shadow @Final private Entity entity;
    @Shadow private List<Entity> lastPassengers;
    @Shadow @Final private Consumer<Packet<?>> broadcast;
    @Shadow private int tickCount;
    @Shadow @Final private ServerLevel level;
    @Shadow protected abstract void sendDirtyEntityData();
    @Shadow @Final private int updateInterval;
    @Shadow @Final private VecDeltaCodec positionCodec;
    @Shadow private boolean wasRiding;
    @Shadow private int teleportDelay;
    @Shadow private boolean wasOnGround;
    @Shadow @Final private boolean trackDelta;
    @Shadow protected abstract void broadcastAndSend(Packet<?> packet);
    @Shadow @Nullable private List<SynchedEntityData.DataValue<?>> trackedDataValues;
    // @formatter:on

    @Shadow
    private static Stream<Entity> removedPassengers(List<Entity> list, List<Entity> list2) {
        return null;
    }

    @Shadow private int lastSentXRot;
    @Shadow private int lastSentYRot;
    @Shadow private Vec3 lastSentMovement;
    @Shadow private int lastSentYHeadRot;
    private Set<ServerPlayerConnection> trackedPlayers;
    @Unique private int lastTick;
    @Unique private int lastUpdate, lastPosUpdate, lastMapUpdate;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void banner$init(ServerLevel serverWorld, Entity entity, int updateFrequency, boolean sendVelocityUpdates, Consumer<Packet<?>> packetConsumer, CallbackInfo ci) {
        trackedPlayers = new HashSet<>();
        lastTick = BukkitFieldHooks.currentTick() - 1;
        lastUpdate = lastPosUpdate = lastMapUpdate = -1;
    }

    @ShadowConstructor
    public void banner$constructor(ServerLevel serverWorld, Entity entity, int updateFrequency, boolean sendVelocityUpdates, Consumer<Packet<?>> packetConsumer) {
        throw new NullPointerException();
    }

    @CreateConstructor
    public void banner$constructor(ServerLevel serverWorld, Entity entity, int updateFrequency, boolean sendVelocityUpdates, Consumer<Packet<?>> packetConsumer, Set<ServerPlayerConnection> set) {
        banner$constructor(serverWorld, entity, updateFrequency, sendVelocityUpdates, packetConsumer);
        this.trackedPlayers = set;
    }

    // Paper start - fix desync when a player is added to the tracker
    @Unique private boolean forceStateResync;
    public void onPlayerAdd() {
        this.forceStateResync = true;
    }
    // Paper end - fix desync when a player is added to the tracker

    /**
     * @author wdog5
     * @reason
     */
    @Overwrite
    public void sendChanges() {
        List<Entity> list = this.entity.getPassengers();

        if (!list.equals(this.lastPassengers)) {
            this.broadcastAndSend(new ClientboundSetPassengersPacket(this.entity)); // CraftBukkit
            removedPassengers(list, this.lastPassengers).forEach((entity) -> {
                if (entity instanceof ServerPlayer entityplayer) {
                    entityplayer.connection.teleport(entityplayer.getX(), entityplayer.getY(), entityplayer.getZ(), entityplayer.getYRot(), entityplayer.getXRot());
                }

            });
            this.lastPassengers = list;
        }

        Entity entity = this.entity;

        if (!this.trackedPlayers.isEmpty() && entity instanceof ItemFrame entityitemframe) { // Paper - Perf: Only tick item frames if players can see it
            if (true || this.tickCount % 10 == 0) { // CraftBukkit - Moved below, should always enter this block
                ItemStack itemstack = entityitemframe.getItem();

                if (this.tickCount / 10 != this.lastMapUpdate && itemstack.getItem() instanceof MapItem) { // CraftBukkit - Moved this.tickCounter % 10 logic here so item frames do not enter the other blocks
                    MapItemSavedData mapdata = MapItem.getSavedData(itemstack, this.level);
                    if (mapdata != null) {
                        for (ServerPlayerConnection connection : this.trackedPlayers) {
                            var serverplayerentity = connection.getPlayer();
                            mapdata.tickCarriedBy(serverplayerentity, itemstack);
                            Packet<?> ipacket = ((MapItem) itemstack.getItem()).getUpdatePacket(itemstack, this.level, serverplayerentity);
                            if (ipacket != null) {
                                serverplayerentity.connection.send(ipacket);
                            }
                        }
                    }
                }

                this.sendDirtyEntityData();
            }
        }

        if (this.forceStateResync || this.tickCount % this.updateInterval == 0 || this.entity.hasImpulse || this.entity.getEntityData().isDirty()) { // Paper - fix desync when a player is added to the tracker
            int i;
            int j;

            if (this.entity.isPassenger()) {
                i = Mth.floor(this.entity.getYRot() * 256.0F / 360.0F);
                j = Mth.floor(this.entity.getXRot() * 256.0F / 360.0F);
                boolean flag = Math.abs(i - this.lastSentYRot) >= 1 || Math.abs(j - this.lastSentXRot) >= 1;

                if (flag) {
                    this.broadcast.accept(new ClientboundMoveEntityPacket.Rot(this.entity.getId(), (byte) i, (byte) j, this.entity.onGround()));
                    this.lastSentYRot = i;
                    this.lastSentXRot = j;
                }

                this.positionCodec.setBase(this.entity.trackingPosition());
                this.sendDirtyEntityData();
                this.wasRiding = true;
            } else {
                ++this.teleportDelay;
                i = Mth.floor(this.entity.getYRot() * 256.0F / 360.0F);
                j = Mth.floor(this.entity.getXRot() * 256.0F / 360.0F);
                Vec3 vec3d = this.entity.trackingPosition();
                // Paper start - reduce allocation of Vec3D here
                Vec3 base = this.positionCodec.getBase();
                double vec3d_dx = vec3d.x - base.x;
                double vec3d_dy = vec3d.y - base.y;
                double vec3d_dz = vec3d.z - base.z;
                boolean flag1 = (vec3d_dx * vec3d_dx + vec3d_dy * vec3d_dy + vec3d_dz * vec3d_dz) >= 7.62939453125E-6D;
                // Paper end - reduce allocation of Vec3D here
                Packet<?> packet1 = null;
                boolean flag2 = flag1 || this.tickCount % 60 == 0;
                boolean flag3 = Math.abs(i - this.lastSentYRot) >= 1 || Math.abs(j - this.lastSentXRot) >= 1;
                boolean flag4 = false;
                boolean flag5 = false;
                long k = this.positionCodec.encodeX(vec3d);
                long l = this.positionCodec.encodeY(vec3d);
                long i1 = this.positionCodec.encodeZ(vec3d);
                boolean flag6 = k < -32768L || k > 32767L || l < -32768L || l > 32767L || i1 < -32768L || i1 > 32767L;

                if (!this.forceStateResync && !flag6 && this.teleportDelay <= 400 && !this.wasRiding && this.wasOnGround == this.entity.onGround()) { // Paper - fix desync when a player is added to the tracker
                    if ((!flag2 || !flag3) && !(this.entity instanceof AbstractArrow)) {
                        if (flag2) {
                            packet1 = new ClientboundMoveEntityPacket.Pos(this.entity.getId(), (short) ((int) k), (short) ((int) l), (short) ((int) i1), this.entity.onGround());
                            flag4 = true;
                        } else if (flag3) {
                            packet1 = new ClientboundMoveEntityPacket.Rot(this.entity.getId(), (byte) i, (byte) j, this.entity.onGround());
                            flag5 = true;
                        }
                    } else {
                        packet1 = new ClientboundMoveEntityPacket.PosRot(this.entity.getId(), (short) ((int) k), (short) ((int) l), (short) ((int) i1), (byte) i, (byte) j, this.entity.onGround());
                        flag4 = true;
                        flag5 = true;
                    }
                } else {
                    this.wasOnGround = this.entity.onGround();
                    this.teleportDelay = 0;
                    packet1 = new ClientboundTeleportEntityPacket(this.entity);
                    flag4 = true;
                    flag5 = true;
                }

                if ((this.trackDelta || this.entity.hasImpulse || this.entity instanceof LivingEntity && ((LivingEntity) this.entity).isFallFlying()) && this.tickCount > 0) {
                    Vec3 vec3d1 = this.entity.getDeltaMovement();
                    double d0 = vec3d1.distanceToSqr(this.lastSentMovement);

                    if (d0 > 1.0E-7D || d0 > 0.0D && vec3d1.lengthSqr() == 0.0D) {
                        this.lastSentMovement = vec3d1;
                        Entity entity1 = this.entity;

                        if (entity1 instanceof AbstractHurtingProjectile) {
                            AbstractHurtingProjectile entityfireball = (AbstractHurtingProjectile) entity1;

                            this.broadcast.accept(new ClientboundBundlePacket(List.of(new ClientboundSetEntityMotionPacket(this.entity.getId(), this.lastSentMovement), new ClientboundProjectilePowerPacket(entityfireball.getId(), entityfireball.accelerationPower))));
                        } else {
                            this.broadcast.accept(new ClientboundSetEntityMotionPacket(this.entity.getId(), this.lastSentMovement));
                        }
                    }
                }

                if (packet1 != null) {
                    this.broadcast.accept(packet1);
                }

                this.sendDirtyEntityData();
                if (flag4) {
                    this.positionCodec.setBase(vec3d);
                }

                if (flag5) {
                    this.lastSentYRot = i;
                    this.lastSentXRot = j;
                }

                this.wasRiding = false;
            }

            i = Mth.floor(this.entity.getYHeadRot() * 256.0F / 360.0F);
            if (Math.abs(i - this.lastSentYHeadRot) >= 1) {
                this.broadcast.accept(new ClientboundRotateHeadPacket(this.entity, (byte) i));
                this.lastSentYHeadRot = i;
            }

            this.entity.hasImpulse = false;
            this.forceStateResync = false; // Paper - fix desync when a player is added to the tracker
        }

        ++this.tickCount;
        if (this.entity.hurtMarked) {
            // CraftBukkit start - Create PlayerVelocity event
            boolean cancelled = false;

            if (this.entity instanceof ServerPlayer) {
                Player player = (Player) this.entity.getBukkitEntity();
                org.bukkit.util.Vector velocity = player.getVelocity();

                PlayerVelocityEvent event = new PlayerVelocityEvent(player, velocity.clone());
                this.entity.level().getCraftServer().getPluginManager().callEvent(event);

                if (event.isCancelled()) {
                    cancelled = true;
                } else if (!velocity.equals(event.getVelocity())) {
                    player.setVelocity(event.getVelocity());
                }
            }

            if (cancelled) {
                return;
            }
            // CraftBukkit end
            this.entity.hurtMarked = false;
            this.broadcastAndSend(new ClientboundSetEntityMotionPacket(this.entity));
        }
    }

    /**
     * @author wdog5
     * @reason
     */
    @Overwrite
    public void sendPairingData(ServerPlayer serverPlayer, Consumer<Packet<ClientGamePacketListener>> consumer) {
        Mob entityinsentient;
        if (this.entity.isRemoved()) {
            return;
        }
        Packet<ClientGamePacketListener> packet = this.entity.getAddEntityPacket(((ServerEntity) (Object) this));
        this.lastSentYHeadRot = Mth.floor(this.entity.getYHeadRot() * 256.0f / 360.0f);
        consumer.accept(packet);
        if (this.trackedDataValues != null) {
            consumer.accept(new ClientboundSetEntityDataPacket(this.entity.getId(), this.trackedDataValues));
        }
        boolean flag = this.trackDelta;
        if (this.entity instanceof LivingEntity livingEntity) {
            Collection<AttributeInstance> collection = livingEntity.getAttributes().getSyncableAttributes();
            if (this.entity.getId() == serverPlayer.getId()) {
                ((ServerPlayer) this.entity).getBukkitEntity().injectScaledMaxHealth(collection, false);
            }
            if (!collection.isEmpty()) {
                consumer.accept(new ClientboundUpdateAttributesPacket(this.entity.getId(), collection));
            }
            if (livingEntity.isFallFlying()) {
                flag = true;
            }
        }
        this.lastSentMovement = this.entity.getDeltaMovement();
        if (flag && !(this.entity instanceof LivingEntity)) {
            consumer.accept(new ClientboundSetEntityMotionPacket(this.entity.getId(), this.lastSentMovement));
        }
        if (this.entity instanceof LivingEntity) {
            ArrayList<Pair<EquipmentSlot, ItemStack>> list = Lists.newArrayList();
            for (EquipmentSlot enumitemslot : EquipmentSlot.values()) {
                ItemStack itemstack = ((LivingEntity) this.entity).getItemBySlot(enumitemslot);
                if (itemstack.isEmpty()) continue;
                list.add(Pair.of(enumitemslot, itemstack.copy()));
            }
            if (!list.isEmpty()) {
                consumer.accept(new ClientboundSetEquipmentPacket(this.entity.getId(), list));
            }
        }
        this.lastSentYHeadRot = Mth.floor(this.entity.getYHeadRot() * 256.0f / 360.0f);
        consumer.accept(new ClientboundRotateHeadPacket(this.entity, (byte) this.lastSentYHeadRot));
        if (this.entity instanceof LivingEntity livingEntity) {
            for (MobEffectInstance mobeffect : livingEntity.getActiveEffects()) {
                consumer.accept(new ClientboundUpdateMobEffectPacket(this.entity.getId(), mobeffect, true));
            }
            livingEntity.detectEquipmentUpdates();
        }
        // CraftBukkit start - MC-109346: Fix for nonsensical head yaw
        if (this.entity instanceof ServerPlayer) {
            consumer.accept(new ClientboundRotateHeadPacket(this.entity, (byte) Mth.floor(this.entity.getYHeadRot() * 256.0F / 360.0F)));
        }
        if (!this.entity.getPassengers().isEmpty()) {
            consumer.accept(new ClientboundSetPassengersPacket(this.entity));
        }
        if (this.entity.isPassenger()) {
            consumer.accept(new ClientboundSetPassengersPacket(this.entity.getVehicle()));
        }
        if (this.entity instanceof Mob && (entityinsentient = (Mob) this.entity).isLeashed()) {
            consumer.accept(new ClientboundSetEntityLinkPacket(entityinsentient, entityinsentient.getLeashHolder()));
        }
    }

    @Inject(method = "sendDirtyEntityData", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/server/level/ServerEntity;broadcastAndSend(Lnet/minecraft/network/protocol/Packet;)V"))
    private void banner$sendScaledHealth(CallbackInfo ci, @Local Set<AttributeInstance> set) {
        if (this.entity instanceof ServerPlayer player) {
            player.getBukkitEntity().injectScaledMaxHealth(set, false);
        }
    }

    @Override
    public void setTrackedPlayers(Set<ServerPlayerConnection> trackedPlayers) {
        this.trackedPlayers = trackedPlayers;
    }
}
