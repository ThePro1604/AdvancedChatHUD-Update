/*
 * Copyright (C) 2021 thepro1604
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.thepro1604.advancedchathud.tabs;

import io.github.thepro1604.advancedchatcore.util.Color;
import lombok.Data;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;

import java.util.UUID;

/** Base ChatTab that allows for custom chat tabs in AdvancedChatHud. */
@Environment(EnvType.CLIENT)
@Data
public abstract class AbstractChatTab {

    // Each tab stores their own messages.
    protected String name;
    protected String abbreviation;
    protected Color mainColor;
    protected Color borderColor;
    protected Color innerColor;
    protected UUID uuid;
    private int unread = 0;
    protected boolean showUnread;

    public AbstractChatTab(
            String name,
            String abbreviation,
            Color mainColor,
            Color borderColor,
            Color innerColor,
            boolean showUnread,
            UUID uuid) {
        this.name = name;
        this.abbreviation = abbreviation;
        this.mainColor = mainColor;
        this.showUnread = showUnread;
        this.innerColor = innerColor;
        this.borderColor = borderColor;
        this.uuid = uuid;
    }

    public void addNewUnread() {
        this.unread++;
    }

    public void resetUnread() {
        this.unread = 0;
    }

    /**
     * If the inputted message should be put into the chat tab.
     *
     * @param Component Object to search.
     * @return True if it should be added.
     */
    public abstract boolean shouldAdd(Component text);
}
