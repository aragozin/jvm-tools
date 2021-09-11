package org.gridkit.jvmtool.gcflow;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.LogManager;

import org.gridkit.jvmtool.gcflow.GcKnowledgeBase.PoolType;
import org.gridkit.nanocloud.CloudFactory;
import org.gridkit.vicluster.ViManager;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.ViProps;
import org.gridkit.vicluster.telecontrol.jvm.JvmProps;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

public class GcKnowledgeBaseCheck {

    public static ViManager cloud = CloudFactory.createCloud();

    @BeforeClass
    public static void logOff() {
        LogManager.getLogManager().getLogger("").setLevel(Level.OFF);
    }

    @BeforeClass
    public static void initCloud() {
        ViProps.at(cloud.node("**")).setLocalType();
        JvmProps.at(cloud.node("**")).addJvmArg("-Djava.util.logging.config.class=" + LogOff.class.getName());
    }

    @AfterClass
    public static void killCloud() {
        cloud.shutdown();
    }

    @Test
    public void classify_local() throws IOException {
        dumpMemoryPools();
    }

    @Test
    public void classify_serial_gc() throws IOException {
        try {
            String gc = "-XX:+UseSerialGC";
            ViNode jvm = cloud.node(gc);
            JvmProps.at(jvm).addJvmArg(gc);
            jvm.exec(new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    dumpMemoryPools();
                    return null;
                }

            });
        } catch(Exception e) {
            Assume.assumeTrue(false);
        }
    }

    @Test
    public void classify_par_new_gc() throws IOException {
        try {
            String gc = "-XX:+UseParNewGC";
            ViNode jvm = cloud.node(gc);
            JvmProps.at(jvm).addJvmArg(gc);
            jvm.exec(new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    dumpMemoryPools();
                    return null;
                }

            });
        } catch(Exception e) {
            Assume.assumeTrue(false);
        }
    }

    @Test
    public void classify_ps_gc() throws IOException {
        try {
            String gc = "|-XX:+UseParallelGC|-XX:-UseParallelOldGC";
            ViNode jvm = cloud.node(gc);
            JvmProps.at(jvm).addJvmArg(gc);
            jvm.exec(new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    dumpMemoryPools();
                    return null;
                }

            });
        } catch(Exception e) {
            Assume.assumeTrue(false);
        }
    }

    @Test
    public void classify_par_old_gc() throws IOException {
        try {
            String gc = "-XX:+UseParallelOldGC";
            ViNode jvm = cloud.node(gc);
            JvmProps.at(jvm).addJvmArg(gc);
            jvm.exec(new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    dumpMemoryPools();
                    return null;
                }

            });
        } catch(Exception e) {
            Assume.assumeTrue(false);
        }
    }

    @Test
    public void classify_cms_def_new_gc() throws IOException {
        try {
            String gc = "|-XX:+UseConcMarkSweepGC|-XX:-UseParNewGC";
            ViNode jvm = cloud.node(gc);
            JvmProps.at(jvm).addJvmArg(gc);
            jvm.exec(new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    dumpMemoryPools();
                    return null;
                }

            });
        } catch(Exception e) {
            Assume.assumeTrue(false);
        }
    }

    @Test
    public void classify_cms_par_new_gc() throws IOException {
        try {
            String gc = "|-XX:+UseConcMarkSweepGC|-XX:+UseParNewGC";
            ViNode jvm = cloud.node(gc);
            JvmProps.at(jvm).addJvmArg(gc);
            jvm.exec(new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    dumpMemoryPools();
                    return null;
                }

            });
        } catch(Exception e) {
            Assume.assumeTrue(false);
        }
    }

    @Test
    public void classify_g1_gc() throws IOException {
        try {
            String gc = "-XX:+UseG1GC";
            ViNode jvm = cloud.node(gc);
            JvmProps.at(jvm).addJvmArg(gc);
            jvm.exec(new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    dumpMemoryPools();
                    return null;
                }

            });
        } catch(Exception e) {
            Assume.assumeTrue(false);
        }
    }

    public static void dumpMemoryPools() throws IOException {
        Map<PoolType, Collection<String>> pools = GcKnowledgeBase.classifyMemoryPools(ManagementFactory.getPlatformMBeanServer());
        for(PoolType pt: pools.keySet()) {
            System.out.println(pt.toString() + pools.get(pt));
        }
    }

}
