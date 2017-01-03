package org.gridkit.jvmtools.gcmon;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import org.gridkit.jvmtool.gcmon.GarbageCollectionEvent;
import org.gridkit.jvmtool.gcmon.GarbageCollectionEventWriter;
import org.gridkit.jvmtool.gcmon.GcEventSubscriber;
import org.gridkit.jvmtool.gcmon.SimpleGcEventEncoder;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class GcEventSubscriberTest {

    private List<GarbageCollectionEvent> events = new ArrayList<GarbageCollectionEvent>();
    private SimpleGcEventEncoder encoder = new SimpleGcEventEncoder(new GarbageCollectionEventWriter() {

        @Override
        public synchronized void storeGcEvent(GarbageCollectionEvent event) {
            events.add(event);
        }

        public void close() {};
    });

    @Test @Ignore("GC notifications are not supported on Java 6")
    public void capture_gc_sample() throws InterruptedException {

        GcEventSubscriber poller = new GcEventSubscriber(ManagementFactory.getPlatformMBeanServer(), encoder);

        Assert.assertTrue(poller.subscribe());

        events.clear();

        Assert.assertTrue("No events", events.isEmpty());

        System.gc();

        Thread.sleep(100);

        Assert.assertFalse("One or more events excepted", events.isEmpty());
    }
}
