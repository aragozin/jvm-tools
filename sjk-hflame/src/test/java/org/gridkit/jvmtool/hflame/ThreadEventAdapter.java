package org.gridkit.jvmtool.hflame;

import java.io.IOException;

import org.gridkit.jvmtool.codec.stacktrace.ThreadSnapshotEventPojo;
import org.gridkit.jvmtool.event.UniversalEventWriter;
import org.gridkit.jvmtool.stacktrace.StackTraceWriter;
import org.gridkit.jvmtool.stacktrace.ThreadSnapshot;

class ThreadEventAdapter implements StackTraceWriter {

    final UniversalEventWriter writer;

    public ThreadEventAdapter(UniversalEventWriter writer) {
        this.writer = writer;
    }

    @Override
    public void write(ThreadSnapshot snap) throws IOException {
        ThreadSnapshotEventPojo pojo = new ThreadSnapshotEventPojo();
        pojo.loadFrom(snap);
        writer.store(pojo);
    }

    @Override
    public void close() {
    	try {
    		writer.close();
    	}
    	catch(IOException e) {
    		throw new RuntimeException(e);
    	}
    }
}