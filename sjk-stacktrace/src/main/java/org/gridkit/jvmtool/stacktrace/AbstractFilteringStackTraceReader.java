package org.gridkit.jvmtool.stacktrace;

import java.io.IOException;

/**
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public abstract class AbstractFilteringStackTraceReader extends StackTraceReader.StackTraceReaderDelegate {

    @Override
    public boolean loadNext() throws IOException {
        while(super.loadNext()) {
            if (evaluate()) {
                return true;
            }
        }
        return false;
    }

    protected abstract boolean evaluate();
}
