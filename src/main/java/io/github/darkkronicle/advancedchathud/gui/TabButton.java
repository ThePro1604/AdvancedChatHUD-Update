/*
 * Copyright (C) 2021 DarkKronicle
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.darkkronicle.advancedchathud.gui;

import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.darkkronicle.advancedchatcore.gui.CleanButton;
import io.github.darkkronicle.advancedchatcore.util.Color;
import io.github.darkkronicle.advancedchatcore.util.Colors;
import io.github.darkkronicle.advancedchatcore.util.TextUtil;
import io.github.darkkronicle.advancedchathud.AdvancedChatHud;
import io.github.darkkronicle.advancedchathud.config.HudConfigStorage;
import io.github.darkkronicle.advancedchathud.itf.IChatHud;
import io.github.darkkronicle.advancedchathud.tabs.AbstractChatTab;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;

public class TabButton extends CleanButton {

    private final AbstractChatTab tab;
    private static final int PADDING = 3;
    private static final int UNREAD_WIDTH = 9; // reserve 9px for unread

    private static final int WHITE = 0xFF_FF_FF_FF;
    private static final int GRAY = 0xFF_AA_AA_AA;
    private static final int RED = 0xFF_FF_55_55;

    private TabButton(AbstractChatTab tab, int x, int y, int width, int height) {
        super(x, y, width, height, tab.getMainColor(), tab.getAbbreviation());
        this.tab = tab;
        AdvancedChatHud.LOGGER.info("[TabButton] Created button for tab: " + tab.getName() + " (UUID: " + tab.getUuid() + ", Object: " + System.identityHashCode(tab) + ")");
    }

    public void render(DrawContext drawContext, int mouseX, int mouseY, boolean selected) {
        int relMX = mouseX - x;
        int relMY = mouseY - y;
        hovered = relMX >= 0 && relMX <= width && relMY >= 0 && relMY <= height;
        Color color = baseColor;
        if (hovered) {
            color = Colors.getInstance().getColorOrWhite("hover").withAlpha(color.alpha());
        }

        selected = false;
        if (HudConfigStorage.General.VANILLA_HUD.config.getBooleanValue()) {
            selected = IChatHud.getInstance().getTab().equals(tab);
        } else {
            ChatWindow window = WindowManager.getInstance().getSelected();
            if (window != null) {
                selected = window.getTab().equals(tab);
            }
        }
        if (!selected) {
            color = new Color(color.red() / 2, color.green() / 2, color.blue() / 2, 100);
        }

        // Use DrawContext directly instead of GuiContext wrapper
        drawContext.fill(x, y, x + width, y + height, color.color());

        drawContext.drawTextWithShadow(mc.textRenderer, displayString, x + PADDING, y + PADDING, selected ? WHITE : GRAY);
        if (tab.isShowUnread() && tab.getUnread() > 0) {
            String unread = TextUtil.toSuperscript(Math.min(tab.getUnread(), 99));
            int unreadX = x + width - ((UNREAD_WIDTH + PADDING) / 2) - 1;
            int unreadWidth = mc.textRenderer.getWidth(unread);
            drawContext.drawTextWithShadow(mc.textRenderer, unread, unreadX - unreadWidth / 2, y + PADDING, RED);
        }
    }

    @Override
    public boolean onMouseClicked(Click click, boolean doubled) {
        // Check if click is inside button bounds
        int clickX = (int) click.x();
        int clickY = (int) click.y();
        boolean inside = clickX >= x && clickX <= x + width && clickY >= y && clickY <= y + height;

        AdvancedChatHud.LOGGER.info("[TabButton] onMouseClicked called for tab: " + tab.getName() + " at position (" + x + "," + y + ") size (" + width + "x" + height + ") click at (" + clickX + "," + clickY + ") inside=" + inside);

        if (!inside) {
            return false;
        }

        this.mc
                .getSoundManager()
                .play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        WindowManager.getInstance().onTabButton(tab);
        return true;
    }

    public static TabButton fromTab(AbstractChatTab tab, int x, int y) {
        int width = StringUtils.getStringWidth(tab.getAbbreviation()) + PADDING * 2;
        if (tab.isShowUnread()) {
            width += UNREAD_WIDTH;
        }
        TabButton button = new TabButton(tab, x, y, width, PADDING + 8 + PADDING);
        AdvancedChatHud.LOGGER.info("[TabButton] fromTab created button at (" + x + "," + y + ") size " + width + "x" + (PADDING + 8 + PADDING));
        return button;
    }
}
