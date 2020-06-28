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
import org.gridkit.lab.jvm.perfdata.JStatData.TickCounter;

public class PerfCounterGcCpuUsageMonitor implements GcCpuUsageMonitor {

    TickCounter gc0;
    TickCounter gc1;

    public PerfCounterGcCpuUsageMonitor(long pid) {
        JStatData jd = JStatData.connect(pid);
        gc0 = (TickCounter) jd.getAllCounters().get("sun.gc.collector.0.time");
        gc1 = (TickCounter) jd.getAllCounters().get("sun.gc.collector.1.time");
    }

    public boolean isAvailable() {
        return gc0 != null && gc1 != null;
    }

    @Override
    public long getYoungGcCpu() {
        return gc0 == null ? 0 : gc0.getNanos();
    }

    @Override
    public long getOldGcCpu() {
        return gc1 == null ? 0 : gc1.getNanos();
    }
}
