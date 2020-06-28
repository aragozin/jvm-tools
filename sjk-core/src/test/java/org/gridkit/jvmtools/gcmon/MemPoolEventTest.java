package org.gridkit.jvmtools.gcmon;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import org.gridkit.jvmtool.gcmon.MemoryPoolEventConsumer;
import org.gridkit.jvmtool.gcmon.MemoryPoolInfoEvent;
import org.gridkit.jvmtool.gcmon.MemoryPoolPoller;
import org.junit.Test;

public class MemPoolEventTest {

    @Test
    public void pollerSmoke() {
        MemoryPoolPoller poller = new MemoryPoolPoller(ManagementFactory.getPlatformMBeanServer(), new Consumer());

        poller.poll();

        List<Object> buf = new ArrayList<Object>();
        for(int i = 0; i != 10000; ++i) {
            if (i < 100) {
                buf.add(new byte[1024]);
            }
            buf.set(i % 100, new byte[1024]);
        }

        poller.poll();

        for(int i = 0; i != 10000; ++i) {
            buf.set(i % 100, new byte[1024]);
        }

        poller.poll();
    }

    public class Consumer implements MemoryPoolEventConsumer {

        @Override
        public void consumeUsageEvent(MemoryPoolInfoEvent event) {
            System.out.println(event.timestamp() + " consumeUsageEvent: " + event.tags() + " " + event.counters());
        }

        @Override
        public void consumePeakEvent(MemoryPoolInfoEvent event) {
            System.out.println(event.timestamp() + " consumePeakEvent: " + event.tags() + " " + event.counters());
        }

        @Override
        public void consumeCollectionUsageEvent(MemoryPoolInfoEvent event) {
            System.out.println(event.timestamp() +  " consumeCollectionUsageEvent: " + event.tags() + " " + event.counters());
        }
    }
}
