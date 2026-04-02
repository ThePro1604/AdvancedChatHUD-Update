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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Style;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class WindowManager implements IRenderer, ResolutionEventHandler {

    private static final WindowManager INSTANCE = new WindowManager();
    private final MinecraftClient client;
    private final List<ChatWindow> windows = new ArrayList<>(8);
    private int dragX = 0;
    private int dragY = 0;
    private ChatWindow drag = null;
    private boolean resize = false;

    public static WindowManager getInstance() {
        return INSTANCE;
    }

    private WindowManager() {
        client = MinecraftClient.getInstance();
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

    public void onRenderGameOverlayPost(DrawContext drawContext) {
        boolean isFocused = isChatFocused();
        int ticks = client.inGameHud.getTicks();
        if (!HudConfigStorage.General.RENDER_IN_OTHER_GUI.config.getBooleanValue() && !isFocused && client.currentScreen != null) {
            return;
        }
        for (int i = windows.size() - 1; i >= 0; i--) {
            windows.get(i).render(drawContext, ticks, isFocused);
        }

        // Render hover tooltips for chat text even when chat is not focused
        renderHoverTooltip(drawContext);
    }

    private void renderHoverTooltip(DrawContext drawContext) {
        if (client.currentScreen != null) {
            // Don't render tooltips when a screen is open (unless it's the chat screen)
            if (!(client.currentScreen instanceof io.github.darkkronicle.advancedchatcore.chat.AdvancedChatScreen)) {
                return;
            }
        }

        // Get mouse position
        double mouseX = client.mouse.getX() * (double) client.getWindow().getScaledWidth() / (double) client.getWindow().getWidth();
        double mouseY = client.mouse.getY() * (double) client.getWindow().getScaledHeight() / (double) client.getWindow().getHeight();

        // Find the style at the mouse position
        Style style = getTextIgnoreFocus(mouseX, mouseY);
        if (style != null && style.getHoverEvent() != null) {
            renderStyleHoverEffect(drawContext, style, (int) mouseX, (int) mouseY);
        }
    }

    /**
     * Render hover effect for a style (tooltip for text, item, entity, etc.)
     * This properly handles all hover event types including formatted text, items, and entities
     */
    private void renderStyleHoverEffect(DrawContext drawContext, Style style, int x, int y) {
        net.minecraft.text.HoverEvent hoverEvent = style.getHoverEvent();
        if (hoverEvent == null) {
            return;
        }

        // HoverEvent in 1.21 is a sealed interface with ShowText, ShowItem, and ShowEntity implementations
        // We need to use reflection to access the obfuscated record component accessors
        try {
            // Look for comp_XXXX methods that return Text or ItemStack
            for (java.lang.reflect.Method method : hoverEvent.getClass().getMethods()) {
                if (method.getParameterCount() == 0 && method.getName().startsWith("comp_")) {
                    Class<?> returnType = method.getReturnType();

                    // Check if this returns Text (ShowText hover)
                    if (net.minecraft.text.Text.class.isAssignableFrom(returnType)) {
                        net.minecraft.text.Text text = (net.minecraft.text.Text) method.invoke(hoverEvent);
                        if (text != null) {
                            // Split text into lines and render with proper formatting
                            java.util.List<net.minecraft.text.OrderedText> lines = splitFormattedTextIntoLines(text);
                            drawContext.drawOrderedTooltip(client.textRenderer, lines, x, y);
                        }
                        return;
                    }

                    // Check if this returns ItemStack (ShowItem hover)
                    if (net.minecraft.item.ItemStack.class.isAssignableFrom(returnType)) {
                        net.minecraft.item.ItemStack itemStack = (net.minecraft.item.ItemStack) method.invoke(hoverEvent);
                        if (itemStack != null) {
                            drawContext.drawItemTooltip(client.textRenderer, itemStack, x, y);
                        }
                        return;
                    }
                }
            }
        } catch (Exception e) {
            AdvancedChatHud.LOGGER.error("[WindowManager] Failed to render hover tooltip: " + e.getMessage(), e);
        }
    }

    /**
     * Split formatted Text into lines while preserving all formatting (colors, bold, etc.)
     */
    private java.util.List<net.minecraft.text.OrderedText> splitFormattedTextIntoLines(net.minecraft.text.Text text) {
        java.util.List<net.minecraft.text.OrderedText> result = new java.util.ArrayList<>();
        java.util.List<net.minecraft.text.Text> textLines = new java.util.ArrayList<>();

        // Recursively split the text by newlines while preserving formatting
        splitTextRecursive(text, textLines, net.minecraft.text.Text.empty());

        // Convert each Text line to OrderedText
        for (net.minecraft.text.Text line : textLines) {
            result.add(line.asOrderedText());
        }

        return result;
    }

    /**
     * Recursively process Text and split by newlines while preserving all formatting
     */
    private void splitTextRecursive(net.minecraft.text.Text text, java.util.List<net.minecraft.text.Text> lines,
                                     net.minecraft.text.MutableText currentLine) {
        // Visit each text component
        text.visit((style, str) -> {
            if (str.contains("\n")) {
                // Split by newlines
                String[] parts = str.split("\n", -1);
                for (int i = 0; i < parts.length; i++) {
                    if (i > 0) {
                        // Finish current line and start new one
                        lines.add(currentLine.copy());
                        currentLine.getSiblings().clear();
                        currentLine.setStyle(net.minecraft.text.Style.EMPTY);
                    }
                    if (!parts[i].isEmpty()) {
                        currentLine.append(net.minecraft.text.Text.literal(parts[i]).setStyle(style));
                    }
                }
            } else {
                currentLine.append(net.minecraft.text.Text.literal(str).setStyle(style));
            }
            return java.util.Optional.empty();
        }, text.getStyle());

        // Add the last line if it has content
        if (!currentLine.getString().isEmpty() || lines.isEmpty()) {
            lines.add(currentLine);
        }
    }

    /**
     * Get text at position without requiring chat to be focused
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
        return this.client.currentScreen instanceof AdvancedChatScreen;
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

        if (!HudConfigStorage.General.CHANGE_START_MESSAGE.config.getBooleanValue() || !(client.currentScreen instanceof AdvancedChatScreen screen)) {
            return;
        }
        if (window.getTab() instanceof MainChatTab) {
            for (ChatWindow w : windows) {
                if (w.getTab() instanceof CustomChatTab tab2) {
                    if (screen.getChatField().getText().startsWith(tab2.getStartingMessage()) && tab2.getStartingMessage().length() > 0) {
                        screen.getChatField().setText(screen.getChatField().getText().substring(tab2.getStartingMessage().length()));
                        break;
                    }
                }
            }
        } else if (window.getTab() instanceof CustomChatTab tab) {
            boolean replaced = false;

            for (ChatWindow w : windows) {
                if (w.getTab() instanceof CustomChatTab tab2) {
                    if (screen.getChatField().getText().startsWith(tab2.getStartingMessage()) && tab2.getStartingMessage().length() > 0) {
                        screen.getChatField().setText(tab.getStartingMessage() + screen.getChatField().getText().substring(tab2.getStartingMessage().length()));

                        replaced = true;

                        break;
                    }
                }
            }

            if (!replaced) {
                screen.getChatField().setText(tab.getStartingMessage() + screen.getChatField().getText());
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
            // Handle text click - open URLs, run commands, etc.
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
            x = Math.min(x, client.getWindow().getScaledWidth() - drag.getConvertedWidth());
            y = Math.min(y, client.getWindow().getScaledHeight());
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
        int windowHeight = client.getWindow().getScaledHeight();
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
     * Reimplements Screen.handleTextClick functionality for Minecraft 1.21.11
     */
    private boolean handleStyleClick(Style style, Screen screen) {
        if (style == null) {
            return false;
        }

        net.minecraft.text.ClickEvent clickEvent = style.getClickEvent();
        if (clickEvent == null) {
            return false;
        }

        // Use reflection to get the value since the API uses obfuscated method names in Minecraft 1.21.11
        try {
            net.minecraft.text.ClickEvent.Action action = clickEvent.getAction();

            // Try to get the value - iterate through String-returning methods
            String value = null;
            Object rawValue = null;



            // Try all methods with no parameters to find the value accessor
            // For different actions, the value might be String (RUN_COMMAND) or URI (OPEN_URL/OPEN_FILE)
            for (java.lang.reflect.Method method : clickEvent.getClass().getMethods()) {
                if (method.getParameterCount() == 0) {
                    String methodName = method.getName();
                    Class<?> returnType = method.getReturnType();

                    // Look for comp_XXXX methods or methods with "value" in the name
                    if (methodName.startsWith("comp_") || methodName.toLowerCase().contains("value")) {
                        try {
                            Object result = method.invoke(clickEvent);
                            if (result != null) {
                                rawValue = result;
                                // Convert to String based on the type
                                if (result instanceof String) {
                                    value = (String) result;
                                } else if (result instanceof java.net.URI) {
                                    // For URI, preserve the full URI string for later processing
                                    value = result.toString();
                                } else {
                                    value = result.toString();
                                }

                                if (!value.isEmpty()) {
                                    break;
                                }
                            }
                        } catch (Exception ex) {
                            // Continue trying other methods
                        }
                    }
                }
            }

            if (value == null || value.isEmpty()) {
                return false;
            }

            switch (action) {
                case OPEN_URL:
                    // Open URL in browser (http/https only)
                    try {
                        java.net.URI uri;

                        // If rawValue is already a URI object, use it directly
                        if (rawValue instanceof java.net.URI) {
                            uri = (java.net.URI) rawValue;
                        } else {
                            uri = new java.net.URI(value);
                        }

                        String scheme = uri.getScheme();
                        String uriString = uri.toString();

                        // WORKAROUND: Detect screenshot filenames that got corrupted into https:// URIs
                        // Pattern: https://YYYY-MM-DD_HH.MM.SS.png (where the filename became the hostname)
                        if (scheme != null && scheme.equalsIgnoreCase("https")) {
                            // The "hostname" in the corrupted URI is actually the filename
                            // Try to extract it from the URI string
                            String uriStr = uri.toString();

                            // Pattern: https://2026-04-02_14.40.44.png
                            // Extract just the filename part after https://
                            if (uriStr.startsWith("https://") && uriStr.endsWith(".png")) {
                                String filename = uriStr.substring(8); // Remove "https://"

                                // Check if it matches screenshot pattern
                                if (filename.matches("\\d{4}-\\d{2}-\\d{2}_\\d{2}\\.\\d{2}\\.\\d{2}\\.png")) {
                                    // This is a corrupted screenshot link - find the actual file in screenshots folder
                                    java.io.File screenshotsDir = new java.io.File(client.runDirectory, "screenshots");
                                    java.io.File screenshotFile = new java.io.File(screenshotsDir, filename);

                                    if (screenshotFile.exists()) {
                                        net.minecraft.util.Util.getOperatingSystem().open(screenshotFile);
                                        return true;
                                    }
                                }
                            }
                        }

                        // Check if this is actually a file:// URI (screenshot case)
                        if (scheme != null && scheme.equalsIgnoreCase("file")) {
                            // This should be OPEN_FILE action, but handle it gracefully
                            java.io.File file = new java.io.File(uri);
                            net.minecraft.util.Util.getOperatingSystem().open(file);
                            return true;
                        }

                        if (scheme == null) {
                            throw new java.net.URISyntaxException(value, "Missing protocol");
                        }

                        if (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) {
                            throw new java.net.URISyntaxException(value, "Unsupported protocol: " + scheme);
                        }

                        // Use Minecraft's Util class to open URL safely
                        net.minecraft.util.Util.getOperatingSystem().open(uri);
                        return true;
                    } catch (Exception e) {
                        AdvancedChatHud.LOGGER.error("Failed to open URL: " + value, e);
                    }
                    break;

                case OPEN_FILE:
                    // Open file (e.g., screenshots)
                    try {
                        java.io.File file;

                        // If rawValue is a URI object, use it
                        if (rawValue instanceof java.net.URI) {
                            java.net.URI uri = (java.net.URI) rawValue;
                            file = new java.io.File(uri);
                        } else if (value.startsWith("file://")) {
                            // It's a URI string - convert to File
                            file = new java.io.File(new java.net.URI(value));
                        } else {
                            // It's a direct file path
                            file = new java.io.File(value);
                        }

                        // Open the file with the system default application
                        net.minecraft.util.Util.getOperatingSystem().open(file);
                        return true;
                    } catch (Exception e) {
                        AdvancedChatHud.LOGGER.error("Failed to open file: " + value, e);
                    }
                    break;

                case RUN_COMMAND:
                    // Run command (starts with /)
                    if (client.player != null) {
                        String command = value.startsWith("/") ? value.substring(1) : value;
                        client.player.networkHandler.sendChatCommand(command);
                        // Close the chat screen if it's open
                        if (screen instanceof AdvancedChatScreen) {
                            client.setScreen(null);
                        }
                    }
                    return true;

                case SUGGEST_COMMAND:
                    // Suggest command in chat field - open chat if not already open
                    if (screen instanceof AdvancedChatScreen chatScreen) {
                        chatScreen.getChatField().setText(value);
                    } else {
                        // Open chat screen with the suggested command
                        client.setScreen(new AdvancedChatScreen(value));
                    }
                    return true;

                case CHANGE_PAGE:
                    // Page changes are handled by book screens, not chat
                    return false;

                case COPY_TO_CLIPBOARD:
                    // Copy text to clipboard
                    client.keyboard.setClipboard(value);
                    return true;
            }
        } catch (Exception e) {
            AdvancedChatHud.LOGGER.error("Failed to handle style click", e);
        }

        return false;
    }
}
