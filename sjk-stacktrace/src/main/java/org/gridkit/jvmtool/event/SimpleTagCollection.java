package org.gridkit.jvmtool.event;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;

public class SimpleTagCollection implements TagCollection {

    private SortedMap<Tag, Tag> tags = new TreeMap<Tag, Tag>();

    public SimpleTagCollection() {
    }

    public SimpleTagCollection(TagCollection that) {
        for(String key: that) {
            for(String tag: that.tagsFor(key)) {
                put(key, tag);
            }
        }
    }

    protected SimpleTagCollection(Iterable<Tag> tags) {
        for(Tag t: tags) {
            this.tags.put(t, t);
        }
    }

    @Override
    public Iterator<String> iterator() {
        return new KeyIterator(tags.keySet().iterator());
    }

    @Override
    public Iterable<String> tagsFor(final String key) {
        return new Iterable<String>() {
            @Override
            public Iterator<String> iterator() {
                return new TagIterator(key, tags.tailMap(new Tag(key, "")).keySet().iterator());
            }
        };
    }

    @Override
    public String firstTagFor(String key) {
        SortedMap<Tag, Tag> tail = tags.tailMap(new Tag(key, ""));
        if (!tail.isEmpty()) {
            Tag f = tail.firstKey();
            if (key.equals(f.tagKey)) {
                return f.tagValue;
            }
        }
        return null;
    }

    @Override
    public boolean contains(String key, String tag) {
        if (key == null) {
            throw new NullPointerException("'key' is null");
        }
        if (tag == null) {
            throw new NullPointerException("'tag' is null");
        }
        return tags.containsKey(new Tag(key, tag));
    }

    @Override
    public SimpleTagCollection clone() {
        return new SimpleTagCollection(tags.keySet());
    }

    public void put(String key, String tag) {
        if (key == null) {
            throw new NullPointerException("'key' is null");
        }
        if (tag == null) {
            throw new NullPointerException("'tag' is null");
        }
        Tag t = new Tag(key, tag);
        tags.put(t, t);
    }

    public void putAll(TagCollection that) {
        for(String key: that) {
            for(String tag: that.tagsFor(key)) {
                put(key, tag);
            }
        }
    }

    public void remove(String key) {
        if (key == null) {
            throw new NullPointerException("'key' is null");
        }
        Iterator<Tag> it = tags.tailMap(new Tag(key, "")).keySet().iterator();
        while(it.hasNext()) {
            Tag t = it.next();
            if (t.tagKey.equals(key)) {
                it.remove();
            }
            else {
                break;
            }
        }
    }

    public void remove(String key, String tag) {
        if (key == null) {
            throw new NullPointerException("'key' is null");
        }
        if (tag == null) {
            throw new NullPointerException("'tag' is null");
        }
        Tag t = new Tag(key, tag);
        tags.remove(t);
    }

    public void clear() {
        tags.clear();
    }

    private static class KeyIterator implements Iterator<String> {

        private final Iterator<Tag> iterator;
        private String nextKey = null;

        public KeyIterator(Iterator<Tag> tags) {
            iterator = tags;
            seekNext();
        }

        private void seekNext() {
            while(iterator.hasNext()) {
                Tag tag = iterator.next();
                if (!tag.tagKey.equals(nextKey)) {
                    nextKey = tag.tagKey;
                    return;
                }
            }
            nextKey = null;
        }

        public boolean hasNext() {
            return nextKey != null;
        }

        @Override
        public String next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            String tagKey = nextKey;
            seekNext();
            return tagKey;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private static class TagIterator implements Iterator<String> {

        private final String key;
        private final Iterator<Tag> iterator;
        private String nextTag;

        public TagIterator(String key, Iterator<Tag> iterator) {
            this.key = key;
            this.iterator = iterator;
            seekNext();
        }

        private void seekNext() {
            while(iterator.hasNext()) {
                Tag tag = iterator.next();
                if (!key.equals(tag.tagKey)) {
                    break;
                }
                nextTag = tag.tagValue;
                return;
            }
            nextTag = null;
        }

        @Override
        public boolean hasNext() {
            return nextTag != null;
        }

        @Override
        public String next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            String tag = nextTag;
            seekNext();
            return tag;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public String toString() {
        return tags.keySet().toString();
    }

    private static class Tag implements Comparable<Tag> {

        final String tagKey;
        final String tagValue;

        public Tag(String tagKey, String tagValue) {
            this.tagKey = tagKey;
            this.tagValue = tagValue;
        }

        @Override
        public int compareTo(Tag that) {
            int n = tagKey.compareTo(that.tagKey);
            if (n == 0) {
                n = tagValue.compareTo(that.tagValue);
            }
            return n;
        }

        public String toString() {
            return tagKey + ":" + tagValue;
        }
    }
}
