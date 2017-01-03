package org.gridkit.jvmtool.event;

public class SimpleErrorEvent implements ErrorEvent {

    private final Exception error;

    public SimpleErrorEvent(Exception e) {
        this.error = e;
    }

    @Override
    public String message() {
        return error.getMessage();
    }

    @Override
    public Exception exception() {
        return error;
    }
}
