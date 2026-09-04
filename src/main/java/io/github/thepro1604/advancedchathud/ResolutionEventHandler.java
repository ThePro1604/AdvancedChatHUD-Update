/*
 * Copyright (C) 2022 thepro1604
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.thepro1604.advancedchathud;

import java.util.ArrayList;
import java.util.List;

public interface ResolutionEventHandler {

    List<ResolutionEventHandler> ON_RESOLUTION_CHANGE = new ArrayList<>();

    void onResolutionChange();

}
