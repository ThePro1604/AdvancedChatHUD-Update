/*
 * Copyright (C) 2021-2022 thepro1604
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.thepro1604.advancedchathud;

import io.github.thepro1604.advancedchatcore.ModuleHandler;
import io.github.thepro1604.advancedchathud.tabs.MainChatTab;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Environment(EnvType.CLIENT)
public class AdvancedChatHud implements ClientModInitializer {

    public static final String MOD_ID = "advancedchathud";
    public static MainChatTab MAIN_CHAT_TAB;
    public static Logger LOGGER = LogManager.getLogger("AdvancedChatHUD");

    @Override
    public void onInitializeClient() {
        // This will run after AdvancedChatCore's because of load order
        ModuleHandler.getInstance().registerInitHandler(MOD_ID, 0, new HudInitHandler());
    }
}
