/*
 * Copyright (C) 2021 DarkKronicle
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.darkkronicle.advancedchathud.tabs;

import fi.dy.masa.malilib.config.options.ConfigColor;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.darkkronicle.Konstruct.functions.Function;
import io.github.darkkronicle.Konstruct.nodes.LiteralNode;
import io.github.darkkronicle.Konstruct.nodes.Node;
import io.github.darkkronicle.Konstruct.parser.IntRange;
import io.github.darkkronicle.Konstruct.parser.ParseContext;
import io.github.darkkronicle.Konstruct.parser.Result;
import io.github.darkkronicle.Konstruct.type.BooleanObject;
import io.github.darkkronicle.advancedchatcore.util.Color;
import io.github.darkkronicle.advancedchatcore.util.SearchUtils;
import io.github.darkkronicle.advancedchathud.AdvancedChatHud;
import io.github.darkkronicle.advancedchathud.config.ChatTab;
import io.github.darkkronicle.advancedchathud.config.Match;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** ChatTab that loads from {@link ChatTab}. Easy to customize. */
public class CustomChatTab extends AbstractChatTab {

    @Getter private List<Match> matches;

    @Setter
    @Getter
    private boolean forward;

    @Getter
    @Setter
    private String startingMessage;

    @Getter
    @Setter
    private ChatTab tab;

    @Setter
    private Function function;

    /** Helper method to convert ConfigColor to Color */
    private static Color getColor(ConfigColor configColor) {
        return new Color(StringUtils.getColor(configColor.getStringValue(), 0xFFFFFFFF));
    }

    public CustomChatTab(ChatTab tab) {
        super(
            tab.getName().config.getStringValue(),
            tab.getAbbreviation().config.getStringValue(),
            getColor(tab.getMainColor().config),
            getColor(tab.getBorderColor().config),
            getColor(tab.getInnerColor().config),
            tab.getShowUnread().config.getBooleanValue(),
            tab.getUuid()
        );
        this.tab = tab;
        this.matches = new ArrayList<>(tab.getMatches());
        this.forward = tab.getForward().config.getBooleanValue();
        this.startingMessage = tab.getStartingMessage().config.getStringValue();
        setDefaultFunction();
    }

    public void setDefaultFunction() {
        function = new Function() {
            @Override
            public Result parse(ParseContext context, List<Node> input) {
                CustomChatTab self = ((ChatTabObject) Function.parseArgument(context, input, 0).getContent()).getTab();
                String search = Function.parseArgument(context, input, 1).getContent().getString();
                for (Match m : self.matches) {
                    if (SearchUtils.isMatch(search, m.getPattern(), m.getFindType())) {
                        return Result.success(new BooleanObject(true));
                    }
                }
                return Result.success(new BooleanObject(false));
            }

            @Override
            public IntRange getArgumentCount() {
                return IntRange.of(2);
            }
        };
    }

    @Override
    public boolean shouldAdd(Text text) {
        String search = text.getString();
        ParseContext context = AdvancedChatHud.MAIN_CHAT_TAB.getProcessor().createContext();
        Result result = function.parse(context, List.of(new Node() {
            @Override
            public Result parse(ParseContext context) {
                return Result.success(new ChatTabObject(CustomChatTab.this));
            }

            @Override
            public List<Node> getChildren() {
                return new ArrayList<>(0);
            }

            @Override
            public void addChild(Node node) {

            }
        }, new LiteralNode(search)));
        return result.getContent().getBoolean();
    }
}
