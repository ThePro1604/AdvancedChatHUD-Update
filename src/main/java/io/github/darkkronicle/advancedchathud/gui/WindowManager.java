/*
 * Copyright (C) 2022 DarkKronicle
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.darkkronicle.advancedchathud.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.interfaces.IRenderer;
import io.github.darkkronicle.advancedchatcore.chat.AdvancedChatScreen;
import io.github.darkkronicle.advancedchatcore.chat.ChatMessage;
import io.github.darkkronicle.advancedchatcore.util.SyncTaskQueue;
import io.github.darkkronicle.advancedchathud.AdvancedChatHud;
import io.github.darkkronicle.advancedchathud.HudChatMessage;
import io.github.darkkronicle.advancedchathud.ResolutionEventHandler;
import io.github.darkkronicle.advancedchathud.config.HudConfigStorage;
import io.github.darkkronicle.advancedchathud.config.gui.ChatWindowEditor;
import io.github.darkkronicle.advancedchathud.itf.IChatHud;
import io.github.darkkronicle.advancedchathud.tabs.AbstractChatTab;
import io.github.darkkronicle.advancedchathud.tabs.CustomChatTab;
import io.github.darkkronicle.advancedchathud.tabs.MainChatTab;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class WindowManager implements IRenderer, ResolutionEventHandler {

    private static final WindowManager INSTANCE = new WindowManager();
    private final Minecraft client;
    private final List<ChatWindow> windows = new ArrayList<>(8);
    private int dragX = 0;
    private int dragY = 0;
    private ChatWindow drag = null;
    private boolean resize = false;

    public static WindowManager getInstance() {
        return INSTANCE;
    }

    private WindowManager() {
        client = Minecraft.getInstance();
    }

    public void reset() {
        windows.clear();
    }

    private void addWindow(ChatWindow window) {
        // Remove duplicates being spawned from somewhere
        windows.removeIf(w -> w == window);
        windows.add(window);
    }

    public void loadFromJson(JsonArray array) {
        reset();
        if (!HudConfigStorage.General.VANILLA_HUD.config.getBooleanValue()) {
            if (array == null || array.size() == 0) {
                ChatWindow base = new ChatWindow(AdvancedChatHud.MAIN_CHAT_TAB);
                base.setSelected(true);
                addWindow(base);
                return;
            }
        } else {
            if (array == null || array.size() == 0) {
                return;
            }
        }
        ChatWindow.ChatWindowSerializer serializer = new ChatWindow.ChatWindowSerializer();
        for (JsonElement e : array) {
            if (!e.isJsonObject()) {
                continue;
            }
            ChatWindow w;
            try {
                w = serializer.load(e.getAsJsonObject());
                if (w == null) {
                    continue;
                }
            } catch (Exception err) {
                AdvancedChatHud.LOGGER.error("Error while loading in ChatWindow ", err);
                continue;
            }
            addWindow(w);
        }
    }

    public JsonArray saveJson() {
        JsonArray array = new JsonArray();
        ChatWindow.ChatWindowSerializer serializer = new ChatWindow.ChatWindowSerializer();
        for (ChatWindow w : windows) {
            array.add(serializer.save(w));
        }
        return array;
    }

    public void onRenderGameOverlayPost(GuiGraphicsExtractor drawContext, Minecraft mc, float partialTicks) {
        boolean isFocused = isChatFocused();
        int ticks = client.gui.getGuiTicks();
        if (!HudConfigStorage.General.RENDER_IN_OTHER_GUI.config.getBooleanValue() && !isFocused && client.screen != null) {
            return;
        }
        for (int i = windows.size() - 1; i >= 0; i--) {
            windows.get(i).render(drawContext, ticks, isFocused);
        }

        // Render hover tooltips for chat text even when chat is not focused
        renderHoverTooltip(drawContext);
    }

    // Overload for direct calls from Mixin
    public void onRenderGameOverlayPost(GuiGraphicsExtractor drawContext) {
        onRenderGameOverlayPost(drawContext, client, 0.0f);
    }

    private void renderHoverTooltip(GuiGraphicsExtractor drawContext) {
        if (client.screen != null) {
            // Don't render tooltips when a screen is open (unless it's the chat screen)
            if (!(client.screen instanceof io.github.darkkronicle.advancedchatcore.chat.AdvancedChatScreen)) {
                return;
            }
        }

        // Get mouse position
        double mouseX = client.mouseHandler.xpos() * (double) client.getWindow().getGuiScaledWidth() / (double) client.getWindow().getWidth();
        double mouseY = client.mouseHandler.ypos() * (double) client.getWindow().getGuiScaledHeight() / (double) client.getWindow().getHeight();

        // Find the style at the mouse position
        Style style = getTextIgnoreFocus(mouseX, mouseY);
        if (style != null && style.getHoverEvent() != null) {
            renderStyleHoverEffect(drawContext, style, (int) mouseX, (int) mouseY);
        }
    }

    /**
     * Render hover effect for a style (tooltip for text, item, entity, etc.)
     */
    private void renderStyleHoverEffect(GuiGraphicsExtractor drawContext, Style style, int x, int y) {
        io.github.darkkronicle.advancedchatcore.util.ChatHudHelper.renderHoverTooltip(drawContext, style, x, y);
    }

    /**
     * Split formatted Component into lines while preserving all formatting (colors, bold, etc.)
     */
    private java.util.List<net.minecraft.util.FormattedCharSequence> splitFormattedTextIntoLines(net.minecraft.network.chat.Component text) {
        java.util.List<net.minecraft.util.FormattedCharSequence> result = new java.util.ArrayList<>();
        java.util.List<net.minecraft.network.chat.Component> textLines = new java.util.ArrayList<>();

        // Recursively split the Component by newlines while preserving formatting
        splitTextRecursive(text, textLines, net.minecraft.network.chat.Component.empty());

        // Convert each Component line to FormattedCharSequence
        for (net.minecraft.network.chat.Component line : textLines) {
            result.add(line.getVisualOrderText());
        }

        return result;
    }

    /**
     * Recursively process Component and split by newlines while preserving all formatting
     */
    private void splitTextRecursive(net.minecraft.network.chat.Component text, java.util.List<net.minecraft.network.chat.Component> lines,
                                     net.minecraft.network.chat.MutableComponent currentLine) {
        // Visit each Component component
        text.visit((style, str) -> {
            if (str.contains("\n")) {
                // Split by newlines
                String[] parts = str.split("\n", -1);
                for (int i = 0; i < parts.length; i++) {
                    if (i > 0) {
                        // Finish current line and start new one
                        lines.add(currentLine.copy());
                        currentLine.getSiblings().clear();
                        currentLine.setStyle(net.minecraft.network.chat.Style.EMPTY);
                    }
                    if (!parts[i].isEmpty()) {
                        currentLine.append(net.minecraft.network.chat.Component.literal(parts[i]).setStyle(style));
                    }
                }
            } else {
                currentLine.append(net.minecraft.network.chat.Component.literal(str).setStyle(style));
            }
            return java.util.Optional.empty();
        }, text.getStyle());

        // Add the last line if it has content
        if (!currentLine.getString().isEmpty() || lines.isEmpty()) {
            lines.add(currentLine);
        }
    }

    /**
     * Get Component at position without requiring chat to be focused
     */
    private Style getTextIgnoreFocus(double mouseX, double mouseY) {
        for (ChatWindow w : windows) {
            if (w.isMouseOver(mouseX, mouseY)) {
                return w.getTextIgnoreFocus(mouseX, mouseY);
            }
        }
        return null;
    }

    public void resetScroll() {
        for (ChatWindow w : windows) {
            w.resetScroll();
        }
    }

    public boolean scroll(double amount, double mouseX, double mouseY) {
        for (ChatWindow w : windows) {
            // Prioritize mouse over first
            if (w.isMouseOver(mouseX, mouseY)) {
                w.scroll(amount);
                return true;
            }
        }
        for (ChatWindow w : windows) {
            if (w.isSelected()) {
                w.scroll(amount);
                return true;
            }
        }
        return false;
    }

    public Style getText(double mouseX, double mouseY) {
        for (ChatWindow w : windows) {
            if (w.isMouseOver(mouseX, mouseY)) {
                return w.getText(mouseX, mouseY);
            }
        }
        return null;
    }

    public ChatMessage getMessage(double mouseX, double mouseY) {
        for (ChatWindow w : windows) {
            if (w.isMouseOver(mouseX, mouseY)) {
                return w.getMessage(mouseX, mouseY);
            }
        }
        return null;
    }

    public boolean isChatFocused() {
        return this.client.screen instanceof AdvancedChatScreen;
    }

    public ChatWindow getSelected() {
        for (ChatWindow w : windows) {
            if (w.isSelected()) {
                return w;
            }
        }
        return null;
    }

    public void unSelect() {
        for (ChatWindow w : windows) {
            w.setSelected(false);
        }
    }

    public void setSelected(ChatWindow window) {
        for (ChatWindow w : windows) {
            w.setSelected(window.equals(w));
        }
        windows.removeIf(w -> w == window);
        windows.add(0, window);

        if (!HudConfigStorage.General.CHANGE_START_MESSAGE.config.getBooleanValue() || !(client.screen instanceof AdvancedChatScreen screen)) {
            return;
        }
        if (window.getTab() instanceof MainChatTab) {
            for (ChatWindow w : windows) {
                if (w.getTab() instanceof CustomChatTab tab2) {
                    if (screen.getChatField().getValue().startsWith(tab2.getStartingMessage()) && tab2.getStartingMessage().length() > 0) {
                        screen.getChatField().setValue(screen.getChatField().getValue().substring(tab2.getStartingMessage().length()));
                        break;
                    }
                }
            }
        } else if (window.getTab() instanceof CustomChatTab tab) {
            boolean replaced = false;

            for (ChatWindow w : windows) {
                if (w.getTab() instanceof CustomChatTab tab2) {
                    if (screen.getChatField().getValue().startsWith(tab2.getStartingMessage()) && tab2.getStartingMessage().length() > 0) {
                        screen.getChatField().setValue(tab.getStartingMessage() + screen.getChatField().getValue().substring(tab2.getStartingMessage().length()));

                        replaced = true;

                        break;
                    }
                }
            }

            if (!replaced) {
                screen.getChatField().setValue(tab.getStartingMessage() + screen.getChatField().getValue());
            }
        }
    }

    public boolean mouseClicked(Screen screen, double mouseX, double mouseY, int button) {
        ChatWindow over = null;
        for (ChatWindow w : windows) {
            if (w.isMouseOver(mouseX, mouseY)) {
                over = w;
                break;
            }
        }
        if (over == null) {
            if (HudConfigStorage.General.VANILLA_HUD.config.getBooleanValue()
                    && overVanillaHud(mouseX, mouseY)) {
                unSelect();
            }
            return false;
        }
        if (button == 0) {
            setSelected(over);
            if (over.isMouseOverDragBar(mouseX, mouseY)) {
                drag = over;
                dragX = (int) mouseX - over.getConvertedX();
                dragY = (int) mouseY - over.getConvertedY();
                resize = false;
                return true;
            } else if (over.isMouseOverResize(mouseX, mouseY)) {
                drag = over;
                dragX = (int) mouseX - over.getConvertedWidth();
                dragY = (int) mouseY + over.getConvertedHeight();
                resize = true;
                return true;
            }
            Style style = over.getText(mouseX, mouseY);
            // Handle Component click - open URLs, run commands, etc.
            if (style != null) {
                if (handleStyleClick(style, screen)) {
                    return true;
                }
            }
            if (over.onMouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        // Don't consume the click if nothing was handled - allow it to pass through
        return false;
    }

    private boolean overVanillaHud(double mouseX, double mouseY) {
        return IChatHud.getInstance().isOver(mouseX, mouseY);
    }

    public boolean mouseDragged(
            double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (drag != null && !resize) {
            int x = Math.max((int) mouseX - dragX, 0);
            int y = Math.max((int) mouseY - dragY, drag.getActualHeight());
            x = Math.min(x, client.getWindow().getGuiScaledWidth() - drag.getConvertedWidth());
            y = Math.min(y, client.getWindow().getGuiScaledHeight());
            drag.setPosition(x, y);
            return true;
        } else if (drag != null) {
            int width = Math.max((int) mouseX - dragX, 80);
            int height = Math.max(dragY - (int) mouseY, 40);
            drag.setDimensions(width, height);
            return true;
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        if (drag != null) {
            drag = null;
            return true;
        }
        return false;
    }

    public void onTabButton(AbstractChatTab tab) {
        ChatWindow selected = null;
        for (ChatWindow w : windows) {
            if (w.isSelected()) {
                selected = w;
                w.setTab(tab);
                return;
            }
        }
        // Set it if no other window is selected
        IChatHud.getInstance().setTab(tab);
    }

    public void onTabAddButton(AbstractChatTab tab) {
        ChatWindow window = new ChatWindow(tab);
        ChatWindow sel = getSelected();
        if (sel == null) {
            sel = window;
        }
        window.setPosition(sel.getConvertedX() + 15, sel.getConvertedY() + 15);
//        windows.add(window);
        setSelected(window);
    }

    public void deleteWindow(ChatWindow chatWindow) {
        windows.remove(chatWindow);
        if (!windows.isEmpty()) {
            for (ChatWindow w : windows) {
                w.setSelected(false);
            }
            windows.get(0).setSelected(true);
        }
    }

    public void onStackedMessage(HudChatMessage message) {
        for (ChatWindow w : windows) {
            w.stackMessage(message);
        }
    }

    public void onNewMessage(HudChatMessage message) {
        IChatHud.getInstance().addMessage(message);
        for (ChatWindow w : windows) {
            w.addMessage(message);
        }
    }

    public void clear() {
        IChatHud.getInstance().clear(false);
        for (ChatWindow w : windows) {
            w.clearLines();
        }
    }

    @Override
    public void onResolutionChange() {
        // Delay resolution change because when toggling full screen it can take a render cycle for it to apply
        SyncTaskQueue.getInstance().add(2, () -> {
            for (ChatWindow w : windows) {
                w.onResolutionChange();
            }
        });
    }

    public void onRemoveMessage(ChatMessage remove) {
        IChatHud.getInstance().removeMessage(remove);
        for (ChatWindow w : windows) {
            w.removeMessage(remove);
        }
    }

    public void duplicateTab(ChatWindow hovered, int x, int y) {
        ChatWindow window = new ChatWindow(IChatHud.getInstance().getTab());
        window.setRelativeDimensions(hovered.getWidthPercent(), hovered.getHeightPercent());
        window.setVisibility(hovered.getVisibility());
        window.setPosition(x, y);
        setSelected(window);
    }

    public ChatWindow getHovered(int x, int y) {
        int windowHeight = client.getWindow().getGuiScaledHeight();
        for (ChatWindow w : windows) {
            int wX = w.getConvertedX();
            int wY = w.getConvertedY();
            if (x >= wX && x <= wX + w.getConvertedWidth() && y <= wY && y >= wY - w.getConvertedHeight()) {
                return w;
            }
        }
        return null;
    }

    public void configureTab(AdvancedChatScreen screen, ChatWindow window) {
        GuiBase.openGui(new ChatWindowEditor(screen, window));
    }

    /**
     * Handle clicking on a style (for URLs, commands, etc.)
     * Uses 26.1 sealed ClickEvent interface with direct instanceof dispatch.
     */
    private boolean handleStyleClick(Style style, Screen screen) {
        if (style == null) {
            return false;
        }
        net.minecraft.network.chat.ClickEvent event = style.getClickEvent();
        if (event == null) {
            return false;
        }

        if (event instanceof net.minecraft.network.chat.ClickEvent.RunCommand cmd) {
            String command = cmd.command();
            if (command.startsWith("/")) command = command.substring(1);
            if (client.player != null && client.player.connection != null) {
                client.player.connection.sendCommand(command);
            }
            if (screen instanceof AdvancedChatScreen) {
                client.setScreen(null);
            }
            return true;
        } else if (event instanceof net.minecraft.network.chat.ClickEvent.OpenUrl openUrl) {
            java.net.URI uri = openUrl.uri();
            String scheme = uri.getScheme();
            // Screenshot workaround: https://YYYY-MM-DD_HH.MM.SS.png was corrupted from a file URI
            if ("https".equalsIgnoreCase(scheme)) {
                String uriStr = uri.toString();
                if (uriStr.startsWith("https://") && uriStr.endsWith(".png")) {
                    String filename = uriStr.substring(8);
                    if (filename.matches("\\d{4}-\\d{2}-\\d{2}_\\d{2}\\.\\d{2}\\.\\d{2}\\.png")) {
                        java.io.File screenshotFile = new java.io.File(
                                new java.io.File(client.gameDirectory, "screenshots"), filename);
                        if (screenshotFile.exists()) {
                            net.minecraft.util.Util.getPlatform().openFile(screenshotFile);
                            return true;
                        }
                    }
                }
            }
            if ("file".equalsIgnoreCase(scheme)) {
                net.minecraft.util.Util.getPlatform().openFile(new java.io.File(uri));
                return true;
            }
            net.minecraft.util.Util.getPlatform().openUri(uri);
            return true;
        } else if (event instanceof net.minecraft.network.chat.ClickEvent.OpenFile openFile) {
            net.minecraft.util.Util.getPlatform().openFile(openFile.file());
            return true;
        } else if (event instanceof net.minecraft.network.chat.ClickEvent.SuggestCommand suggest) {
            if (screen instanceof AdvancedChatScreen chatScreen) {
                chatScreen.getChatField().setValue(suggest.command());
            } else {
                client.setScreen(new AdvancedChatScreen(suggest.command()));
            }
            return true;
        } else if (event instanceof net.minecraft.network.chat.ClickEvent.CopyToClipboard copy) {
            client.keyboardHandler.setClipboard(copy.value());
            return true;
        }

        return false;
    }
}
