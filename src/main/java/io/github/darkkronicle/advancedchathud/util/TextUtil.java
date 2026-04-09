/*
 * Copyright (C) 2021 DarkKronicle
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.darkkronicle.advancedchathud.util;

import lombok.experimental.UtilityClass;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.util.Optional;

/**
 * Utility class for text manipulation and conversion.
 */
@UtilityClass
@Environment(EnvType.CLIENT)
public class TextUtil {

    /**
     * Converts a Minecraft Text object to a string with HEX color codes.
     * This preserves all color information including custom colors, ranks, nicknames, etc.
     *
     * @param text The Text object to convert
     * @return A string with HEX color codes in the format &#RRGGBB and formatting codes like &l, &o, etc.
     */
    public static String toStringWithHexColors(Text text) {
        if (text == null) {
            return "";
        }
        
        StringBuilder result = new StringBuilder();
        
        // Visit all text components and build the formatted string
        text.visit((style, content) -> {
            // Append color code if present
            appendStyleCodes(result, style);
            
            // Append the actual text content
            result.append(content);
            
            return Optional.empty();
        }, text.getStyle());
        
        return result.toString();
    }
    
    /**
     * Appends style codes (color and formatting) to the StringBuilder.
     * 
     * @param builder The StringBuilder to append to
     * @param style The style containing color and formatting information
     */
    private static void appendStyleCodes(StringBuilder builder, Style style) {
        if (style == null) {
            return;
        }
        
        // Handle color - prefix with &
        TextColor color = style.getColor();
        if (color != null) {
            // Convert color to HEX format with & prefix (e.g., &#FF5555)
            int rgb = color.getRgb();
            builder.append(String.format("&#%06X", rgb & 0xFFFFFF));
        }

        // Handle formatting codes (bold, italic, underline, etc.) - prefix with &
        if (style.isBold()) {
            builder.append("&l");
        }
        if (style.isItalic()) {
            builder.append("&o");
        }
        if (style.isUnderlined()) {
            builder.append("&n");
        }
        if (style.isStrikethrough()) {
            builder.append("&m");
        }
        if (style.isObfuscated()) {
            builder.append("&k");
        }
    }
    
    /**
     * Converts a Minecraft Text object to a string with both HEX color codes and legacy formatting codes.
     * This is a more compatible version that includes both modern hex colors and legacy formatting.
     * 
     * @param text The Text object to convert
     * @return A string with HEX color codes and formatting codes
     */
    public static String toStringWithHexColorsAndFormatting(Text text) {
        if (text == null) {
            return "";
        }
        
        StringBuilder result = new StringBuilder();
        Style previousStyle = Style.EMPTY;
        
        // Visit all text components and build the formatted string
        text.visit((style, content) -> {
            // Only add formatting if style changed
            if (!style.equals(previousStyle)) {
                appendFormattingCodesIfNeeded(result, style);
            }
            
            // Append the actual text content
            result.append(content);
            
            return Optional.empty();
        }, text.getStyle());
        
        return result.toString();
    }
    
    /**
     * Appends formatting codes only if they differ from the default.
     *
     * @param builder The StringBuilder to append to
     * @param style The style to extract formatting from
     */
    private static void appendFormattingCodesIfNeeded(StringBuilder builder, Style style) {
        if (style == null) {
            return;
        }

        // Add color first - prefix with &
        TextColor color = style.getColor();
        if (color != null) {
            int rgb = color.getRgb();
            builder.append(String.format("&#%06X", rgb & 0xFFFFFF));
        }

        // Then add formatting modifiers - prefix with &
        if (style.isBold()) {
            builder.append("&l");
        }
        if (style.isItalic()) {
            builder.append("&o");
        }
        if (style.isUnderlined()) {
            builder.append("&n");
        }
        if (style.isStrikethrough()) {
            builder.append("&m");
        }
        if (style.isObfuscated()) {
            builder.append("&k");
        }
    }
}

