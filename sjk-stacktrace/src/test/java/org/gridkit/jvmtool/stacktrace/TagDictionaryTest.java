package org.gridkit.jvmtool.stacktrace;

import static org.assertj.core.api.Assertions.assertThat;

import org.gridkit.jvmtool.event.SimpleTagCollection;
import org.gridkit.jvmtool.event.TagCollection;
import org.junit.Test;

public class TagDictionaryTest {

    TagDictionary dic = new TagDictionary(4);
    TestEncoder encoder = new TestEncoder();

    @Test
    public void empty_tag_set() {
        int id = dic.intern(tags(), encoder);

        assertThat(id).isEqualTo(0);
        assertThat(encoder.toString()).isEqualTo("");
    }

    @Test
    public void tag_set_matching() {
        int id1 = dic.intern(tags("A", "1"), encoder);
        int id2 = dic.intern(tags("A", "1"), encoder);

        assertThat(id1).isEqualTo(id2);
    }

    @Test
    public void tag_encoding() {
        int id1 = dic.intern(tags("A", "1"), encoder);
        int id2 = dic.intern(tags("A", "2"), encoder);
        int id3 = dic.intern(tags("B", "1", "A", "1"), encoder);

        assertThat(id1).isEqualTo(1);
        assertThat(id2).isEqualTo(2);
        assertThat(id3).isEqualTo(3);
        assertThat(encoder.toString()).isEqualTo(
          "[1,0] +A:1! [2,0] +A:2! [3,1] +B:1!");
    }

    @Test
    public void tag_eviction() {
        int id1 = dic.intern(tags("A", "1"), encoder);
        int id2 = dic.intern(tags("A", "2"), encoder);
        int id3 = dic.intern(tags("B", "1", "A", "1", "A", "2"), encoder);
        int id11 = dic.intern(tags("A", "1"), encoder);
        int id4 = dic.intern(tags("B", "1", "A", "2", "C", "1"), encoder);
        int id5 = dic.intern(tags("C", "1", "B", "1"), encoder);

        assertThat(id1).isEqualTo(1);
        assertThat(id2).isEqualTo(2);
        assertThat(id3).isEqualTo(3);
        assertThat(id11).isEqualTo(1);
        assertThat(id4).isEqualTo(2); // id reused
        assertThat(id5).isEqualTo(1); // id reused, 3 was used as base so it survives
        assertThat(encoder.toString()).isEqualTo(
                "[1,0] +A:1! [2,0] +A:2! [3,1] +A:2 +B:1! [2,3] -A:1 +C:1! [1,2] -A!");
    }

    @Test
    public void tag_key_override() {
        int id1 = dic.intern(tags("A", "long", "B", "2"), encoder);
        int id2 = dic.intern(tags("A", "long", "B", "1"), encoder);
        int id3 = dic.intern(tags("A", "long", "B", "3"), encoder);

        assertThat(id1).isEqualTo(1);
        assertThat(id2).isEqualTo(2);
        assertThat(id3).isEqualTo(3);
        // reader must apply adds before removals
        assertThat(encoder.toString()).isEqualTo(
          "[1,0] +A:long +B:2! [2,1] +B:1 -B! [3,1] -B +B:3!");
    }


    public TagCollection tags(String... kv) {
        SimpleTagCollection tc = new SimpleTagCollection();
        if (kv.length % 2 != 0) {
            throw new IllegalArgumentException();
        }

        for(int i = 0; i < kv.length; i += 2) {
            tc.put(kv[i], kv[i + 1]);
        }

        return tc;
    }

    public static class TestEncoder implements TagDictionary.TagSetEncoder {

        StringBuilder sb = new StringBuilder();

        @Override
        public int cost(String key, String tag) {
            return tag == null
                    ? 2 + key.length()
                    : 3 + key.length() + tag.length();
        }

        @Override
        public void startTagSet(int setId, int baseId) {
            if (sb.length() != 0) {
                sb.append(' ');
            }
            sb.append("[" + setId + "," + baseId + "]");

        }

        @Override
        public void append(String key, String tag) {
            sb.append(" +" + key + ":" + tag);
        }

        @Override
        public void remove(String key) {
            sb.append(" -" + key);
        }

        @Override
        public void remove(String key, String tag) {
            sb.append(" -" + key + ":" + tag);
        }

        @Override
        public void finishTag() {
            sb.append("!");
        }

        public String toString() {
            return sb.toString();
        }
    }
}
