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

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

public class GcEventPoller implements Runnable {

    private static final ObjectName RUNTIME_MXBEAN = mname(ManagementFactory.RUNTIME_MXBEAN_NAME);

    private static ObjectName mname(String name) {
        try {
            return new ObjectName(name);
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }
    }

    protected final MBeanServerConnection mserver;
    protected final GarbageCollectionEventConsumer eventSink;
    protected long jvmStartTime;
    protected List<GcTracker> trackers = new ArrayList<GcTracker>();

    public GcEventPoller(MBeanServerConnection mserver, GarbageCollectionEventConsumer eventSink) {
        this.mserver = mserver;
        this.eventSink = eventSink;

        RuntimeMXBean runtime = JMX.newMXBeanProxy(mserver, RUNTIME_MXBEAN, RuntimeMXBean.class);
        jvmStartTime = runtime.getStartTime();

        initTrackers();


    }

    private void initTrackers() {
        try {
            for(ObjectName name: mserver.queryNames(null, null)) {
                if (name.getDomain().equals("java.lang") && "GarbageCollector".equals(name.getKeyProperty("type"))) {
                    GcTracker tracker = new GcTracker(name, name.getKeyProperty("name"));
                    initTracker(tracker);
                    trackers.add(tracker);
                    tracker.capture();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void initTracker(GcTracker tracker) throws InstanceNotFoundException, IOException {

    }

    @Override
    public void run() {
        for(GcTracker tracker: trackers) {
            try {
                tracker.capture();
            } catch (JMException e) {
                // ignore
            } catch (IOException e) {
                // ignore
            }
        }
    }

    public ExecutorService schedule(long period) {
        ScheduledThreadPoolExecutor sp = new ScheduledThreadPoolExecutor(1);
        sp.scheduleAtFixedRate(this, period, period, TimeUnit.MILLISECONDS);
        return sp;
    }

    class GcTracker {

        ObjectName name;
        String collectorName;

        long lastReportedEvent = -1;

        public GcTracker(ObjectName name, String collectorName) {
            this.name = name;
            this.collectorName = collectorName;
        }

        public synchronized void capture() throws JMException, IOException {
            CompositeData lastGc = getLastGcInfo();

            if (lastGc != null) {

                long id = along(lastGc.get("id"));
                long ts = along(lastGc.get("startTime"));
                // ignoring initial (t==0) pseudo event
                if (ts > 0 && id != lastReportedEvent) {
                    processGcEvent(lastGc);
                }
            }
        }

        protected synchronized void processGcEvent(CompositeData lastGc) throws JMException, IOException {

            long id = along(lastGc.get("id"));

            if (lastReportedEvent == id) {
                return;
            }

            lastReportedEvent = id;

            long dur = along(lastGc.get("duration"));
            long startTs = along(lastGc.get("startTime")) + jvmStartTime;

            @SuppressWarnings("unchecked")
            Map<List<?>, CompositeData> beforeGC = (Map<List<?>, CompositeData>) lastGc.get("memoryUsageBeforeGc");
            @SuppressWarnings("unchecked")
            Map<List<?>, CompositeData> afterGC = (Map<List<?>, CompositeData>) lastGc.get("memoryUsageAfterGc");

            GcSummary summary = new GcSummary();
            summary.name = collectorName;
            summary.timestamp = startTs;
            summary.durationMs = dur;
            summary.collectionCount = id;
            summary.collectionTotalTime = totalTime(id);

            for(Entry<List<?>, CompositeData> e: beforeGC.entrySet()) {
                String pool = (String) e.getKey().get(0);
                String spaceId = toSpaceId(pool);
                MemoryUsage membefore = MemoryUsage.from((CompositeData) e.getValue().get("value"));
                MemUsage mu = new MemUsage(pool, membefore);
                summary.before.put(spaceId, mu);
            }

            for(Entry<List<?>, CompositeData> e: afterGC.entrySet()) {
                String pool = (String) e.getKey().get(0);
                String spaceId = toSpaceId(pool);
                MemUsage memafter = new MemUsage(pool, MemoryUsage.from((CompositeData) e.getValue().get("value")));
                MemUsage mu = memafter;
                summary.after.put(spaceId, mu);
            }

            eventSink.consume(summary);
        }

        private String toSpaceId(String key) {
            return key.toLowerCase().replace(' ', '-');
        }

        private long along(Object v) {
            return ((Number)v).longValue();
        }

        private long totalTime(long id) throws JMException, IOException {
            AttributeList list = mserver.getAttributes(name, new String[]{"CollectionCount", "CollectionTime"});
            long rid = ((Number)((Attribute)list.get(0)).getValue()).longValue();
            if (id == rid) {
                return ((Number)((Attribute)list.get(1)).getValue()).longValue();
            }
            else {
                return -1;
            }
        }

        private CompositeData getLastGcInfo() throws JMException, IOException {
            return (CompositeData)mserver.getAttribute(name, "LastGcInfo");
        }
    }

    private static class GcSummary implements GarbageCollectionSummary {

        private String name;
        private long timestamp;
        private long durationMs;

        private long collectionCount;
        private long collectionTotalTime;

        private Map<String, MemUsage> before = new HashMap<String, MemUsage>();
        private Map<String, MemUsage> after = new HashMap<String, MemUsage>();

        @Override
        public long timestamp() {
            return timestamp;
        }

        @Override
        public long duration() {
            return TimeUnit.MILLISECONDS.toMicros(durationMs);
        }

        @Override
        public String collectorName() {
            return name;
        }

        @Override
        public long collectionCount() {
            return collectionCount;
        }

        @Override
        public long collectionTotalTime() {
            return collectionTotalTime;
        }

        @Override
        public Iterable<String> memorySpaces() {
            return before.keySet();
        }

        @Override
        public long memoryBefore(String spaceId) {
            MemUsage mu = before.get(spaceId);
            if (mu == null) {
                return Long.MIN_VALUE;
            }
            else {
                return mu.used;
            }
        }

        @Override
        public long memoryAfter(String spaceId) {
            MemUsage mu = after.get(spaceId);
            if (mu == null) {
                return Long.MIN_VALUE;
            }
            else {
                return mu.used;
            }
        }

        @Override
        public long memoryMax(String spaceId) {
            MemUsage mu = after.get(spaceId);
            if (mu == null) {
                return Long.MIN_VALUE;
            }
            else {
                return mu.max;
            }
        }

        @Override
        public String memorySpaceName(String spaceId) {
            MemUsage mu = after.get(spaceId);
            if (mu == null) {
                return null;
            }
            else {
                return mu.name;
            }
        }
    }

    private static class MemUsage {

        String name;
        @SuppressWarnings("unused")
        long init;
        long used;
        @SuppressWarnings("unused")
        long commited;
        long max;

        public MemUsage(String name, MemoryUsage mu) {
            this(name, mu.getInit(), mu.getUsed(), mu.getCommitted(), mu.getMax());
        }

        public MemUsage(String name, long init, long used, long commited, long max) {
            this.name = name;
            this.init = init;
            this.used = used;
            this.commited = commited;
            this.max = max;
        }
    }
}
