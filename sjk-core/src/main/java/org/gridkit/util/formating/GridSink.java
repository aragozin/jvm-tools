package org.gridkit.util.formating;

public interface GridSink {

    public GridSink append(Object value);

    public GridSink endOfRow();

}
