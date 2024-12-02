package com.mohistmc.banner.mixin.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.Cancellable;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FarmBlock.class)
public abstract class MixinFarmBlock extends Block {
    public MixinFarmBlock(Properties properties) {
        super(properties);
    }

    @Inject(method = "turnToDirt", cancellable = true, at = @At("HEAD"))
    private static void banner$blockFade(Entity entity, BlockState state, Level worldIn, BlockPos pos, CallbackInfo ci) {
        if (CraftEventFactory.callBlockFadeEvent(worldIn, pos, Blocks.DIRT.defaultBlockState()).isCancelled()) {
            ci.cancel();
        }
    }

    @Redirect(method = "randomTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    public boolean banner$moistureChange(ServerLevel world, BlockPos pos, BlockState newState, int flags) {
        return CraftEventFactory.handleMoistureChangeEvent(world, pos, newState, flags);
    }

    @Inject(method = "fallOn", at = @At("HEAD"))
    private void banner$moveSuperToHead(Level level, BlockState blockState, BlockPos blockPos, Entity entity, float f, CallbackInfo ci) {
        super.fallOn(level, blockState, blockPos, entity, f);
    }

    @Inject(method = "fallOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/FarmBlock;turnToDirt(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"), cancellable = true)
    private void banner$handleTrampling(Level level, BlockState blockState, BlockPos blockPos, Entity entity, float f, CallbackInfo ci) {
        Cancellable cancellable;

        if (entity instanceof Player player) {
            cancellable = CraftEventFactory.callPlayerInteractEvent(player, Action.PHYSICAL, blockPos, null, null, null);
        } else {
            cancellable = new EntityInteractEvent(entity.getBukkitEntity(), level.getWorld().getBlockAt(blockPos.getX(), blockPos.getY(), blockPos.getZ()));
            level.getCraftServer().getPluginManager().callEvent((EntityInteractEvent) cancellable);
        }

        if (cancellable.isCancelled()) {
            ci.cancel();
            return;
        }

        if (!CraftEventFactory.callEntityChangeBlockEvent(entity, blockPos, Blocks.DIRT.defaultBlockState())) {
            ci.cancel();
        }
    }

    @Redirect(method = "fallOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;fallOn(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;F)V"))
    private void banner$noopOriginalFallOnMethod(Block instance, Level level, BlockState blockState, BlockPos blockPos, Entity entity, float f) {
    }
}
