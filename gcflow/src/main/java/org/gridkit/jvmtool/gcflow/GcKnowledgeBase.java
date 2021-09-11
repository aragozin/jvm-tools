package org.gridkit.jvmtool.gcflow;

import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

class GcKnowledgeBase {

    enum PoolType {

        EDEN,
        SURVIVOR,
        TENURED,
        PERMANENT

    }

    private static GcTypeMatcher[] GC_CATALOG = {
        // HotSpot default
        eden("Copy", "Eden Space"),
        survivour("Copy", "Survivor Space"),
        tenured("MarkSweepCompact", "Tenured Gen"),
        permanent("MarkSweepCompact", "Perm Gen"),
        permanent("MarkSweepCompact", "Perm Gen [shared-ro]"),
        permanent("MarkSweepCompact", "Perm Gen [shared-rw]"),

        // HotSpot Parallel Scavenge and Parallel Old GC
        eden("PS Scavenge", "PS Eden Space"),
        survivour("PS Scavenge", "PS Survivor Space"),
        tenured("PS MarkSweep", "PS Old Gen"),
        permanent("PS MarkSweep", "PS Perm Gen"),

        // Concurrent Mark Sweep
        eden("ParNew", "Par Eden Space"),
        survivour("ParNew", "Par Survivor Space"),
        tenured("ConcurrentMarkSweep", "CMS Old Gen"),
        permanent("ConcurrentMarkSweep", "CMS Perm Gen"),

        // G1
        eden("G1 Young Generation", "G1 Eden"),
        survivour("G1 Young Generation", "G1 Survivor"),
        tenured("G1 Old Generation", "G1 Old Gen"),
        permanent("G1 Old Generation", "G1 Perm Gen"),

        // JRockit
        eden("JRockit", "Nursery"),
        // no separate survivor space
        tenured("JRockit", "Old Space"),
        permanent("JRockit", "Class Memory"),
    };

    public static ObjectName RUNTIME_MXBEAN = name(ManagementFactory.RUNTIME_MXBEAN_NAME);

    public static ObjectName COLLECTORS_PATTERN = name("java.lang:type=GarbageCollector,name=*");

    private static ObjectName name(String name) {
        try {
            return new ObjectName(name);
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }
    }


    public static Collection<String> allCollectedPools(MBeanServerConnection conn) throws IOException {

        Set<String> pools = new LinkedHashSet<String>();

        for(ObjectName gcn: conn.queryNames(COLLECTORS_PATTERN, null)) {
            GarbageCollectorMXBean gc = JMX.newMXBeanProxy(conn, gcn, GarbageCollectorMXBean.class);

            for(String pool: gc.getMemoryPoolNames()) {
                pools.add(pool);
            }
        }
        return pools;
    }

    public static Map<PoolType, Collection<String>> classifyMemoryPools(MBeanServerConnection conn) throws IOException {

        RuntimeMXBean rtmx = JMX.newMXBeanProxy(conn, RUNTIME_MXBEAN, RuntimeMXBean.class);

        boolean jrockit = rtmx.getVmName().toUpperCase().contains("JROCKIT");
        Map<PoolType, Collection<String>> map = new HashMap<GcKnowledgeBase.PoolType, Collection<String>>();
        for(ObjectName gcn: conn.queryNames(COLLECTORS_PATTERN, null)) {
            GarbageCollectorMXBean gc = JMX.newMXBeanProxy(conn, gcn, GarbageCollectorMXBean.class);

            String gcName = jrockit ? "JRockit" : gc.getName();
            for(String pool: gc.getMemoryPoolNames()) {
                PoolType type = classify(gcName, pool);
                if (type != null) {
                    add(map, type, pool);
                }
            }
        }
        return map;
    }

    public static PoolType classify(String gcName, String pool) {
        for(GcTypeMatcher m: GC_CATALOG) {
            if (m.gcName.equals(gcName) && m.poolName.equals(pool)) {
                return m.type;
            }
        }
        return null;
    }

    private static void add(Map<PoolType, Collection<String>> map, PoolType type, String name) {
        if (map.containsKey(type)) {
            List<String> names = new ArrayList<String>();
            names.addAll(map.get(type));
            names.add(name);
            map.put(type, names);
        }
        else {
            map.put(type, Collections.singleton(name));
        }
    }

    private static GcTypeMatcher eden(String algo, String poolName) {
        return new GcTypeMatcher(algo, poolName, PoolType.EDEN);
    }

    private static GcTypeMatcher survivour(String algo, String poolName) {
        return new GcTypeMatcher(algo, poolName, PoolType.SURVIVOR);
    }

    private static GcTypeMatcher tenured(String algo, String poolName) {
        return new GcTypeMatcher(algo, poolName, PoolType.TENURED);
    }

    private static GcTypeMatcher permanent(String algo, String poolName) {
        return new GcTypeMatcher(algo, poolName, PoolType.PERMANENT);
    }

    private static class GcTypeMatcher {

        String gcName;
        String poolName;
        PoolType type;

        public GcTypeMatcher(String gcName, String poolName, PoolType type) {
            this.gcName = gcName;
            this.poolName = poolName;
            this.type = type;
        }
    }
}
