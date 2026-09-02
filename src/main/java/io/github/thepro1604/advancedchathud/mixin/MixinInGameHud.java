/*
 * Copyright (C) 2024 thepro1604
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.thepro1604.advancedchathud.mixin;

import io.github.thepro1604.advancedchathud.gui.WindowManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Gui.extractRenderState no longer takes a GuiGraphicsExtractor in 26.2 (it now delegates
// the actual overlay drawing to Hud.extractRenderState), so inject there instead.
@Mixin(Hud.class)
@Environment(EnvType.CLIENT)
public class MixinInGameHud {

    @Inject(at = @At("TAIL"), method = "extractRenderState")
    private void onRenderHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        // Render chat windows after all other HUD elements
        WindowManager.getInstance().onRenderGameOverlayPost(graphics);
    }
}

