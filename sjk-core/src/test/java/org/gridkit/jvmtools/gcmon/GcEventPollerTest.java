package org.gridkit.jvmtools.gcmon;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import org.gridkit.jvmtool.gcmon.GarbageCollectionEvent;
import org.gridkit.jvmtool.gcmon.GarbageCollectionEventWriter;
import org.gridkit.jvmtool.gcmon.GcEventPoller;
import org.gridkit.jvmtool.gcmon.SimpleGcEventEncoder;
import org.junit.Assert;
import org.junit.Test;

public class GcEventPollerTest {

    private List<GarbageCollectionEvent> events = new ArrayList<GarbageCollectionEvent>();
    private SimpleGcEventEncoder encoder = new SimpleGcEventEncoder(new GarbageCollectionEventWriter() {

        @Override
        public void storeGcEvent(GarbageCollectionEvent event) {
            events.add(event);
        }

        public void close() {};
    });

    @Test
    public void capture_gc_sample() {

        GcEventPoller poller = new GcEventPoller(ManagementFactory.getPlatformMBeanServer(), encoder);

        events.clear();

        System.gc();

        Assert.assertTrue("No events", events.isEmpty());

        poller.run();

        Assert.assertFalse("One or more events excepted", events.isEmpty());
    }


}
