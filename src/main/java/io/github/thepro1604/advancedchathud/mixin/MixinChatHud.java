/*
 * Copyright (C) 2021 thepro1604
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.thepro1604.advancedchathud.mixin;

import io.github.thepro1604.advancedchatcore.chat.ChatMessage;
import io.github.thepro1604.advancedchathud.HudChatMessage;
import io.github.thepro1604.advancedchathud.HudChatMessageHolder;
import io.github.thepro1604.advancedchathud.config.HudConfigStorage;
import io.github.thepro1604.advancedchathud.gui.WindowManager;
import io.github.thepro1604.advancedchathud.itf.IChatHud;
import io.github.thepro1604.advancedchathud.tabs.AbstractChatTab;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.ComponentRenderUtils;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.ComponentCollector;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Iterator;
import java.util.List;

@Mixin(value = ChatComponent.class, priority = 1050)
@Environment(EnvType.CLIENT)
public abstract class MixinChatHud implements IChatHud {

    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private List<GuiMessage> allMessages;
    @Shadow @Final private List<GuiMessage.Line> trimmedMessages;

    @Shadow private int chatScrollbarPos;
    @Shadow private boolean newMessageSinceScroll;

    @Unique
    private AbstractChatTab tab;

    @Shadow
    public abstract int getWidth();

    @Shadow
    public abstract double getScale();

    @Shadow
    public abstract boolean isChatFocused();

    @Shadow
    public abstract void scrollChat(int amount);

    @Shadow
    public abstract int getHeight();

    @Inject(at = @At("HEAD"), method = "scrollChat", cancellable = true)
    private void onScrollChat(int amount, CallbackInfo ci) {
        // Only scroll if nothing is focused
        if (WindowManager.getInstance().getSelected() != null) {
            ci.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "extractRenderState", cancellable = true)
    private void onExtractRenderState(GuiGraphicsExtractor graphics, net.minecraft.client.gui.Font font, int ticks, int mouseX, int mouseY, ChatComponent.DisplayMode displayMode, boolean changeCursorOnInsertions, CallbackInfo ci) {
        // Ignore rendering vanilla chat if disabled
        if (!HudConfigStorage.General.VANILLA_HUD.config.getBooleanValue()) {
            ci.cancel();
        }
    }

    // Note: getTextStyleAt method was removed/renamed in Minecraft 1.21.11
    // Hover detection for custom chat windows currently relies on WindowManager.getValue()
    // being called from click handlers. Tooltips work during chat screen open state.
    // TODO: Find the new method name for hover Component detection in 1.21.11
    // @Inject(at = @At("HEAD"), method = "getTextStyleAt", cancellable = true)
    // public void getTextHead(double x, double y, CallbackInfoReturnable<Style> cir) {
    //     // Ignore checking vanilla chat for hovered Component if disabled
    //     if (!HudConfigStorage.General.VANILLA_HUD.config.getBooleanValue()) {
    //         cir.setReturnValue(WindowManager.getInstance().getText(x, y));
    //     }
    // }
    //
    // @Inject(at = @At("RETURN"), method = "getTextStyleAt", cancellable = true)
    // public void getTextReturn(double x, double y, CallbackInfoReturnable<Style> cir) {
    //     // If vanilla chat didn't find any text, search on our own windows
    //     if (cir.getReturnValue() == null) {
    //         cir.setReturnValue(WindowManager.getInstance().getText(x, y));
    //     }
    // }

    @Override
    public AbstractChatTab getTab() {
        return tab;
    }

    @Override
    public void setTab(AbstractChatTab tab) {
        this.tab = tab;
        this.allMessages.clear();
        this.trimmedMessages.clear();

        List<HudChatMessage> messages = HudChatMessageHolder.getInstance().getMessages();
        for (int i = messages.size() - 1; i >= 0; i--) {
            addMessage(messages.get(i));
        }
    }

    @Override
    public void removeMessage(ChatMessage remove) {
        // Reset messages that exist
        setTab(this.tab);
    }

    @Override
    public void addMessage(HudChatMessage hudMsg) {
        if (tab == null || !hudMsg.getTabs().contains(tab)) {
            return;
        }
        if (HudConfigStorage.General.VANILLA_HUD.config.getBooleanValue()) {
            tab.resetUnread();
        }

        int width = Mth.floor((double) this.getWidth() / this.getScale());

        ChatMessage msg = hudMsg.getMessage();

        // Create the GuiMessage first - source is null since we don't track the message source
        GuiMessage guiMessage = new GuiMessage(msg.getCreationTick(), msg.getDisplayText(), msg.getSignature(), null, msg.getIndicator());

        List<FormattedCharSequence> list =
                ComponentRenderUtils.wrapComponents(
                        msg.getDisplayText(), width, this.minecraft.font);

        // Now create GuiMessage.Line instances that reference the GuiMessage
        FormattedCharSequence orderedText;
        for (Iterator<FormattedCharSequence> it = list.iterator();
                it.hasNext();
                this.trimmedMessages.addFirst(new GuiMessage.Line(guiMessage, orderedText, !it.hasNext()))) {
            orderedText = it.next();
            if (this.isChatFocused() && this.chatScrollbarPos > 0) {
                this.newMessageSinceScroll = true;
                this.scrollChat(1);
            }
        }

        while (this.trimmedMessages.size()
                > HudConfigStorage.General.STORED_LINES.config.getIntegerValue()) {
            this.trimmedMessages.removeLast();
        }

        this.allMessages.addFirst(guiMessage);
        while (this.allMessages.size()
                > HudConfigStorage.General.STORED_LINES.config.getIntegerValue()) {
            this.allMessages.removeLast();
        }
    }

    @Shadow
    public abstract void clearMessages(boolean clearHistory);

    // Implement the IChatHud.clear() method by delegating to the Mojang clearMessages() method
    @Override
    public void clear(boolean clearHistory) {
        clearMessages(clearHistory);
    }

    @Override
    public boolean isOver(double mouseX, double mouseY) {
        double minX = 4 - (4 * getScale());
        double maxX = 4 + (getWidth() + 4 * getScale());

        mouseY = (minecraft.getWindow().getGuiScaledHeight() - mouseY - 40) / getScale();
        return mouseX >= minX && mouseX < maxX && mouseY >= 0 && mouseY < getHeight();
    }
}
