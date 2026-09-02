/*
 * Copyright (C) 2021 thepro1604
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.thepro1604.advancedchathud.itf;

import io.github.thepro1604.advancedchatcore.chat.ChatMessage;
import io.github.thepro1604.advancedchathud.HudChatMessage;
import io.github.thepro1604.advancedchathud.tabs.AbstractChatTab;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

@Environment(EnvType.CLIENT)
public interface IChatHud {

    AbstractChatTab getTab();

    void setTab(AbstractChatTab tab);

    void addMessage(HudChatMessage message);

    void clear(boolean clearHistory);

    boolean isOver(double mouseX, double mouseY);

    static IChatHud getInstance() {
        return (IChatHud) Minecraft.getInstance().gui.hud.getChat();
    }

    void removeMessage(ChatMessage remove);
}
