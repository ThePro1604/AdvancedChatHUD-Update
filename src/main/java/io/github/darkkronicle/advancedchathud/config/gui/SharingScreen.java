/*
 * Copyright (C) 2021 DarkKronicle
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.darkkronicle.advancedchathud.config.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.Message;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.wrappers.TextFieldWrapper;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.darkkronicle.advancedchatcore.gui.buttons.NamedSimpleButton;
import io.github.darkkronicle.advancedchathud.AdvancedChatHud;
import io.github.darkkronicle.advancedchathud.config.ChatTab;
import io.github.darkkronicle.advancedchathud.config.HudConfigStorage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

/** Screen for importing and exporting {@link ChatTab}. */
public class SharingScreen extends GuiBase {

    private final String starting;
    private static final Gson GSON = new GsonBuilder().create();
    private GuiTextFieldGeneric text;

    public SharingScreen(String starting, Screen parent) {
        this.setParent(parent);
        this.setTitle(StringUtils.translate("advancedchat.gui.menu.import"));
        this.starting = starting;
    }

    /** Creates a SharingScreen from a tab */
    public static SharingScreen fromTab(ChatTab tab, Screen parent) {
        ChatTab.ChatTabJsonSave tabJsonSave = new ChatTab.ChatTabJsonSave();
        return new SharingScreen(GSON.toJson(tabJsonSave.save(tab)), parent);
    }

    @Override
    public void init() {
        int x = this.width / 2 - 150;
        int y = 50;
        text = new GuiTextFieldGeneric(x, y, 300, 20, client.textRenderer);
        y -= 24;

        // IMPORTANT: Set maxLength and validator BEFORE creating wrapper
        // Do NOT set text yet - TextFieldWrapper will crash if text is already set
        text.setMaxLength(64000);
        setPermissiveValidator(text);

        text.setFocused(true);
        text.setDrawsBackground(true);
        text.setEditable(true);

        // Create wrapper and add the text field (BEFORE setting text!)
        TextFieldWrapper<GuiTextFieldGeneric> wrapper = new TextFieldWrapper<>(text, null);
        this.addTextField(text, null);

        // Fix character limit again - TextFieldWrapper resets maxLength to 12
        text.setMaxLength(64000);
        setPermissiveValidator(text);
        setWrapperMaxLength(wrapper, 64000);

        // Now it's safe to set the text after wrapper is configured
        if (starting != null) {
            text.setText(starting);
            text.setFocused(true);
        }

        this.addButton(
                x, y, "advancedchathud.gui.button.import", (button, mouseButton) -> importTab());
    }

    private void addButton(int x, int y, String translation, IButtonActionListener listener) {
        this.addButton(new NamedSimpleButton(x, y, StringUtils.translate(translation)), listener);
    }

    public void importTab() {
        ChatTab.ChatTabJsonSave tabSave = new ChatTab.ChatTabJsonSave();
        ChatTab tab = tabSave.load(new JsonParser().parse(text.getText()).getAsJsonObject());
        if (tab == null) {
            throw new NullPointerException("Filter is null!");
        }
        HudConfigStorage.TABS.add(tab);
        AdvancedChatHud.MAIN_CHAT_TAB.setUpTabs();
        addGuiMessage(
                Message.MessageType.SUCCESS,
                5000,
                StringUtils.translate("advancedchat.gui.message.successful"));
    }

    public void resize(MinecraftClient mc, int width, int height) {
        this.width = width;
        this.height = height;

        clearElements();
        init();
    }

    private static void setPermissiveValidator(GuiTextFieldGeneric field) {
        try {
            Class<?> superClazz = field.getClass().getSuperclass();
            java.lang.reflect.Field validatorField = superClazz.getDeclaredField("field_2104");
            validatorField.setAccessible(true);

            // Create a permissive validator that accepts any string
            java.util.function.Predicate<String> permissiveValidator = s -> true;  // Always accept
            validatorField.set(field, permissiveValidator);
        } catch (Exception e) {
            // Silently fail - the field might not exist in all versions
        }
    }

    private static void setWrapperMaxLength(TextFieldWrapper<GuiTextFieldGeneric> wrapper, int maxLength) {
        try {
            // The TextFieldWrapper has a 'type' field (TextFieldType enum) that controls validation
            Class<?> wrapperClass = wrapper.getClass();
            java.lang.reflect.Field typeField = wrapperClass.getDeclaredField("type");
            typeField.setAccessible(true);
            Object type = typeField.get(wrapper);

            if (type != null) {
                // Get the TextFieldType class and update its maxLength field
                Class<?> typeClass = type.getClass();

                for (java.lang.reflect.Field field : typeClass.getDeclaredFields()) {
                    field.setAccessible(true);

                    // Set maxLength to a high value to allow long patterns
                    if ((field.getName().toLowerCase().contains("max") && field.getName().toLowerCase().contains("length"))
                        || field.getName().equals("maxLength")) {
                        if (field.getType() == int.class || field.getType() == Integer.class) {
                            field.set(type, maxLength);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Silently fail - the field might not exist in all versions
        }
    }
}
