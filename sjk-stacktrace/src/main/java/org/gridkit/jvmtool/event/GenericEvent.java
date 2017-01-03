package org.gridkit.jvmtool.event;

public class GenericEvent implements CommonEvent {

    private long timestamp;
    private SimpleTagCollection tags = new SimpleTagCollection();
    private SimpleCounterCollection counters = new SimpleCounterCollection();

    public GenericEvent() {
    }

    public GenericEvent(CommonEvent that) {
        copyCommonEventFrom(that);
    }

    @Override
    public long timestamp() {
        return timestamp;
    }

    public void timestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public SimpleCounterCollection counters() {
        return counters;
    }

    @Override
    public SimpleTagCollection tags() {
        return tags;
    }

    protected void copyCommonEventFrom(CommonEvent event) {
        timestamp(event.timestamp());
        counters.clear();
        counters.setAll(event.counters());
        tags.clear();
        tags.putAll(event.tags());
    }
}
