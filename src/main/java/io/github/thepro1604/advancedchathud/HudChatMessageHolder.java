/*
 * Copyright (C) 2021 thepro1604
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.thepro1604.advancedchathud;

import io.github.thepro1604.advancedchatcore.chat.ChatMessage;
import io.github.thepro1604.advancedchatcore.interfaces.IChatMessageProcessor;
import io.github.thepro1604.advancedchathud.config.HudConfigStorage;
import io.github.thepro1604.advancedchathud.gui.WindowManager;
import lombok.Getter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class HudChatMessageHolder implements IChatMessageProcessor {

    @Getter private final List<HudChatMessage> messages = new ArrayList<>();

    private static final HudChatMessageHolder INSTANCE = new HudChatMessageHolder();

    private HudChatMessageHolder() {}

    public static HudChatMessageHolder getInstance() {
        return INSTANCE;
    }

    @Override
    public void onMessageUpdate(ChatMessage message, UpdateType type) {
        if (type == UpdateType.ADDED) {
            addMessage(new HudChatMessage(message));
        } else if (type == UpdateType.REMOVE) {
            remove(message);
        } else if (type == UpdateType.STACK) {
            HudChatMessage m = getMessage(message);
            if (m != null) {
                WindowManager.getInstance().onStackedMessage(m);
            }
        }
    }

    private void addMessage(HudChatMessage message) {
        messages.addFirst(message);
        WindowManager.getInstance().onNewMessage(message);
        while (messages.size() > HudConfigStorage.General.STORED_LINES.config.getIntegerValue()) {
            messages.removeLast();
        }
    }

    public void clear() {
        this.messages.clear();
    }

    public void remove(ChatMessage message) {
        HudChatMessage remove = getMessage(message);
        if (remove != null) {
            messages.remove(remove);
            WindowManager.getInstance().onRemoveMessage(remove.getMessage());
        }
    }

    public void removeChatMessage(ChatMessage message) {
        HudChatMessage m = getMessageFromContent(message);
        if (m != null) {
            messages.remove(m);
            WindowManager.getInstance().onRemoveMessage(message);
        }
    }

    public HudChatMessage getMessage(ChatMessage message) {
        for (HudChatMessage m : messages) {
            if (m.getMessage().getUuid().equals(message.getUuid())) {
                return m;
            }
        }
        return null;
    }

    public HudChatMessage getMessageFromContent(ChatMessage message) {
        for (HudChatMessage m : messages) {
            if (m.getMessage().isSimilar(message) && m.getMessage().getTime().equals(message.getTime())) {
                return m;
            }
        }
        return null;
    }
}
