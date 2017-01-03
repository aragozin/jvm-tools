package org.gridkit.jvmtool.gcmon;

public interface GarbageCollectionEventConsumer {

    public void consume(GarbageCollectionSummary eventInfo);

}
