package org.gridkit.jvmtool.gcflow;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.junit.Test;

public class SelfReporterTest {

    private List<GcAdapter> adapters = new ArrayList<GcAdapter>();

    public void run(int seconds) {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(seconds);
        while(System.currentTimeMillis() < deadline) {
            for(GcAdapter a: adapters) {
                a.report();
            }
            long nextRep = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(50);
            while(System.nanoTime() < nextRep) {
                Random rnd = new Random();
                List<String> garbage = new ArrayList<String>();
                for(int i = 0; i != 10000; ++i) {
                    int n = rnd.nextInt(512) + 128;
                    StringBuilder sb = new StringBuilder();
                    for(int j = 0; j != n; ++j) {
                        sb.append(rnd.nextInt());
                    }
                    garbage.add(sb.toString());
                }
            }
            LockSupport.parkNanos(1);
        }
    }

    public void addToAll(GarbageCollectionSampler sampler) throws MalformedObjectNameException, IOException {
        for(ObjectName gc: ManagementFactory.getPlatformMBeanServer().queryNames(GcKnowledgeBase.COLLECTORS_PATTERN, null)) {
            GcAdapter a = new GcAdapter(ManagementFactory.getPlatformMBeanServer(), gc, sampler);
            adapters.add(a);
        }
    }

    @Test
        public void simple_report() throws MalformedObjectNameException, IOException {
            addToAll(new SimpleReporter());
            run(20);
        }

    private final class SimpleReporter implements GarbageCollectionSampler {
        @Override
        public void report(String algoName, int eventsMissed, GcReport info) {
            if (eventsMissed > 0) {
                System.out.println("Missed " + eventsMissed + " events for [" + algoName + "]");
            }
            StringBuilder sb = new StringBuilder();
            sb.append("[" + algoName + "] #" + info.getId()).append(' ');
            reportSize(info, sb, info.getAllMemoryPools());
            sb.append('\n');
            sb.append("  ");
            Collection<String> eden = info.getEdenPools();
            Collection<String> surv = info.getSurvivourPools();
            Collection<String> old = info.getOldSpacePools();
            Collection<String> perm = info.getPermSpacePools();
            if (!eden.isEmpty()) {
                sb.append("EDEN[");
                reportSize(info, sb, eden);
                sb.append("] ");
            }
            if (!surv.isEmpty()) {
                sb.append("SURVIVOUR[");
                reportSize(info, sb, surv);
                sb.append("] ");
            }
            if (!old.isEmpty()) {
                sb.append("OLD[");
                reportSize(info, sb, old);
                sb.append("] ");
            }
            if (!perm.isEmpty() && info.getSizeBefore(perm) != info.getSizeAfter(perm)) {
                sb.append("PERM[");
                reportSize(info, sb, perm);
                sb.append("] ");
            }
            sb.append('\n');

            System.out.print(sb);
        }

        public void reportSize(GcReport info, StringBuilder sb, Collection<String> pools) {
            long delta = info.getSizeAfter(pools) - info.getSizeBefore(pools);
            sb.append(FormatHelper.toMemoryUnits(info.getSizeBefore(pools))).append('B');
            if (delta != 0) {
                sb.append("->")
                .append(FormatHelper.toMemoryUnits(info.getSizeAfter(pools))).append('B');
                if (delta < 0) {
                    sb.append(" -").append(FormatHelper.toMemoryUnits(-delta)).append('B');
                }
                else if (delta > 0){
                    sb.append(" +").append(FormatHelper.toMemoryUnits(delta)).append('B');
                }
            }
        }
    }
}
