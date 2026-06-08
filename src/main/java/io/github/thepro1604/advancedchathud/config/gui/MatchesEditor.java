/*
 * Copyright (C) 2021 thepro1604
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.thepro1604.advancedchathud.config.gui;

import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.thepro1604.advancedchatcore.gui.CoreGuiListBase;
import io.github.thepro1604.advancedchatcore.gui.buttons.BackButtonListener;
import io.github.thepro1604.advancedchatcore.gui.buttons.Buttons;
import io.github.thepro1604.advancedchatcore.gui.buttons.NamedSimpleButton;
import io.github.thepro1604.advancedchatcore.interfaces.IClosable;
import io.github.thepro1604.advancedchatcore.util.FindType;
import io.github.thepro1604.advancedchathud.AdvancedChatHud;
import io.github.thepro1604.advancedchathud.config.ChatTab;
import io.github.thepro1604.advancedchathud.config.Match;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class MatchesEditor extends CoreGuiListBase<Match, WidgetMatchEntry, WidgetListMatches>
        implements IClosable {

    private final ChatTab tab;

    protected MatchesEditor(Screen parent, ChatTab tab) {
        super(10, 60);
        this.title = StringUtils.translate("advancedchathud.config.match.screen.name");
        this.tab = tab;
        this.setParent(parent);
    }

    @Override
    public void initGui() {
        super.initGui();
        int y = 26;
        int x = this.width - 10;
        this.addButton(x, y, "advancedchathud.gui.button.addmatch",
                (button, mouseButton) -> this.addMatch());
        this.addButton(Buttons.BACK.createButton(10, y), new BackButtonListener(this));
    }

    protected void addButton(int x, int y, String translation, IButtonActionListener listener) {
        ButtonGeneric button = new NamedSimpleButton(x, y, StringUtils.translate(translation), false);
        // Right aline
        this.addButton(button, listener);
    }

    @Override
    protected void closeGui(boolean showParent) {
        getListWidget().save();
        AdvancedChatHud.MAIN_CHAT_TAB.setUpTabs();
        super.closeGui(showParent);
    }

    @Override
    public void resize(Minecraft mc, int width, int height) {
        this.width = width;
        this.height = height;

        clearElements();
        initGui();
    }

    @Override
    protected WidgetListMatches createListWidget(int listX, int listY) {
        return new WidgetListMatches(
                listX, listY, this.getBrowserWidth(), this.getBrowserHeight(), null, tab, this);
    }

    public void addMatch() {
        tab.getMatches().add(new Match("I will match to text!", FindType.LITERAL));
        getListWidget().refreshEntries();
    }

    @Override
    public void close(ButtonBase button) {
        this.closeGui(true);
    }
}
