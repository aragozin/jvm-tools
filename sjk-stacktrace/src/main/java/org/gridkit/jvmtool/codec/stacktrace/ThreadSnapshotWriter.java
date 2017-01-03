package org.gridkit.jvmtool.codec.stacktrace;

import org.gridkit.jvmtool.event.TypedEventWriter;

public interface ThreadSnapshotWriter extends TypedEventWriter {

    public void storeThreadEvent(ThreadSnapshotEvent event);

}
