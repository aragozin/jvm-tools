package org.gridkit.jvmtool.codec;

import static org.assertj.core.api.Assertions.assertThat;

import org.gridkit.jvmtool.event.SimpleTagCollection;
import org.junit.Test;

public class SimpleTagCollectionTest {

    @Test
    public void empty_collection() {

        SimpleTagCollection tags = new SimpleTagCollection();

        assertThat(tags).isEmpty();
        assertThat(tags.tagsFor("A")).isEmpty();
        assertThat(tags.contains("A", "A")).isFalse();
    }

    @Test
    public void simple_ops() {

        SimpleTagCollection tags = new SimpleTagCollection();

        tags.put("A", "1");
        tags.put("B", "1");
        tags.put("B", "2");
        tags.put("B", "3");

        assertThat(tags).containsExactly("A", "B");
        assertThat(tags.tagsFor("A")).containsExactly("1");
        assertThat(tags.tagsFor("B")).containsExactly("1", "2", "3");
    }

    @Test
    public void removal_ops() {

        SimpleTagCollection tags = new SimpleTagCollection();

        tags.put("A", "1");
        tags.put("B", "1");
        tags.put("B", "2");
        tags.put("B", "3");
        tags.put("D", "0");

        assertThat(tags).containsExactly("A", "B", "D");
        assertThat(tags.tagsFor("A")).containsExactly("1");
        assertThat(tags.tagsFor("B")).containsExactly("1", "2", "3");
        assertThat(tags.tagsFor("D")).containsExactly("0");

        tags.remove("B", "2");

        assertThat(tags).containsExactly("A", "B", "D");
        assertThat(tags.tagsFor("A")).containsExactly("1");
        assertThat(tags.tagsFor("B")).containsExactly("1", "3");
        assertThat(tags.tagsFor("D")).containsExactly("0");

        tags.remove("B");

        assertThat(tags).containsExactly("A", "D");
        assertThat(tags.tagsFor("A")).containsExactly("1");
        assertThat(tags.tagsFor("B")).isEmpty();
        assertThat(tags.tagsFor("D")).containsExactly("0");
    }

    @Test
    public void empty_tag() {

        SimpleTagCollection tags = new SimpleTagCollection();

        tags.put("A", "");

        assertThat(tags).containsExactly("A");
        assertThat(tags.tagsFor("A")).containsExactly("");
    }
}
