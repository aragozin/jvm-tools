/**
 * Copyright 2012 Alexey Ragozin
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
package org.gridkit.benchmark.gc;

import java.io.IOException;
import java.io.Serializable;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
@SuppressWarnings("restriction")
@Parameters(commandDescriptionKey="ygc", commandDescription = "Young GC benchmark")
public class YoungGCPauseBenchmark {

    public enum DataMode {
        STRING,
        LONG,
        INT,
        CONST,
    }

    private Random random = new Random(0);
    private Map<Integer, Object> maps[];

    @Parameter(names = {"-es", "--entry-size"}, description = "Memory size of entry")
    int entrySize = -1;

    @Parameter(names = {"-h", "--head-room"}, description = "Reserved portion of old space in MiB")
    int headRoom = 256;

    @Parameter(names = {"-m", "--data-mode"}, description = "Type of garbage data")
    DataMode mode = DataMode.STRING;

    @Parameter(names = {"-l", "--string-len"}, description = "Average length of string")
    int stringLen = 64;

    @Parameter(names = {"-d", "--dry-mode"}, description = "After filling old space, test will stop dirting old space. This mode should exclude dirty page scanning from measurement.")
    boolean dryMode = false;

    @Parameter(names = {"--max-time"}, description = "Benchmark time limit (sec)")
    int maxTime = 60;

    @Parameter(names = {"--max-old"}, description = "Number of old collections to benchmark")
    int maxOld = -1;

    @Parameter(names = {"--max-young"}, description = "Number of young collections to benchmark")
    int maxYoung = -1;

    @Parameter(names = {"--min-time"}, description = "Minimum benchmark time limit (sec)")
    int minTime = -1;

    @Parameter(names = {"--min-old"}, description = "Minimum number of old collections to measure")
    int minOld = -1;

    @Parameter(names = {"--min-young"}, description = "Minimum number of young collections to measure")
    int minYoung = -1;

    @Parameter(names = {"-r", "--overide-rate"}, description = "Chance that new object would be put to the map (otherwise existing would be reiserted)")
    double overrideRate = 0.1;

    @Parameter(names = {"-e", "--print-events"}, description = "Print GC events to console")
    boolean printEvents = false;

    private int count;

    private com.sun.management.GarbageCollectorMXBean oldGcMBean;
    private GarbageCollectorMXBean youngGcMBean;

    private MemoryPoolMXBean oldMemPool;
    private double activeOverrideRate = 1.1;


    private static char[] STRING_TEMPLATE;

    private static List<String> OLD_POOLS = Arrays.asList("Tenured Gen", "PS Old Gen", "CMS Old Gen", "G1 Old Gen", "Old Space");
    private static List<String> CONC_MODE = Arrays.asList("CMS Old Gen", "G1 Old Gen");
    private boolean concurentMode;

    private int align(int size, int al) {
        return (size + al - 1) & (~(al - 1));
    }

    public TestResult benchmark() throws IOException {

        STRING_TEMPLATE = new char[stringLen];

        if (mode == DataMode.CONST) {
            overrideRate = 1.1d;
        }

        System.out.println("Java: " + System.getProperty("java.version") + " VM: " + System.getProperty("java.vm.version"));
        System.out.println("Data model: " + Integer.getInteger("sun.arch.data.model"));

        long tenuredSize = Runtime.getRuntime().maxMemory();
        concurentMode = false;

        // getting more accurate data
        for(MemoryPoolMXBean bean : ManagementFactory.getMemoryPoolMXBeans()) {
            if (CONC_MODE.contains(bean.getName())) {
                concurentMode = true;
            }
            if (OLD_POOLS.contains(bean.getName())) {
                tenuredSize = bean.getUsage().getMax();
                if (tenuredSize < 0) {
                    tenuredSize = bean.getUsage().getCommitted();
                }
                System.out.println("Exact old space size is " + (tenuredSize) + " bytes");
                oldMemPool = bean;
                break;
            }
        }

        beans:
        for(GarbageCollectorMXBean bean :  ManagementFactory.getGarbageCollectorMXBeans()) {
            for(String pool: bean.getMemoryPoolNames()) {
                if (OLD_POOLS.contains(pool)) {
                    oldGcMBean = (com.sun.management.GarbageCollectorMXBean) bean;
                    continue beans;
                }
                else {
                }
            }
            youngGcMBean = bean;
        }

        boolean entrySizeAdjust = false;
        if (entrySize <= 0) {
            entrySizeAdjust = true;
            System.out.println("Estimating entry memory footprint ...");
            System.gc();
            long used = getOldSpaceUsed();

            int testSize = 100000;
            initMaps(testSize);

             while(size() < testSize) {
                 populateMap(concurentMode, testSize);
            }
             System.gc();
             long footPrint = getOldSpaceUsed() - used;
             entrySize = align((int) (footPrint / testSize), 32);
             System.out.println("Entry footprint: " + entrySize);
             maps = null;
        }

        System.gc();
        long oldSpaceUsed = getOldSpaceUsed();
        long freeTenured = tenuredSize - getOldSpaceUsed();
        calculateCount(freeTenured);

        if (concurentMode) {
            System.out.println("Concurent mode is enabled");
            System.out.println("Available old space: " + (freeTenured >> 20) + "MiB");
        }
        else {
            System.out.println("Available old space: " + (freeTenured >> 20) + "MiB (-" + headRoom + " MiB)");
        }
        if (count < 0) {
            System.out.println("Heap size is too small, increase heap size or reduce headroom");
            return null;
        }
        System.out.println("Young space collector: " + youngGcMBean.getName());
        System.out.println("Old space collector: " + oldGcMBean.getName());

        System.out.println("Populating - " + count);
        initMaps(count);
        int n = 0;
        if (entrySizeAdjust) {
            int targetSize = 4 * count / 5;
            while(size() < targetSize) {
                populateMap(concurentMode, count);
                n++;
            }
            System.gc();
            System.gc();
            long footPrint = getOldSpaceUsed() - oldSpaceUsed;
            entrySize = align((int) (footPrint / size()), 32);
            System.out.println("Adjusted entry footprint: " + entrySize);
            calculateCount(freeTenured);
        }

         while(size() < count) {
             populateMap(concurentMode, count);
             n++;
        }

         if (concurentMode) {
             while(--n > 0) {
                 processMap(false);
             }
         }

         // Let's wait for at least one major collection to complete
         if (!oldGcMBean.getName().startsWith("G1")) {
             // in G1 incremental collections are not treated as full
             // so we have to ignore this
             if (oldGcMBean != null) {
                 long c = oldGcMBean.getCollectionCount();
                 while(c == oldGcMBean.getCollectionCount()) {
                     processMap(false);
                 }
             }
         }

         System.out.println("Size: " + size());
         if (!dryMode) {
             System.out.println("Processing ... ");
         }
         else {
             System.out.println("Processing ... (DRY MODE ENABLED)");
         }
         StringBuffer sb = new StringBuffer();
         sb.append("Limits:");
         if (maxTime > 0) {
             sb.append(" ").append(maxTime + " sec");
         }
         if (maxOld > 0) {
             sb.append(" ").append(maxOld + " old collections");
         }
         if (maxYoung > 0) {
             sb.append(" ").append(maxYoung + " young collections");
         }
         System.out.println(sb.toString());

         activeOverrideRate = overrideRate;

         YoungGcTimeTracker tracker = new YoungGcTimeTracker();
         tracker.init();

         // start count down here
         long startTime = System.currentTimeMillis();
         long oldC = oldGcMBean.getCollectionCount();
         long youngC = youngGcMBean.getCollectionCount();

         while(true) {
            processMap(dryMode);
            tracker.probe();

            if (maxOld > 0) {
                if (oldGcMBean.getCollectionCount() - oldC > maxOld) {
                    break;
                }
            }
            if (maxYoung > 0) {
                if (youngGcMBean.getCollectionCount() - youngC > maxYoung) {
                    break;
                }
            }
            if (maxTime > 0 && (System.currentTimeMillis() > (startTime + TimeUnit.SECONDS.toMillis(maxTime)))) {
                break;
            }
        }

         System.out.println("Benchmark complete");
        return tracker.result();
    }

    private long getOldSpaceUsed() {
        long usage = oldMemPool.getUsage().getUsed();
        if (usage < 0) {
            return oldGcMBean.getLastGcInfo().getMemoryUsageAfterGc().get(oldMemPool.getName()).getUsed();
        }
        else {
            return usage;
        }
        //return jstatLong("sun.gc.generation.1.space.0.used");
    }

    private void calculateCount(long tenuredSize) {
        count = (int) ((tenuredSize - (headRoom << 20)) / entrySize);
        if (concurentMode) {
            count /= 2;
        }
    }

    @SuppressWarnings("unchecked")
    private void initMaps(int entryCount) {
        maps = new Map[(entryCount + 200000 - 1) / 200000];
        for(int i = 0; i != maps.length; ++i) {
            maps[i] = new HashMap<Integer, Object>(250000 >> 8);
        }
    }

    private void processMap(boolean dry) {
        boolean remove = size() > 1.01 * count;
        for(int i = 0; i != 1000; ++i) {
            if ((remove) && random.nextBoolean()) {
                if (dry) {
                    dryRemoveRandom(count);
                }
                else {
                    removeRandom(count);
                }
            }
            else {
                if (dry) {
                    dryPutRandom(count);
                }
                else {
                    putRandom(count);
                }
            }
        }
    }

    private void populateMap(boolean concurentMode, int count) {
        for(int i = 0; i != 1000; ++i) {
            putRandom(count);
            if (concurentMode & random.nextInt(10) > 7) {
                removeRandom(count);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private int size() {
        int size = 0;
        for(Map map: maps) {
            size += map.size();
        }
        return size;
    }

    private Object newObject() {
        switch (mode) {
        case STRING: return new String(STRING_TEMPLATE);
        case INT: return new Integer(random.nextInt());
        case LONG: return new Long(random.nextInt());
        case CONST: return this;
        }
        return null;
    }

    private void putRandom(int count) {
        int key = random.nextInt(2 * count);
        if (Math.abs(random.nextDouble()) < activeOverrideRate) {
            Object val = newObject();
            maps[key % maps.length].put(new Integer(key), val);
        }
        else {
            Integer ik = new Integer(key);
            Object v = maps[key % maps.length].get(ik);
            if (v != null) {
                maps[key % maps.length].put(ik, v);
            }
        }
    }

    private void dryPutRandom(int count) {
        int key = random.nextInt(2 * count);
        Object val = newObject();
        val.equals(maps[key % maps.length].get(key));
    }

    private void removeRandom(int count) {
        int key = random.nextInt(2 * count);
        maps[key % maps.length].remove(key);
    }

    private void dryRemoveRandom(int count) {
        int key = random.nextInt(2 * count);
        maps[key % maps.length].get(key);
    }

    private class YoungGcTimeTracker {

        private long totalTime = 0;
        private long evenCount = 0;
        private double squareTotal = 0;

        private long lastTime;
        private long lastYoungCount;
        private long lastOldCount;

        public void init() {
            while(true) {
                long ygc = youngGcMBean.getCollectionCount();
                long ogc = oldGcMBean.getCollectionCount();
                long yt = youngGcMBean.getCollectionTime();
                if (youngGcMBean.getCollectionCount() == ygc
                        || oldGcMBean.getCollectionCount() == ogc) {
                    lastTime = yt;
                    lastYoungCount = ygc;
                    lastOldCount = ogc;
                    break;
                }
            }
        }

        public void probe() {
            while(true) {
                long ygc = youngGcMBean.getCollectionCount();
                if (ygc == lastYoungCount) {
                    return;
                }
                long ogc = oldGcMBean.getCollectionCount();
                long yt = youngGcMBean.getCollectionTime();
                if (youngGcMBean.getCollectionCount() == ygc
                        || oldGcMBean.getCollectionCount() == ogc) {

                    long ycd = ygc - lastYoungCount;
                    long ocd = ogc - lastOldCount;
                    long td = yt - lastTime;

                    lastYoungCount = ygc;
                    lastOldCount = ogc;
                    lastTime = yt;

                    if (!concurentMode && ocd > 0) {
                        // ignoring young part of full gc
                        ycd -= ocd;
                    }

                    if (ycd > 0) {
                        totalTime += td;
                        evenCount += ycd;
                        double avg = ((double)td)/ycd;
                        squareTotal += ycd * avg * avg;

                        if (printEvents) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("Young GC (" + ycd + " events), total time: " + td + "ms, (Old events: " + ogc + ")");
                            System.out.println(sb);
                        }
                    }

                    break;
                }
            }
        }

        public TestResult result() {
            TestResult result = new TestResult();
            result.totalSquareTime = squareTotal;
            result.totalTime = totalTime;
            result.youngGcCount = evenCount;
            return result;
        }

    }

    public static class TestResult implements Serializable {

        private static final long serialVersionUID = 20130518L;

        public long totalTime;
        public double totalSquareTime;
        public long youngGcCount;

        public double getAverage() {
            double avg = ((double)totalTime) / youngGcCount;
            return avg;
        }

        public double getStdDev() {
            double avg = ((double)totalTime) / youngGcCount;
            double stdDev = Math.sqrt((totalSquareTime / youngGcCount) - (avg * avg));
            return stdDev;
        }

        public String toString() {
            double avg = ((double)totalTime) / youngGcCount;
            double stdDev = Math.sqrt((totalSquareTime / youngGcCount) - (avg * avg));
            return String.format("%f [%f] ms", avg, stdDev);
        }
    }
}
