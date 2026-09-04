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
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Match {

    private String pattern;
    private FindType findType;

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
