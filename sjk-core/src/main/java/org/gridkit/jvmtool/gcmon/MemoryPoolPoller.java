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

import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.Map;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

public class MemoryPoolPoller {

    private Map<String, MemPoolTracker> trackers = new HashMap<String, MemPoolTracker>();
    private MemoryPoolEventConsumer consumer;
    
    public MemoryPoolPoller(MBeanServerConnection mserver, MemoryPoolEventConsumer consumer) {
        this.consumer = consumer;
        try {
            ObjectName name = new ObjectName("java.lang:type=MemoryPool,name=*");
            for(ObjectName on: mserver.queryNames(name, null)) {
                
                MemoryPoolMXBean mpool = JMX.newMXBeanProxy(mserver, on, MemoryPoolMXBean.class);
                MemPoolTracker tracker = init(mpool);
                trackers.put(tracker.poolName, tracker);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public void poll() {
        for(MemPoolTracker t: trackers.values()) {
            poll(t);
        }
    }
    
    private MemPoolTracker init(MemoryPoolMXBean mpool) {
        MemPoolTracker tracker = new MemPoolTracker();
        tracker.poolName = mpool.getName();
        tracker.poolBean = mpool;
        tracker.nonHeap = mpool.getType() != MemoryType.HEAP;
        poll(tracker);
        return tracker;
    }

    private void poll(MemPoolTracker tracker) {
        MemoryUsage usage = tracker.poolBean.getUsage();
        MemoryUsage peakUsage = tracker.poolBean.getPeakUsage();
        MemoryUsage collectionUsage = tracker.poolBean.getCollectionUsage();
        long ts = System.currentTimeMillis();
        
        MemoryUsageBean busage = usage == null ? null : new MemoryUsageBean(usage);
        MemoryUsageBean bpeakUsage = peakUsage == null ? null : new MemoryUsageBean(peakUsage);
        MemoryUsageBean bcollectionUsage = collectionUsage == null ? null : new MemoryUsageBean(collectionUsage);
        
        if (busage != null && !busage.equals(tracker.lastMemUsage)) {
            tracker.lastMemUsage = busage;
            fireUsageEvent(ts, tracker);
        }
        if (bpeakUsage != null && !bpeakUsage.equals(tracker.lastMemPeak)) {
            tracker.lastMemPeak = bpeakUsage;
            firePeakUsageEvent(ts, tracker);
        }
        if (bcollectionUsage != null && !bcollectionUsage.equals(tracker.lastMemCollection)) {
            tracker.lastMemCollection = bcollectionUsage;
            fireCollectionUsageEvent(ts, tracker);
        }
    }

    private void fireUsageEvent(long timestamp, MemPoolTracker tracker) {
        MemoryInfoEventPojo pojo = new MemoryInfoEventPojo();
        pojo.timestamp(System.currentTimeMillis());
        pojo.name(tracker.poolName);
        pojo.nonHeap(tracker.nonHeap);
        pojo.currentUsage(tracker.lastMemUsage);
        
        consumer.consumeUsageEvent(pojo);
        
    }

    private void firePeakUsageEvent(long timestamp, MemPoolTracker tracker) {
        MemoryInfoEventPojo pojo = new MemoryInfoEventPojo();
        pojo.timestamp(System.currentTimeMillis());
        pojo.name(tracker.poolName);
        pojo.nonHeap(tracker.nonHeap);
        pojo.peakUsage(tracker.lastMemPeak);
        
        consumer.consumePeakEvent(pojo);
    }

    private void fireCollectionUsageEvent(long timestamp, MemPoolTracker tracker) {
        MemoryInfoEventPojo pojo = new MemoryInfoEventPojo();
        pojo.timestamp(System.currentTimeMillis());
        pojo.name(tracker.poolName);
        pojo.nonHeap(tracker.nonHeap);
        pojo.collectionUsage(tracker.lastMemCollection);
        
        consumer.consumeCollectionUsageEvent(pojo);
    }

    private class MemPoolTracker {
        
        MemoryPoolMXBean poolBean;
        String poolName;
        boolean nonHeap;
        
        MemoryUsageBean lastMemUsage;
        MemoryUsageBean lastMemPeak;
        MemoryUsageBean lastMemCollection;
        
    }
}
