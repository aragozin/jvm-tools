package org.gridkit.jvmtool.gcflow;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.gridkit.jvmtool.gcflow.GarbageCollectionSampler.GcReport;
import org.gridkit.jvmtool.gcflow.GcKnowledgeBase.PoolType;

import com.sun.management.GarbageCollectorMXBean;
import com.sun.management.GcInfo;

@SuppressWarnings("restriction")
class GcAdapter {

    private final GarbageCollectionSampler sampler;
    private final GarbageCollectorMXBean gc;

    private final String name;
    private final long processStartMs;
    private final List<String> collectedPools;
    private final List<String> allCollectedPools;

    private final List<String> edenPools;
    private final List<String> survivourPools;
    private final List<String> youngPools;
    private final List<String> oldPools;
    private final List<String> permPools;



    private final boolean isYoung;
    private final boolean isConcurent;

    private long gcCount = -1;
    private long prevCollectionEndTime = -1;


    public GcAdapter(MBeanServerConnection mserver, ObjectName gcname, GarbageCollectionSampler sampler) throws IOException, MalformedObjectNameException {
        this.sampler = sampler;
        gc = JMX.newMXBeanProxy(mserver, gcname, GarbageCollectorMXBean.class);
        name = gc.getName();
        RuntimeMXBean runtime = JMX.newMXBeanProxy(mserver, new ObjectName(ManagementFactory.RUNTIME_MXBEAN_NAME), RuntimeMXBean.class);

        processStartMs = runtime.getStartTime();
        collectedPools = Arrays.asList(gc.getMemoryPoolNames());

        allCollectedPools = new ArrayList<String>(GcKnowledgeBase.allCollectedPools(mserver));
        Map<GcKnowledgeBase.PoolType, Collection<String>> types = GcKnowledgeBase.classifyMemoryPools(mserver);

        edenPools = getMemPools(types, PoolType.EDEN);
        survivourPools = getMemPools(types, PoolType.SURVIVOR);
        oldPools = getMemPools(types, PoolType.TENURED);
        permPools = getMemPools(types, PoolType.PERMANENT);
        youngPools = new ArrayList<String>();
        youngPools.addAll(edenPools);
        youngPools.addAll(survivourPools);

        isYoung = collectedPools.containsAll(oldPools);
        isConcurent = "ConcurrentMarkSweep".equals(name);
    }

    private List<String> getMemPools(Map<PoolType, Collection<String>> types, PoolType type) {
        List<String> pools;
        if (types.containsKey(type)) {
            pools = new ArrayList<String>(types.get(type));
        }
        else {
            pools = Collections.emptyList();
        }
        return pools;
    }

    public void report() {
        try {
            GcInfo lastGc = gc.getLastGcInfo();
            if (lastGc == null || lastGc.getId() == gcCount) {
                return;
            }
            else {
                int missed = (int)(lastGc.getId() - 1 - gcCount);
                if (gcCount < 0) {
                    missed = 0;
                }
                long gcInterval = lastGc.getStartTime() - prevCollectionEndTime;
                prevCollectionEndTime = lastGc.getEndTime();
                if (gcCount < 0) {
                    gcInterval = -1;
                }
                if (lastGc.getEndTime() == 0) {
                    // no GC so far
                    prevCollectionEndTime = 0;
                    gcCount = lastGc.getId();
                }
                else {
                    gcCount = lastGc.getId();
                    sampler.report(name, missed, new Report(lastGc, gcInterval));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private class Report implements GcReport {

        private GcInfo gcInfo;
        private long gcInterval;

        public Report(GcInfo gcInfo, long gcInterval) {
            this.gcInfo = gcInfo;
            this.gcInterval = gcInterval;
        }

        @Override
        public long getId() {
            return gcInfo.getId();
        }

        @Override
        public long getWallClockStartTime() {
            return processStartMs + gcInfo.getStartTime();
        }

        @Override
        public long getWallClockEndTime() {
            return processStartMs + gcInfo.getEndTime();
        }

        @Override
        public long getJvmClockStartTime() {
            return gcInfo.getStartTime();
        }

        @Override
        public long getJvmClockEndTime() {
            return gcInfo.getEndTime();
        }

        @Override
        public long getDuration() {
            return gcInfo.getDuration();
        }

        @Override
        public long getTimeSincePreviousGC() {
            return gcInterval;
        }

        @Override
        public boolean isYoungGC() {
            return isYoung;
        }

        @Override
        public boolean isConcurrentGC() {
            return isConcurent;
        }

        @Override
        public long getCollectedSize() {
            return getTotalSizeBefore() - getTotalSizeAfter();
        }

        @Override
        public long getPromotedSize() {
            return getSizeAfter(oldPools) - getSizeBefore(oldPools);
        }

        @Override
        public long getTotalSizeBefore() {
            return getSizeBefore(allCollectedPools);
        }

        @Override
        public long getTotalSizeAfter() {
            return getSizeAfter(allCollectedPools);
        }

        @Override
        public Collection<String> getColletedPools() {
            return Collections.unmodifiableCollection(collectedPools);
        }

        @Override
        public Collection<String> getAllCollectedPools() {
            return Collections.unmodifiableCollection(allCollectedPools);
        }

        @Override
        public Collection<String> getAllMemoryPools() {
            return Collections.unmodifiableCollection(gcInfo.getMemoryUsageAfterGc().keySet());
        }

        @Override
        public long getSizeBefore(String pool) {
            return gcInfo.getMemoryUsageBeforeGc().get(pool).getUsed();
        }

        @Override
        public long getSizeAfter(String pool) {
            return gcInfo.getMemoryUsageAfterGc().get(pool).getUsed();
        }

        @Override
        public long getSizeBefore(Collection<String> pools) {
            long total = 0;
            for(String pool: pools) {
                total += getSizeBefore(pool);
            }
            return total;
        }

        @Override
        public long getSizeAfter(Collection<String> pools) {
            long total = 0;
            for(String pool: pools) {
                total += getSizeAfter(pool);
            }
            return total;
        }

        @Override
        public Collection<String> getEdenPools() {
            List<String> list = new ArrayList<String>(edenPools);
            list.retainAll(gcInfo.getMemoryUsageAfterGc().keySet());
            return list;
        }

        @Override
        public Collection<String> getSurvivourPools() {
            List<String> list = new ArrayList<String>(survivourPools);
            list.retainAll(gcInfo.getMemoryUsageAfterGc().keySet());
            return list;
        }

        @Override
        public Collection<String> getOldSpacePools() {
            List<String> list = new ArrayList<String>(oldPools);
            list.retainAll(gcInfo.getMemoryUsageAfterGc().keySet());
            return list;
        }

        @Override
        public Collection<String> getPermSpacePools() {
            List<String> list = new ArrayList<String>(permPools);
            list.retainAll(gcInfo.getMemoryUsageAfterGc().keySet());
            return list;
        }
    }
}
