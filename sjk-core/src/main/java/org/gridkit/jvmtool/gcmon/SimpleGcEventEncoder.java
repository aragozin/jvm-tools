package org.gridkit.jvmtool.gcmon;

public class SimpleGcEventEncoder implements GarbageCollectionEventConsumer {

    private final GarbageCollectionEventWriter writer;

    public SimpleGcEventEncoder(GarbageCollectionEventWriter writer) {
        this.writer = writer;
    }

    @Override
    public void consume(GarbageCollectionSummary eventInfo) {
        GarbageCollectionEventPojo pojo = new GarbageCollectionEventPojo();
        pojo.loadFrom(eventInfo);
        writer.storeGcEvent(pojo);
    }
}
