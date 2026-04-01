/*
 * Copyright (C) 2021-2022 DarkKronicle
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.darkkronicle.advancedchathud;

import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.RenderEventHandler;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import io.github.darkkronicle.advancedchatcore.AdvancedChatCore;
import io.github.darkkronicle.advancedchatcore.chat.ChatHistory;
import io.github.darkkronicle.advancedchatcore.chat.ChatScreenSectionHolder;
import io.github.darkkronicle.advancedchatcore.config.gui.GuiConfigHandler;
import io.github.darkkronicle.advancedchathud.config.HudConfigStorage;
import io.github.darkkronicle.advancedchathud.config.gui.GuiTabManager;
import io.github.darkkronicle.advancedchathud.gui.HudSection;
import io.github.darkkronicle.advancedchathud.gui.WindowManager;
import io.github.darkkronicle.advancedchathud.itf.IChatHud;
import io.github.darkkronicle.advancedchathud.tabs.MainChatTab;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class HudInitHandler implements IInitializationHandler {

    @Override
    public void registerModHandlers() {
        AdvancedChatHud.LOGGER.info("[AdvancedChatHUD] Starting initialization...");
        AdvancedChatCore.FORWARD_TO_HUD = false;
        ConfigManager.getInstance().registerConfigHandler(AdvancedChatHud.MOD_ID, new HudConfigStorage());
        AdvancedChatHud.LOGGER.info("[AdvancedChatHUD] Config registered");
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
        AdvancedChatHud.LOGGER.info("[AdvancedChatHUD] GUI config tab added");
        IChatHud.getInstance().setTab(AdvancedChatHud.MAIN_CHAT_TAB = new MainChatTab());
        AdvancedChatHud.LOGGER.info("[AdvancedChatHUD] Main chat tab initialized");

        // Register on the clear
        ChatScreenSectionHolder.getInstance().addSectionSupplier(HudSection::new);
        AdvancedChatHud.LOGGER.info("[AdvancedChatHUD] HudSection registered with ChatScreenSectionHolder");
        ChatHistory.getInstance().addOnClear(() -> WindowManager.getInstance().clear());
        ChatHistory.getInstance().addOnClear(() -> HudChatMessageHolder.getInstance().clear());
        ChatHistory.getInstance().addOnUpdate(HudChatMessageHolder.getInstance());
        RenderEventHandler.getInstance().registerGameOverlayRenderer(WindowManager.getInstance());
        AdvancedChatHud.LOGGER.info("[AdvancedChatHUD] WindowManager registered as overlay renderer");
        ResolutionEventHandler.ON_RESOLUTION_CHANGE.add(WindowManager.getInstance());
        AdvancedChatHud.LOGGER.info("[AdvancedChatHUD] Initialization complete!");
    }
}
