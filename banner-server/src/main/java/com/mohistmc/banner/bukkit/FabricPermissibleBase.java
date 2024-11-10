package com.mohistmc.banner.bukkit;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.util.TriState;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.ServerOperator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FabricPermissibleBase extends PermissibleBase {
    public FabricPermissibleBase(@Nullable ServerOperator opable) {
        super(opable);
    }

    @Override
    public boolean hasPermission(@NotNull String inName) {
        Permission perm = Bukkit.getServer().getPluginManager().getPermission(inName);

        try {
            return (this.banner$getParent() instanceof CraftHumanEntity humanEntity && Permissions.check(humanEntity.getHandle(), inName, perm != null && perm.getDefault().getValue(Permission.DEFAULT_PERMISSION.getValue(isOp())))) || super.hasPermission(inName);
        } catch (Throwable ignored) {
            if (perm != null) {
                return perm.getDefault().getValue(isOp());
            } else {
                return Permission.DEFAULT_PERMISSION.getValue(isOp());
            }
        }
    }

    @Override
    public boolean hasPermission(@NotNull Permission perm) {
        try {
            return (this.banner$getParent() instanceof CraftHumanEntity humanEntity && Permissions.check(humanEntity.getHandle(), perm.getName(), perm.getDefault().getValue(this.isOp()))) || super.hasPermission(perm);
        } catch (Throwable ignored) {
            return perm.getDefault().getValue(this.isOp());
        }
    }

    @Override
    public boolean isPermissionSet(@NotNull String name) {
        return (this.banner$getParent() instanceof CraftHumanEntity humanEntity && Permissions.getPermissionValue(humanEntity.getHandle(), name) != TriState.DEFAULT) || super.isPermissionSet(name);
    }

    @Override
    public boolean isPermissionSet(@NotNull Permission perm) {
        return (this.banner$getParent() instanceof CraftHumanEntity humanEntity && Permissions.getPermissionValue(humanEntity.getHandle(), perm.getName()) != TriState.DEFAULT) || super.isPermissionSet(perm);
    }
}
