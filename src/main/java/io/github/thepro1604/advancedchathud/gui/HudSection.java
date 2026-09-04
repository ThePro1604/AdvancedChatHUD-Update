/*
 * Copyright (C) 2021 thepro1604
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.thepro1604.advancedchathud.gui;

import fi.dy.masa.malilib.config.options.ConfigColor;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.thepro1604.advancedchatcore.chat.AdvancedChatScreen;
import io.github.thepro1604.advancedchatcore.chat.ChatMessage;
import io.github.thepro1604.advancedchatcore.config.ConfigStorage;
import io.github.thepro1604.advancedchatcore.gui.ContextMenu;
import io.github.thepro1604.advancedchatcore.gui.IconButton;
import io.github.thepro1604.advancedchatcore.interfaces.AdvancedChatScreenSection;
import io.github.thepro1604.advancedchatcore.util.Color;
import io.github.thepro1604.advancedchatcore.util.RowList;
import io.github.thepro1604.advancedchatcore.util.TextBuilder;
import io.github.thepro1604.advancedchathud.AdvancedChatHud;
import io.github.thepro1604.advancedchathud.HudChatMessageHolder;
import io.github.thepro1604.advancedchathud.config.HudConfigStorage;
import io.github.thepro1604.advancedchathud.itf.IChatHud;
import io.github.thepro1604.advancedchathud.tabs.AbstractChatTab;
import io.github.thepro1604.advancedchathud.tabs.CustomChatTab;
import io.github.thepro1604.advancedchathud.util.TextUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.Level;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

@Environment(EnvType.CLIENT)
public class HudSection extends AdvancedChatScreenSection {

    // Sprite IDs — textures at assets/advancedchathud/textures/gui/sprites/chatwindow/*.png
    private static final Identifier ADD_ICON =
            Identifier.fromNamespaceAndPath(AdvancedChatHud.MOD_ID, "chatwindow/add_window");

    private static final Identifier RESET_ICON =
            Identifier.fromNamespaceAndPath(AdvancedChatHud.MOD_ID, "chatwindow/reset_windows");

    private ContextMenu menu = null;

    private ChatMessage message = null;

    private Component hoveredMenuEntry = null;
    private LinkedHashMap<Component, ContextMenu.ContextConsumer> menuOptions = null;

    public HudSection(AdvancedChatScreen screen) {
        super(screen);
    }

    private Color getColor() {
        Color baseColor;
        ChatWindow sel = WindowManager.getInstance().getSelected();
        if (sel == null) {
            baseColor = new Color(StringUtils.getColor(HudConfigStorage.MAIN_TAB.getInnerColor().config.getStringValue(), 0xFFFFFFFF));
        } else {
            baseColor = sel.getTab().getInnerColor();
        }
        return baseColor;
    }

    @Override
    public void initGui() {
        boolean left = !HudConfigStorage.General.TAB_BUTTONS_ON_RIGHT.config.getBooleanValue();
        List<AbstractChatTab> tabs = new ArrayList<>(AdvancedChatHud.MAIN_CHAT_TAB.getAllChatTabs());
        if (!left) {
            Collections.reverse(tabs);
        }
        RowList<ButtonBase> rows = left ? getScreen().getLeftSideButtons() : getScreen().getRightSideButtons();
        rows.createSection("tabs", 0);
        for (AbstractChatTab tab : tabs) {
            TabButton button = TabButton.fromTab(tab, 0, 0);
            rows.add("tabs", button);
        }
        IconButton window = new IconButton(0, 0, 14, 32, ADD_ICON, (button) -> WindowManager.getInstance().onTabAddButton(IChatHud.getInstance().getTab()));
        IconButton reset = new IconButton(0, 0, 14, 32, RESET_ICON, (button) -> WindowManager.getInstance().reset());
        if (left) {
            rows.add("tabs", window);
            rows.add("tabs", reset);
        } else {
            rows.add("tabs", window, 0);
            rows.add("tabs", reset, 0);
        }

        if (getScreen().getChatField().getValue().isEmpty()) {
            ChatWindow chatWindow = WindowManager.getInstance().getSelected();
            if (chatWindow == null) {
                return;
            }
            AbstractChatTab tab = chatWindow.getTab();
            if (tab instanceof CustomChatTab custom) {
                getScreen().getChatField().setValue(custom.getStartingMessage());
                getScreen().getChatField().moveCursorTo(custom.getStartingMessage().length(), false);
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        if (menu != null) {
            // Render context menu directly - replicate ContextMenu's render logic using GuiGraphicsExtractor
            renderContextMenuDirect(context, menu, mouseX, mouseY);
        }
    }

    private void renderContextMenuDirect(GuiGraphicsExtractor context, ContextMenu menu, int mouseX, int mouseY) {
        // Get menu properties via reflection since we can't directly access them
        try {
            java.lang.reflect.Field bgField = ContextMenu.class.getDeclaredField("background");
            bgField.setAccessible(true);
            Color background = (Color) bgField.get(menu);

            java.lang.reflect.Field hoverField = ContextMenu.class.getDeclaredField("hover");
            hoverField.setAccessible(true);
            Color hover = (Color) hoverField.get(menu);

            java.lang.reflect.Field optionsField = ContextMenu.class.getDeclaredField("options");
            optionsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            LinkedHashMap<Component, ContextMenu.ContextConsumer> options = (LinkedHashMap<Component, ContextMenu.ContextConsumer>) optionsField.get(menu);

            // Store options for click handling
            menuOptions = options;

            int x = menu.getX();
            int y = menu.getY();
            int width = menu.getWidth();
            int height = menu.getHeight();

            Minecraft mc = Minecraft.getInstance();

            // Draw background
            context.fill(x, y, x + width, y + height, background.color());

            int rX = x + 2;
            int rY = y + 2;

            // Reset hovered entry
            hoveredMenuEntry = null;

            // Draw each option
            for (Component option : options.keySet()) {
                if (mouseX >= x && mouseX <= x + width && mouseY >= rY - 2 && mouseY < rY + mc.font.lineHeight + 1) {
                    // Draw hover highlight
                    context.fill(rX - 2, rY - 2, rX - 2 + width, rY - 2 + mc.font.lineHeight + 2, hover.color());
                    hoveredMenuEntry = option;
                }
                context.text(mc.font, option, rX, rY, -1, true);
                rY += mc.font.lineHeight + 2;
            }
        } catch (Exception e) {
            AdvancedChatHud.LOGGER.error("[HudSection] Failed to render context menu: " + e.getMessage());
        }
    }

    public void createContextMenu(int mouseX, int mouseY) {
        LinkedHashMap<Component, ContextMenu.ContextConsumer> actions = new LinkedHashMap<>();
        message = WindowManager.getInstance().getMessage(mouseX, mouseY);
        if (message != null) {
            TextBuilder data = new TextBuilder();
            try {
                data.append(
                        message.getTime().format(DateTimeFormatter.ofPattern(ConfigStorage.General.TIME_FORMAT.config.getStringValue())), Style.EMPTY.withColor(ChatFormatting.AQUA)
                );
            } catch (IllegalArgumentException e) {
                AdvancedChatHud.LOGGER.log(Level.WARN, "Can't format time for context menu!", e);
            }
            if (message.getOwner() != null) {
                data.append(" - ", Style.EMPTY.withColor(ChatFormatting.GRAY));
                if (message.getOwner().getEntry().getTabListDisplayName() != null) {
                    data.append(message.getOwner().getEntry().getTabListDisplayName());
                } else {
                    data.append(message.getOwner().getEntry().getProfile().name());
                }
            }
            if (!data.build().getString().isBlank())  {
                actions.put(data.build(), (x, y) -> {
                    InfoUtils.printActionbarMessage("advancedchathud.context.nothing");
                });
            }
            actions.put(Component.literal(StringUtils.translate("advancedchathud.context.copy")), (x, y) -> {
                Minecraft.getInstance().keyboardHandler.setClipboard(message.getOriginalText().getString());
                InfoUtils.printActionbarMessage("advancedchathud.context.copied");
            });
            actions.put(Component.literal(StringUtils.translate("advancedchathud.context.copyhex")), (x, y) -> {
                String hexText = TextUtil.toStringWithHexColors(message.getOriginalText());
                Minecraft.getInstance().keyboardHandler.setClipboard(hexText);
                InfoUtils.printActionbarMessage("advancedchathud.context.copied");
            });
            actions.put(Component.literal(StringUtils.translate("advancedchathud.context.delete")), (x, y) -> {
                HudChatMessageHolder.getInstance().removeChatMessage(message);
            });
            if (message.getOwner() != null) {
                actions.put(Component.literal(StringUtils.translate("advancedchathud.context.messageowner")), (x, y) -> {
                    getScreen().getChatField().setValue("/msg " + message.getOwner().getEntry().getProfile().name() + " ");
                });
            }
        }
        ChatWindow hovered = WindowManager.getInstance().getHovered(mouseX, mouseY);
        actions.put(Component.literal(StringUtils.translate("advancedchathud.context.removeallwindows")), (x, y) -> WindowManager.getInstance().reset());
        actions.put(Component.literal(StringUtils.translate("advancedchathud.context.clearallmessages")), (x, y) -> WindowManager.getInstance().clear());
        if (hovered != null) {
            actions.put(Component.literal(StringUtils.translate("advancedchathud.context.duplicatewindow")), (x, y) -> WindowManager.getInstance().duplicateTab(hovered, x, y));
            actions.put(Component.literal(StringUtils.translate("advancedchathud.context.configurewindow")), (x, y) -> WindowManager.getInstance().configureTab(getScreen(), hovered));
            actions.put(Component.literal(StringUtils.translate("advancedchathud.context.minimalist")), (x, y) -> hovered.toggleMinimalist());
        }
        menu = new ContextMenu(mouseX, mouseY, actions, () -> menu = null);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        // Handle context menu clicks manually
        if (menu != null) {
            if (button == 0) {
                // Left click - check if we clicked on a menu item
                if (hoveredMenuEntry != null && menuOptions != null) {
                    ContextMenu.ContextConsumer action = menuOptions.get(hoveredMenuEntry);
                    if (action != null) {
                        action.takeAction(menu.getContextX(), menu.getContextY());
                        menu = null;
                        menuOptions = null;
                        hoveredMenuEntry = null;
                        return true;
                    }
                }
            }
            // Any click (even outside) closes the menu
            menu = null;
            menuOptions = null;
            hoveredMenuEntry = null;
            return true;
        }

        if (button == 1) {
            createContextMenu((int) mouseX, (int) mouseY);
            return true;
        }

        boolean result = WindowManager.getInstance().mouseClicked(getScreen(), mouseX, mouseY, button);
        return result;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        double mouseX = click.x();
        double mouseY = click.y();
        int mouseButton = click.button();

        return WindowManager.getInstance().mouseReleased(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        return WindowManager.getInstance().mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (amount > 1.0D) {
            amount = 1.0D;
        }

        if (amount < -1.0D) {
            amount = -1.0D;
        }
        // Note: Screen.hasShiftDown() is now accessed differently in Minecraft 1.21+
        // For now, always use the non-shift behavior
        // if (!Screen.hasShiftDown()) {
            amount *= 7.0D;
        // }

        return WindowManager.getInstance().scroll(amount, mouseX, mouseY);
    }
}
