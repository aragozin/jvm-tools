/**
 * Copyright 2014 Alexey Ragozin
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
package org.gridkit.jvmtool;

import org.gridkit.lab.jvm.perfdata.JStatData;
import org.gridkit.lab.jvm.perfdata.JStatData.LongCounter;
import org.gridkit.lab.jvm.perfdata.JStatData.TickCounter;

public class PerfCounterSafePointMonitor implements SafePointMonitor {

    LongCounter safePointCount;
    TickCounter safePointTime;
    TickCounter safePointSyncTime;

    public PerfCounterSafePointMonitor(long pid) {
        JStatData jd = JStatData.connect(pid);
        safePointCount = (LongCounter) jd.getAllCounters().get("sun.rt.safepoints");
        safePointTime = (TickCounter) jd.getAllCounters().get("sun.rt.safepointTime");
        safePointSyncTime = (TickCounter) jd.getAllCounters().get("sun.rt.safepointSyncTime");
    }

    public boolean isAvailable() {
        return safePointCount != null && safePointTime != null && safePointSyncTime != null;
    }

    @Override
    public long getSafePointCount() {
        return safePointCount == null ? 0 : safePointCount.getLong();
    }

    @Override
    public long getSafePointTime() {
        return safePointTime == null ? 0 : safePointTime.getNanos();
    }

    @Override
    public long getSafePointSyncTime() {
        return safePointSyncTime == null ? 0 : safePointSyncTime.getNanos();
    }
}
