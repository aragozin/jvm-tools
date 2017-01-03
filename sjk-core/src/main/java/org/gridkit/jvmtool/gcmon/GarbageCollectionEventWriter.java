package org.gridkit.jvmtool.gcmon;

import org.gridkit.jvmtool.event.TypedEventWriter;

public interface GarbageCollectionEventWriter extends TypedEventWriter {

    public void storeGcEvent(GarbageCollectionEvent event);
}
