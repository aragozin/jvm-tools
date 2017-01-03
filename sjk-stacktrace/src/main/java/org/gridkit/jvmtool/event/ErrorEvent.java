package org.gridkit.jvmtool.event;

/**
 * <p>
 * This is fake event produced if error has accured during
 * decoding of event stream.
 * </p>
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public interface ErrorEvent extends Event {

    public String message();

    public Exception exception();
}
