/**
 * Copyright 2017 Alexey Ragozin
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
package org.gridkit.jvmtool.gcmon;

import java.lang.management.MemoryUsage;

public class MemoryUsageBean {

    private final long init;
    private final long used;
    private final long committed;
    private final long max;
    
    public MemoryUsageBean(long init, long used, long committed, long max) {
        this.init = init;
        this.used = used;
        this.committed = committed;
        this.max = max;
    }

    public MemoryUsageBean(MemoryUsage mu) {
        this.init = mu.getInit();
        this.used = mu.getUsed();
        this.committed = mu.getCommitted();
        this.max = mu.getMax();
    }

    public long init() {
        return init;
    }
    
    public long used() {
        return used;
    }
    
    public long committed() {
        return committed;
    }
    
    public long max() {
        return max;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (committed ^ (committed >>> 32));
        result = prime * result + (int) (init ^ (init >>> 32));
        result = prime * result + (int) (max ^ (max >>> 32));
        result = prime * result + (int) (used ^ (used >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MemoryUsageBean other = (MemoryUsageBean) obj;
        if (committed != other.committed)
            return false;
        if (init != other.init)
            return false;
        if (max != other.max)
            return false;
        if (used != other.used)
            return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[init=");
        builder.append(init);
        builder.append(", used=");
        builder.append(used);
        builder.append(", committed=");
        builder.append(committed);
        builder.append(", max=");
        builder.append(max);
        builder.append("]");
        return builder.toString();
    }
}
