/**
 * Copyright 2019 Alexey Ragozin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gridkit.jvmtool.stacktrace.analytics;

import org.gridkit.jvmtool.event.CommonEvent;

public interface WeigthCalculator {

    public static enum CaptionFlavour {
        COMMON
    }

    public long getWeigth(CommonEvent event);

    public long getDenominator();

    public String formatWeight(CaptionFlavour flavour, long weight);

}
