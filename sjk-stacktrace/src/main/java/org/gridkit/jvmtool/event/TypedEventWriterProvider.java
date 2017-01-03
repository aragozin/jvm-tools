package org.gridkit.jvmtool.event;

public interface TypedEventWriterProvider {

    public <T extends Event> UniversalEventWriter getWriterFor(Class<T> eventInterface);

    public void close();

}
