package org.gridkit.jvmtool.event;

import java.util.Iterator;

/**
 * This interface is used to iterate though event sequence.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public interface EventReader<T extends Event> extends Iterable<T>, Iterator<T> {

    public <M extends Event> EventReader<M> morph(EventMorpher<T, M> morpher);

    /**
     * Return next element without advancing iterator.
     */
    public T peekNext();

    public void dispose();
}
