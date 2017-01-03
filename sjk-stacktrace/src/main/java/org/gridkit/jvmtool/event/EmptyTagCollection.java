package org.gridkit.jvmtool.event;

import java.util.Collections;
import java.util.Iterator;

class EmptyTagCollection implements TagCollection {

    @Override
    public Iterator<String> iterator() {
        return Collections.<String>emptyList().iterator();
    }

    @Override
    public Iterable<String> tagsFor(String key) {
        return Collections.<String>emptyList();
    }

    @Override
    public String firstTagFor(String key) {
        return null;
    }

    @Override
    public boolean contains(String key, String tag) {
        return false;
    }

    @Override
    public EmptyTagCollection clone() {
        return new EmptyTagCollection();
    }

    @Override
    public String toString() {
        return "[]";
    }
}
