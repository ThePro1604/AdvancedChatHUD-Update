/*
 * Copyright (C) 2021-2022 thepro1604
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.thepro1604.advancedchathud;

import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.RenderEventHandler;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import io.github.thepro1604.advancedchatcore.AdvancedChatCore;
import io.github.thepro1604.advancedchatcore.chat.ChatHistory;
import io.github.thepro1604.advancedchatcore.chat.ChatScreenSectionHolder;
import io.github.thepro1604.advancedchatcore.config.gui.GuiConfigHandler;
import io.github.thepro1604.advancedchathud.config.HudConfigStorage;
import io.github.thepro1604.advancedchathud.config.gui.GuiTabManager;
import io.github.thepro1604.advancedchathud.gui.HudSection;
import io.github.thepro1604.advancedchathud.gui.WindowManager;
import io.github.thepro1604.advancedchathud.itf.IChatHud;
import io.github.thepro1604.advancedchathud.tabs.MainChatTab;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class HudInitHandler implements IInitializationHandler {

    @Override
    public void registerModHandlers() {
        AdvancedChatCore.FORWARD_TO_HUD = false;
        ConfigManager.getInstance().registerConfigHandler(AdvancedChatHud.MOD_ID, new HudConfigStorage());
        GuiConfigHandler.getInstance().addTab(
                GuiConfigHandler.children("advancedchathud", "advancedchathud.tab.advancedchathud",
                        GuiConfigHandler.wrapSaveableOptions(
                                "hud_general",
                                "advancedchathud.tab.general",
                                HudConfigStorage.General.OPTIONS
                        ),
                        GuiConfigHandler.wrapScreen(
                                "tabs",
                                "advancedchathud.tab.tabs",
                                (parent) -> new GuiTabManager()
                        )
                )
        );
        IChatHud.getInstance().setTab(AdvancedChatHud.MAIN_CHAT_TAB = new MainChatTab());

        // Register on the clear
        ChatScreenSectionHolder.getInstance().addSectionSupplier(HudSection::new);
        ChatHistory.getInstance().addOnClear(() -> WindowManager.getInstance().clear());
        ChatHistory.getInstance().addOnClear(() -> HudChatMessageHolder.getInstance().clear());
        ChatHistory.getInstance().addOnUpdate(HudChatMessageHolder.getInstance());
        // Rendering is now handled by MixinInGameHud instead of MaLiLib's RenderEventHandler
        // RenderEventHandler.getInstance().registerGameOverlayRenderer(WindowManager.getInstance());
        ResolutionEventHandler.ON_RESOLUTION_CHANGE.add(WindowManager.getInstance());
    }
}
