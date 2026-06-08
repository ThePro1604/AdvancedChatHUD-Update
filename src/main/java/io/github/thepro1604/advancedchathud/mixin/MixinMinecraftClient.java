/*
 * Copyright (C) 2022 thepro1604
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.thepro1604.advancedchathud.mixin;

import io.github.thepro1604.advancedchathud.ResolutionEventHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraftClient {

    @Inject(method = "resizeGui", at = @At("HEAD"))
    private void onResChange(CallbackInfo ci) {
        for (ResolutionEventHandler handler : ResolutionEventHandler.ON_RESOLUTION_CHANGE) {
            handler.onResolutionChange();
        }
    }

}
