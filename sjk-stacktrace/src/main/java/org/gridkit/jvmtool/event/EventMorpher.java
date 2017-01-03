package org.gridkit.jvmtool.event;

public interface EventMorpher<S extends Event, T extends Event> {

    /**
     * @return transformed event or null if event should be skipped
     */
    public T morph(S event);

}
