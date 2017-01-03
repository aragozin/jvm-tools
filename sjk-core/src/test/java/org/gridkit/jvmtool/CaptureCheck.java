package org.gridkit.jvmtool;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServerConnection;

import org.gridkit.jvmtool.codec.stacktrace.ThreadSnapshotEvent;
import org.gridkit.jvmtool.codec.stacktrace.ThreadSnapshotEventPojo;
import org.gridkit.jvmtool.codec.stacktrace.ThreadSnapshotWriter;
import org.gridkit.jvmtool.event.Event;
import org.gridkit.jvmtool.event.EventReader;
import org.gridkit.jvmtool.event.TypedEventWriterProxy;
import org.gridkit.jvmtool.event.UniversalEventWriter;
import org.gridkit.jvmtool.gcmon.GarbageCollectionEvent;
import org.gridkit.jvmtool.gcmon.GarbageCollectionEventWriter;
import org.gridkit.jvmtool.gcmon.GcEventSubscriber;
import org.gridkit.jvmtool.gcmon.SimpleGcEventEncoder;
import org.gridkit.jvmtool.stacktrace.StackTraceWriter;
import org.gridkit.jvmtool.stacktrace.ThreadDumpSampler;
import org.gridkit.jvmtool.stacktrace.ThreadEventCodec;
import org.gridkit.jvmtool.stacktrace.ThreadMXBeanEx;
import org.gridkit.jvmtool.stacktrace.ThreadSnapshot;
import org.gridkit.lab.jvm.attach.AttachManager;
import org.junit.Test;

public class CaptureCheck {

    private static long PID;
    static {
        String pid = ManagementFactory.getRuntimeMXBean().getName();
        pid = pid.substring(0, pid.indexOf('@'));
        PID = Long.valueOf(pid);
//        PID = xxx;
    }

    private String taget = "target/test.cap";

    private long captureTime = TimeUnit.SECONDS.toMillis(30);

    @Test
    public void capture() throws FileNotFoundException, IOException {
        File dump = new File(taget);

        if (dump.getParentFile() != null) {
            dump.getParentFile().mkdirs();
        }

        UniversalEventWriter writer = ThreadEventCodec.createEventWriter(new FileOutputStream(dump));

        MyEventWriter twriter = TypedEventWriterProxy.decorate(writer)
                .pass(ThreadSnapshotEvent.class)
                .pass(GarbageCollectionEvent.class)
                .facade(MyEventWriter.class);

        MBeanServerConnection conn = AttachManager.getJmxConnection(PID);

        GcEventSubscriber subscriber = new GcEventSubscriber(conn, new SimpleGcEventEncoder(twriter));
        if (!subscriber.subscribe()) {
            // polling fallback
            subscriber.schedule(500);
        }

        ThreadDumpSampler tdumper = new ThreadDumpSampler();
        ThreadMXBean threadMXBean = ThreadMXBeanEx.BeanHelper.connectThreadMXBean(conn);
        if (threadMXBean.isThreadContentionMonitoringSupported()) {
            threadMXBean.setThreadContentionMonitoringEnabled(true);
        }
        tdumper.connect(threadMXBean);

        long deadline = System.currentTimeMillis() + captureTime;

        ThreadEventAdapter threadWriter = new ThreadEventAdapter(twriter);

        while(System.currentTimeMillis() < deadline) {
            tdumper.collect(threadWriter);
        }

        twriter.close();

        System.out.println("Dump complete [" + dump.getPath() + "] " + dump.length() + " bytes");

        int tc = 0;
        int ntc = 0;

        EventReader<Event> reader = ThreadEventCodec.createEventReader(new FileInputStream(dump));
        for(Event e: reader) {
            if (e instanceof ThreadSnapshotEvent) {
                ++tc;
            }
            else {
                ++ntc;
            }
        }

        System.out.println("Thread events: " + tc + " Non thread events: " + ntc);
    }

    private static class ThreadEventAdapter implements StackTraceWriter {

        final ThreadSnapshotWriter writer;

        public ThreadEventAdapter(ThreadSnapshotWriter writer) {
            this.writer = writer;
        }

        @Override
        public void write(ThreadSnapshot snap) throws IOException {
            ThreadSnapshotEventPojo pojo = new ThreadSnapshotEventPojo();
            pojo.loadFrom(snap);
            writer.storeThreadEvent(pojo);
        }

        @Override
        public void close() {
            writer.close();
        }
    }

    public static interface MyEventWriter extends GarbageCollectionEventWriter, ThreadSnapshotWriter {

    }
}
