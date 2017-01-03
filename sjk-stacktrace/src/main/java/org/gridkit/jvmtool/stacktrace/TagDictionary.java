package org.gridkit.jvmtool.stacktrace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.gridkit.jvmtool.event.SimpleTagCollection;
import org.gridkit.jvmtool.event.TagCollection;

public class TagDictionary {

    private static final TagSet EMPTY = new TagSet(new SimpleTagCollection());

    private Map<TagSet, Integer> tagSetDic = new LinkedHashMap<TagSet, Integer>();

    private int limit;

    public TagDictionary(int limit) {
        this.limit = limit;
        tagSetDic.put(EMPTY, 0); // empty entry
    }

    public int intern(TagCollection tags, TagSetEncoder encoder) {
        TagSet ts = new TagSet(tags);
        Integer id = tagSetDic.remove(ts);
        if (id != null) {
            tagSetDic.put(ts, id); // TODO garbage hotspot
            return id;
        }
        else if (tagSetDic.size() < limit) {
            id = tagSetDic.size();
            encodeTag(id, ts, encoder);
            return id;
        }
        else {
            id = evict();
            encodeTag(id, ts, encoder);
            return id;
        }
    }

    protected int evict() {
        Integer id;
        Iterator<Integer> it = tagSetDic.values().iterator();
        id = it.next();
        if (id == 0) {
            // never evict empty tag set
            id = it.next();
        }
        it.remove();
        return id;
    }

    private void encodeTag(int id, TagSet ts, TagSetEncoder encoder) {
        int baseRef = 0;
        TagSet baseSet = EMPTY;
        int minDistance = distance(EMPTY, ts, encoder);

        for(Entry<TagSet, Integer> e: tagSetDic.entrySet()) {
            int d = distance(e.getKey(), ts, encoder);
            if (d < minDistance) {
                baseSet = e.getKey();
                baseRef = e.getValue();
                minDistance = d;
            }
        }

        touch(baseSet, baseRef);
        encoder.startTagSet(id, baseRef);
        encode(baseSet, ts, encoder);
        encoder.finishTag();

        tagSetDic.put(ts, id);
    }

    private void touch(TagSet baseSet, int baseRef) {
        if (baseRef != 0) {
            tagSetDic.remove(baseSet);
            tagSetDic.put(baseSet, baseRef);
        }
    }

    private int distance(TagSet base, TagSet set, TagSetEncoder encoder) {
        int cost = 0;
        int nb = 0;
        int ns = 0;
        while(nb < base.tags.length && ns < set.tags.length) {
            int c = base.tags[nb].compareTo(set.tags[ns]);
            if (c == 0) {
                ++nb;
                ++ns;
            }
            else if (c < 0) {
                cost += encoder.cost(base.tags[nb].key, base.tags[nb].tag);
                ++nb;
            }
            else {
                cost += encoder.cost(set.tags[ns].key, set.tags[ns].tag);
                ++ns;
            }
        }
        while(nb < base.tags.length) {
            cost += encoder.cost(base.tags[nb].key, base.tags[nb].tag);
            ++nb;
        }
        while(ns < set.tags.length) {
            cost += encoder.cost(set.tags[ns].key, set.tags[ns].tag);
            ++ns;
        }

        return cost;
    }

    private void encode(TagSet base, TagSet set, TagSetEncoder encoder) {
        int nb = 0;
        int ns = 0;
        while(nb < base.tags.length && ns < set.tags.length) {
            int c = base.tags[nb].compareTo(set.tags[ns]);
            if (c == 0) {
                ++nb;
                ++ns;
            }
            else if (c < 0) {
                encodeRemoveTag(base, encoder, nb);
                ++nb;
            }
            else {
                encoder.append(set.tags[ns].key, set.tags[ns].tag);
                ++ns;
            }
        }
        while(nb < base.tags.length) {
            encodeRemoveTag(base, encoder, nb);
            ++nb;
        }
        while(ns < set.tags.length) {
            encoder.append(set.tags[ns].key, set.tags[ns].tag);
            ++ns;
        }
    }

    protected void encodeRemoveTag(TagSet base, TagSetEncoder encoder, int nb) {
        boolean multikey = false;
        if (nb > 0 && base.tags[nb - 1].key.equals(base.tags[nb].key)) {
            multikey = true;
        }
        if (nb + 1 < base.tags.length && base.tags[nb + 1].key.equals(base.tags[nb].key)) {
            multikey = true;
        }
        if (multikey) {
            encoder.remove(base.tags[nb].key, base.tags[nb].tag);
        }
        else {
            // short remove
            encoder.remove(base.tags[nb].key);
        }
    }

//    private int cost(Tag tag) {
//        return 2 + tag.key.length() + tag.tag.length();
//    }
//
    public interface TagSetEncoder {

        public void startTagSet(int setId, int baseId);

        public void append(String key, String tag);

        public void remove(String key);

        public void remove(String key, String tag);

        public int cost(String key, String tag);

        public void finishTag();

    }

    private static class TagSet {

        final Tag[] tags;
        final int hash;

        public TagSet(TagCollection col) {
            List<Tag> buf = new ArrayList<TagDictionary.Tag>();
            for(String key: col) {
                for(String tag: col.tagsFor(key)) {
                    buf.add(new Tag(key, tag));
                }
            }
            tags = buf.toArray(new Tag[buf.size()]);
            Arrays.sort(tags);

            hash = Arrays.hashCode(tags);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            TagSet other = (TagSet) obj;
            if (hash != other.hash)
                return false;
            if (!Arrays.equals(tags, other.tags))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return Arrays.toString(tags);
        }
    }

    private static class Tag implements Comparable<Tag> {

        final String key;
        final String tag;

        public Tag(String key, String tag) {
            this.key = key;
            this.tag = tag;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((key == null) ? 0 : key.hashCode());
            result = prime * result + ((tag == null) ? 0 : tag.hashCode());
            return result;
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Tag other = (Tag) obj;
            if (key == null) {
                if (other.key != null)
                    return false;
            } else if (!key.equals(other.key))
                return false;
            if (tag == null) {
                if (other.tag != null)
                    return false;
            } else if (!tag.equals(other.tag))
                return false;
            return true;
        }

        @Override
        public int compareTo(Tag o) {
            int n = key.compareTo(o.key);
            if (n == 0) {
                n = tag.compareTo(o.tag);
            }
            return n;
        }

        @Override
        public String toString() {
            return key + ":" + tag;
        }
    }
}
