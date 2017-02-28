package org.gridkit.jvmtool.heapdump.example;
import static org.gridkit.jvmtool.heapdump.HeapWalker.valueOf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.gridkit.jvmtool.heapdump.HeapClusterAnalyzer;
import org.gridkit.jvmtool.heapdump.HeapClusterAnalyzer.ClusterDetails;
import org.gridkit.jvmtool.heapdump.HeapHistogram;
import org.gridkit.jvmtool.heapdump.HeapHistogram.ClassRecord;
import org.gridkit.jvmtool.heapdump.StringCollector;
import org.gridkit.jvmtool.util.TextTable;
import org.junit.Before;
import org.junit.Test;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.HeapFactory;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;

/**
 * This class demonstrates few heap analyzing
 * techniques used for JBoss 6.1 server.
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class JBossServerDumpExample {

    Heap heap;
    
    @Before
    public void initHeapDump() throws IOException {
        String dumppath = ""; // path to dump file                
        heap = HeapFactory.createFastHeap(new File(dumppath));        
    }

    /** Reports retained size of string object in dump */
    @Test
    public void reportStrings() {
        StringCollector collector = new StringCollector();
        collector.collect(heap);
        System.out.println(collector);
    }

    /**
     * Create "jmap -histo" like class histogram from dump
     */
    @Test
    public void printHistogram() {
        StringCollector collector = new StringCollector();
        HeapHistogram histo = new HeapHistogram();
        collector.collect(heap, histo);
        List<ClassRecord> ht = new ArrayList<ClassRecord>(histo.getHisto());
        ht.add(collector.asClassRecord());
        Collections.sort(ht, HeapHistogram.BY_SIZE);
        TextTable tt = new TextTable();
        int n = 0;
        for(ClassRecord cr: ht.subList(0, 500)) {
            tt.addRow("" + (++n), " " + cr.getTotalSize(), " " + cr.getInstanceCount(), " " + cr.getClassName());
        }
        System.out.println(tt.formatTextTableUnbordered(1000));
    }    

    /**
     * This example finds and reports Hibernate entities in dump.
     */
    @Test
    public void printEnties() throws IOException {
     
        Map<String, Integer> histo = new TreeMap<String, Integer>(); 
        
        JavaClass jc = heap.getJavaClassByName("org.hibernate.engine.spi.EntityEntry");
        for(Instance i: jc.getInstances()) {
            try {
                String name = "" + valueOf(i, "entityName");
//                String rowId = "" + valueOf(i, "rowId");
                System.out.println(i.getInstanceId() + "\t" + name);
                int n = histo.containsKey(name) ? histo.get(name) : 0;
                histo.put(name, n + 1);
            }
            catch (IllegalArgumentException e) {
                System.err.println(e);
            }
        }        
        
        System.out.println();
        for(String type: histo.keySet()) {
            System.out.println(type + "\t" + histo.get(type));            
        }
    }

    /**
     * This example extract information related to infinispan
     * caches from dump.
     */
    @Test
    public void printCaches() throws IOException {
     
        HeapClusterAnalyzer hca = new HeapClusterAnalyzer(heap);
        hca.addEntryPoint("(**.CacheImpl).dataContainer.entries");
        hca.blackList("(**.DefaultDataContainer$*)");
        hca.blackList("(java.lang.reflect.**)");
        hca.blackList("(**.TransactionImple)");
        hca.blackList("(**.BoundedConcurrentHashMap$HashEntry).*");
        hca.blackList("(**.BoundedConcurrentHashMap$LRU)");
        hca.prepare();
        
        Map<String, Integer> histo = new TreeMap<String, Integer>(); 
        
        JavaClass jc = heap.getJavaClassByName("org.infinispan.CacheImpl");
        for(Instance i: jc.getInstances()) {
            try {
                String name = "" + valueOf(i, "name");
                ClusterDetails cluster = hca.feed(i);
                ClassRecord classInfo = cluster.getHistogram().getClassInfo("org.infinispan.util.concurrent.BoundedConcurrentHashMap$HashEntry");
                System.out.println(i.getInstanceId() + "\t" + name + "\t" + (classInfo == null ? "0" : classInfo.getInstanceCount()));
                int n = histo.containsKey(name) ? histo.get(name) : 0;
                histo.put(name, n + 1);
            }
            catch (IllegalArgumentException e) {
                System.err.println(e);
            }
        }                
    }

    /**
     * This examples calculates retained set for active HTTP sessions.
     * It also report top 50 entries of class histogram for each cluster.
     * 
     * @throws IOException
     */
    @Test
    public void printSessionClusters() throws IOException {
        HeapClusterAnalyzer analyzer = new HeapClusterAnalyzer(heap);
        
        analyzer.useBreadthSearch();
        
        analyzer.addEntryPoint("(**.StandardSession)");

        // this is a list of singleton objects we do not care
        loadSingletons(analyzer, "src/test/resources/singletons-list.txt");
        
        // below we are configuring edges we want to ignore
        // while traveling heap graph
        // HeapPath notation is used to define edges
        analyzer.blackList("(+java.lang.Enum)");        
        analyzer.blackList("(**.StandardSession).manager");        
        analyzer.blackList("(**.LogImpl)");        
        analyzer.blackList("(**.LogEvsAction*)");        
        analyzer.blackList("(**.SessionFactoryImpl)");        
        analyzer.blackList("(**.Settings)");        
        analyzer.blackList("(**.TransactionEnvironmentImpl)");        
        analyzer.blackList("(**.InjectedDataSourceConnectionProvider)");        
        analyzer.blackList("(**.WrapperDataSource)");        
        analyzer.blackList("(**.JdbcServicesImpl)");        
        analyzer.blackList("(**).log");        
        analyzer.blackList("(**.JBossLogManagerLogger)");        
        analyzer.blackList("(java.lang.reflect.Method)");        
        analyzer.blackList("(java.lang.reflect.Field)");        
        analyzer.blackList("(java.lang.reflect.Constructor)");        
        analyzer.blackList("(java.lang.Class)");        
        analyzer.blackList("(**.TransactionalAccess)");        
        analyzer.blackList("(**.BytecodeProviderImpl)");        
        analyzer.blackList("(**.LinkedHashMap$Entry).after");        
        analyzer.blackList("(**.LinkedHashMap$Entry).before");        
        analyzer.blackList("(sun.reflect.**)");        
        analyzer.blackList("(+java.lang.Throwable)");        
        analyzer.blackList("(+java.lang.Thread)");        
        analyzer.blackList("(java.lang.ref.Finalizer)");        
        analyzer.blackList("(org.jboss.jca.core.connectionmanager.pool.strategy.OnePool)");
        analyzer.blackList("(**.ReentrantLock)");
        analyzer.blackList("(**.SemaphoreArrayListManagedConnectionPool)");
        analyzer.blackList("(**.ImmediateValue).value");
        analyzer.blackList("(org.infinispan.**)");
        analyzer.blackList("(java.security.**)");
        
        analyzer.prepare();
        
        System.out.println("Scanning");
        
        Iterable<Instance> scanList = heap.getAllInstances();
        long lastId = 0;
        for(Instance i: scanList) {
            long id = i.getInstanceId();
            if (id < lastId) {
                System.out.println(lastId + " | " + id);
            }
            lastId = id;
        }
        for(Instance i: scanList) {
            ClusterDetails c = analyzer.feed(i);
            if (c != null) {
                System.out.println("Cluster " + c.getRootInstance().getInstanceId());
                System.out.println(c.getHistogram().formatTop(50));
                System.out.println();                
            }
        }
        
        System.out.println("\n\nShared error margin: " + analyzer.getSharedErrorMargin());
        System.out.println("Shared objects");
        System.out.println(analyzer.getSharedSummary().formatTop(100));
        
        System.out.println("\n\nCLUSTERS BY SIZE\n");
        
        List<ClusterDetails> list = new ArrayList<ClusterDetails>(analyzer.getClusters());
        Collections.sort(list, new Comparator<ClusterDetails>() {
            @Override
            public int compare(ClusterDetails o1, ClusterDetails o2) {
                return ((Long)o2.getHistogram().getTotalSize()).compareTo(o1.getHistogram().getTotalSize());
            }            
        });
        
        for(ClusterDetails c: list) {
            System.out.println("Cluster " + c.getRootInstance().getInstanceId());
            System.out.println(c.getHistogram().formatTop(50));
            System.out.println();                            
        }
        System.out.println(list.size() + " clusters");
    }    
    
    private void loadSingletons(HeapClusterAnalyzer hca, String path) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(path));
        String line;
        while((line = br.readLine()) != null) {
            line = line.trim();
            if (line.length() > 0) {
                hca.addTokenToBlacklist(line);
            }
        }
    }
}
