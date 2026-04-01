/*
 * Copyright (C) 2021 DarkKronicle
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.darkkronicle.advancedchathud.config.gui;

import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import io.github.darkkronicle.advancedchatcore.gui.WidgetConfigList;
import io.github.darkkronicle.advancedchathud.config.ChatTab;
import io.github.darkkronicle.advancedchathud.config.Match;
import net.minecraft.client.gui.screen.Screen;

import java.util.Collection;

public class WidgetListMatches extends WidgetConfigList<Match, WidgetMatchEntry> {

    public final ChatTab tab;

    public WidgetListMatches(
            int x,
            int y,
            int width,
            int height,
            ISelectionListener<Match> selectionListener,
            ChatTab parent,
            Screen screen) {
        super(x, y, width, height, selectionListener, screen);
        this.tab = parent;
        this.setParent(screen);
    }

    @Override
    protected WidgetMatchEntry createListEntryWidget(
            int x, int y, int listIndex, boolean isOdd, Match entry) {
        return new WidgetMatchEntry(
                x,
                y,
                this.browserEntryWidth,
                this.getBrowserEntryHeightFor(entry),
                isOdd,
                entry,
                listIndex,
                this);
    }

    public boolean onKeyTyped(int keyCode, int scanCode, int modifiers) {
        // Note: super.onKeyTyped signature changed in newer malilib versions
        // Commenting out the super call for now as the signature is incompatible
        // boolean val = super.onKeyTyped(keyCode, scanCode, modifiers);
        for (WidgetMatchEntry widget : this.listWidgets) {
            widget.save();
        }
        return false;
    }

    public void save() {
        for (WidgetMatchEntry widget : this.listWidgets) {
            widget.save();
        }
    }

    @Override
    protected Collection<Match> getAllEntries() {
        return tab.getMatches();
    }
}
