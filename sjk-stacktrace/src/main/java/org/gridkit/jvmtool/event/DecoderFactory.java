package org.gridkit.jvmtool.event;

import java.io.InputStream;

public interface DecoderFactory {

    public boolean canHandle(String magic, Class<? extends Event> eventType);

    public <T extends Event> EventReader<T> createEventReader(String magic, InputStream stream, Class<? extends Event> eventType, ErrorHandler handler);

}
