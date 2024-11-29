package com.mohistmc.banner.injection.interaction.component;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;

public interface DataComponentPatch_BuilderInjection {
    void copy(DataComponentPatch orig);
    void clear(DataComponentType<?> type);
    boolean isSet(DataComponentType<?> type);
    boolean isEmpty();
}
