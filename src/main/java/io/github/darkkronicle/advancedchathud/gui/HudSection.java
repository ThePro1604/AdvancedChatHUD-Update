/*
 * Copyright (C) 2021 DarkKronicle
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.darkkronicle.advancedchathud.gui;

import fi.dy.masa.malilib.config.options.ConfigColor;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.darkkronicle.advancedchatcore.chat.AdvancedChatScreen;
import io.github.darkkronicle.advancedchatcore.chat.ChatMessage;
import io.github.darkkronicle.advancedchatcore.config.ConfigStorage;
import io.github.darkkronicle.advancedchatcore.gui.ContextMenu;
import io.github.darkkronicle.advancedchatcore.gui.IconButton;
import io.github.darkkronicle.advancedchatcore.interfaces.AdvancedChatScreenSection;
import io.github.darkkronicle.advancedchatcore.util.Color;
import io.github.darkkronicle.advancedchatcore.util.RowList;
import io.github.darkkronicle.advancedchatcore.util.TextBuilder;
import io.github.darkkronicle.advancedchathud.AdvancedChatHud;
import io.github.darkkronicle.advancedchathud.HudChatMessageHolder;
import io.github.darkkronicle.advancedchathud.config.HudConfigStorage;
import io.github.darkkronicle.advancedchathud.itf.IChatHud;
import io.github.darkkronicle.advancedchathud.tabs.AbstractChatTab;
import io.github.darkkronicle.advancedchathud.tabs.CustomChatTab;
import io.github.darkkronicle.advancedchathud.util.TextUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.Level;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

@Environment(EnvType.CLIENT)
public class HudSection extends AdvancedChatScreenSection {

    private static final Identifier ADD_ICON =
            Identifier.of(AdvancedChatHud.MOD_ID, "textures/gui/chatwindow/add_window.png");

    private static final Identifier RESET_ICON =
            Identifier.of(AdvancedChatHud.MOD_ID, "textures/gui/chatwindow/reset_windows.png");

    private ContextMenu menu = null;

    private ChatMessage message = null;

    private Text hoveredMenuEntry = null;
    private LinkedHashMap<Text, ContextMenu.ContextConsumer> menuOptions = null;

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

        if (getScreen().getChatField().getText().isEmpty()) {
            ChatWindow chatWindow = WindowManager.getInstance().getSelected();
            if (chatWindow == null) {
                return;
            }
            AbstractChatTab tab = chatWindow.getTab();
            if (tab instanceof CustomChatTab custom) {
                getScreen().getChatField().setText(custom.getStartingMessage());
                getScreen().getChatField().setCursor(custom.getStartingMessage().length(), false);
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        if (menu != null) {
            // Render context menu directly - replicate ContextMenu's render logic using DrawContext
            renderContextMenuDirect(context, menu, mouseX, mouseY);
        }
    }

    private void renderContextMenuDirect(DrawContext context, ContextMenu menu, int mouseX, int mouseY) {
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
            LinkedHashMap<Text, ContextMenu.ContextConsumer> options = (LinkedHashMap<Text, ContextMenu.ContextConsumer>) optionsField.get(menu);

            // Store options for click handling
            menuOptions = options;

            int x = menu.getX();
            int y = menu.getY();
            int width = menu.getWidth();
            int height = menu.getHeight();

            MinecraftClient mc = MinecraftClient.getInstance();

            // Draw background
            context.fill(x, y, x + width, y + height, background.color());

            int rX = x + 2;
            int rY = y + 2;

            // Reset hovered entry
            hoveredMenuEntry = null;

            // Draw each option
            for (Text option : options.keySet()) {
                if (mouseX >= x && mouseX <= x + width && mouseY >= rY - 2 && mouseY < rY + mc.textRenderer.fontHeight + 1) {
                    // Draw hover highlight
                    context.fill(rX - 2, rY - 2, rX - 2 + width, rY - 2 + mc.textRenderer.fontHeight + 2, hover.color());
                    hoveredMenuEntry = option;
                }
                context.drawTextWithShadow(mc.textRenderer, option, rX, rY, -1);
                rY += mc.textRenderer.fontHeight + 2;
            }
        } catch (Exception e) {
            AdvancedChatHud.LOGGER.error("[HudSection] Failed to render context menu: " + e.getMessage());
        }
    }

    public void createContextMenu(int mouseX, int mouseY) {
        LinkedHashMap<Text, ContextMenu.ContextConsumer> actions = new LinkedHashMap<>();
        message = WindowManager.getInstance().getMessage(mouseX, mouseY);
        if (message != null) {
            TextBuilder data = new TextBuilder();
            try {
                data.append(
                        message.getTime().format(DateTimeFormatter.ofPattern(ConfigStorage.General.TIME_FORMAT.config.getStringValue())), Style.EMPTY.withFormatting(Formatting.AQUA)
                );
            } catch (IllegalArgumentException e) {
                AdvancedChatHud.LOGGER.log(Level.WARN, "Can't format time for context menu!", e);
            }
            if (message.getOwner() != null) {
                data.append(" - ", Style.EMPTY.withFormatting(Formatting.GRAY));
                if (message.getOwner().getEntry().getDisplayName() != null) {
                    data.append(message.getOwner().getEntry().getDisplayName());
                } else {
                    data.append(message.getOwner().getEntry().getProfile().name());
                }
            }
            if (!data.build().getString().isBlank())  {
                actions.put(data.build(), (x, y) -> {
                    InfoUtils.printActionbarMessage("advancedchathud.context.nothing");
                });
            }
            actions.put(Text.literal(StringUtils.translate("advancedchathud.context.copy")), (x, y) -> {
                MinecraftClient.getInstance().keyboard.setClipboard(message.getOriginalText().getString());
                InfoUtils.printActionbarMessage("advancedchathud.context.copied");
            });
            actions.put(Text.literal(StringUtils.translate("advancedchathud.context.copyhex")), (x, y) -> {
                String hexText = TextUtil.toStringWithHexColors(message.getOriginalText());
                MinecraftClient.getInstance().keyboard.setClipboard(hexText);
                InfoUtils.printActionbarMessage("advancedchathud.context.copied");
            });
            actions.put(Text.literal(StringUtils.translate("advancedchathud.context.delete")), (x, y) -> {
                HudChatMessageHolder.getInstance().removeChatMessage(message);
            });
            if (message.getOwner() != null) {
                actions.put(Text.literal(StringUtils.translate("advancedchathud.context.messageowner")), (x, y) -> {
                    getScreen().getChatField().setText("/msg " + message.getOwner().getEntry().getProfile().name() + " ");
                });
            }
        }
        ChatWindow hovered = WindowManager.getInstance().getHovered(mouseX, mouseY);
        actions.put(Text.literal(StringUtils.translate("advancedchathud.context.removeallwindows")), (x, y) -> WindowManager.getInstance().reset());
        actions.put(Text.literal(StringUtils.translate("advancedchathud.context.clearallmessages")), (x, y) -> WindowManager.getInstance().clear());
        if (hovered != null) {
            actions.put(Text.literal(StringUtils.translate("advancedchathud.context.duplicatewindow")), (x, y) -> WindowManager.getInstance().duplicateTab(hovered, x, y));
            actions.put(Text.literal(StringUtils.translate("advancedchathud.context.configurewindow")), (x, y) -> WindowManager.getInstance().configureTab(getScreen(), hovered));
            actions.put(Text.literal(StringUtils.translate("advancedchathud.context.minimalist")), (x, y) -> hovered.toggleMinimalist());
        }
        menu = new ContextMenu(mouseX, mouseY, actions, () -> menu = null);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
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
    public boolean mouseReleased(Click click) {
        double mouseX = click.x();
        double mouseY = click.y();
        int mouseButton = click.button();

        return WindowManager.getInstance().mouseReleased(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
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
