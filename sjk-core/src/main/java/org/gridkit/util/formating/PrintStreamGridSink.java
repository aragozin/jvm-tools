package org.gridkit.util.formating;

import java.io.PrintStream;

public class PrintStreamGridSink implements GridSink {

    private final PrintStream out;
    private final String separator;
    private boolean leftMost = true;

    public PrintStreamGridSink(PrintStream out) {
        this(out, "\t");
    }

    public PrintStreamGridSink(PrintStream out, String separator) {
        this.out = out;
        this.separator = separator;
    }

    @Override
    public GridSink append(Object value) {
        if (!leftMost) {
            out.print(separator);
        }
        if (value != null) {
            out.print(String.valueOf(value));
        }
        leftMost = false;
        return this;
    }

    @Override
    public GridSink endOfRow() {
        out.println();
        leftMost = true;
        return this;
    }
}
