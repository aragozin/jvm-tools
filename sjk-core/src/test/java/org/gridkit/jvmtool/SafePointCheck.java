package org.gridkit.jvmtool;

import java.lang.management.ManagementFactory;

import org.junit.Test;

public class SafePointCheck {

    @SuppressWarnings("restriction")
    @Test
    public void test_jmx() throws InterruptedException {
        Thread t = new Thread() {
            double x = 0.1;

            @Override
            public void run() {
                try {
                    while(true) {
                        try {
                            Thread.sleep(1);
                            for(int i = 0; i != 10000000; ++i) {
                                x *= 1.001;
                            }
                        } catch (InterruptedException e) {
                        }
                    }
                }
                finally {
                    if (x < 0) {
                        System.out.println("");
                    }
                }
            }
        };

        t.setDaemon(true);
        t.start();

        Thread.sleep(5000);

        while(true) {
            long val = ((com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean()).getThreadCpuTime(t.getId());
//            long val = ((com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean()).getThreadAllocatedBytes(t.getId());
            System.out.println(val);
            Thread.sleep(2000);
        }
    }

}
