/*
 * Copyright (C) 2021 thepro1604
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.thepro1604.advancedchathud.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.thepro1604.advancedchatcore.interfaces.IJsonSave;
import io.github.thepro1604.advancedchatcore.util.FindType;
import io.github.thepro1604.advancedchatcore.util.SearchUtils;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Match {

    private String pattern;
    private FindType findType;

    /** Placeholder that matches any text in {@link FindType#LITERAL} and {@link FindType#UPPERLOWER} patterns */
    public static final String IGNORE_PLACEHOLDER = "{ignore}";

    private static final Pattern IGNORE_SPLIT =
            Pattern.compile(Pattern.quote(IGNORE_PLACEHOLDER), Pattern.CASE_INSENSITIVE);

    public boolean isMatch(String input) {
        if ((findType == FindType.LITERAL || findType == FindType.UPPERLOWER)
                && IGNORE_SPLIT.matcher(pattern).find()) {
            return SearchUtils.isMatch(input, toIgnoreRegex(), FindType.REGEX);
        }
        return SearchUtils.isMatch(input, pattern, findType);
    }

    /** Converts the pattern into a regex where each placeholder matches any text and everything else is literal */
    private String toIgnoreRegex() {
        StringBuilder regex = new StringBuilder();
        if (findType == FindType.UPPERLOWER) {
            regex.append("(?i)");
        }
        String[] parts = IGNORE_SPLIT.split(pattern, -1);
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                regex.append(".+?");
            }
            if (!parts[i].isEmpty()) {
                regex.append(Pattern.quote(parts[i]));
            }
        }
        return regex.toString();
    }

    public static class MatchSerializer implements IJsonSave<Match> {

        @Override
        public Match load(JsonObject obj) {
            JsonElement pattern = obj.get("pattern");
            JsonElement findType = obj.get("findtype");
            if (pattern == null || findType == null) {
                return null;
            }
            FindType type = FindType.fromFindType(findType.getAsString());
            return new Match(pattern.getAsString(), type);
        }

        @Override
        public JsonObject save(Match match) {
            JsonObject obj = new JsonObject();
            obj.addProperty("pattern", match.getPattern());
            obj.addProperty("findtype", match.getFindType().configString);
            return obj;
        }
    }
}
