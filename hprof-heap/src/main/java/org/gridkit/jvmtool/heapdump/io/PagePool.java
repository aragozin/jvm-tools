package org.gridkit.jvmtool.heapdump.io;

import java.nio.ByteBuffer;

public interface PagePool {

    public int getPageSize();
    
    public boolean hasFreePages();
    
    public ByteBuffer accurePage() throws NoMorePagesException;
    
    public void releasePage(ByteBuffer buffer);
    
    public static class NoMorePagesException extends RuntimeException {

        private static final long serialVersionUID = 20160903L;

        public NoMorePagesException() {
            super();
        }

        public NoMorePagesException(String message, Throwable cause) {
            super(message, cause);
        }

        public NoMorePagesException(String message) {
            super(message);
        }

        public NoMorePagesException(Throwable cause) {
            super(cause);
        }
    }
}
