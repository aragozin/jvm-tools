package org.gridkit.jvmtool.event;

import static org.gridkit.jvmtool.event.ExcludeIterator.exclude;
import static org.gridkit.jvmtool.event.MergeIterator.merge;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class MergeExcludeIteratorTest {

    Iterable<String> NO_STR = seq(new String[0]);
    Iterable<Integer> NO_INT = seq(new Integer[0]);

    @SuppressWarnings({ "unchecked", "rawtypes" })
    Comparator<String> SCMP = (Comparator)new ReverseComparator();
    @SuppressWarnings({ "unchecked", "rawtypes" })
    Comparator<Integer> ICMP = (Comparator)new ReverseComparator();

    public <T> Iterable<T> seq(T... values) {
        return Arrays.asList(values);
    }

    @SuppressWarnings("unchecked")
    public <T> Iterable<T> empty(Class<T> c) {
        return Collections.EMPTY_LIST;
    }

    @Test
    public void simple_string_merge() {

        Assertions.assertThat(merge(seq("A", "B", "C"), NO_STR)).containsExactly("A", "B", "C");
        Assertions.assertThat(merge(NO_STR, seq("A", "B", "C"))).containsExactly("A", "B", "C");
        Assertions.assertThat(merge(NO_STR, NO_STR)).containsExactly();
        Assertions.assertThat(merge(seq("A"), seq("B"))).containsExactly("A", "B");
        Assertions.assertThat(merge(seq("B"), seq("A"))).containsExactly("A", "B");
        Assertions.assertThat(merge(seq("A", "B"), seq("A"))).containsExactly("A", "B");
        Assertions.assertThat(merge(seq("A", "B"), seq("A", "B"))).containsExactly("A", "B");
        Assertions.assertThat(merge(seq("B"), seq("A", "B"))).containsExactly("A", "B");
        Assertions.assertThat(merge(seq("A", "B"), seq("B"))).containsExactly("A", "B");
        Assertions.assertThat(merge(seq("A"), seq("B", "C"))).containsExactly("A", "B", "C");
        Assertions.assertThat(merge(seq("B"), seq("A", "C"))).containsExactly("A", "B", "C");
        Assertions.assertThat(merge(seq("A", "B"), seq("B", "C"))).containsExactly("A", "B", "C");
        Assertions.assertThat(merge(seq("A", "B", "C"), seq("B", "C"))).containsExactly("A", "B", "C");
        Assertions.assertThat(merge(seq("A", "B", "C"), seq("A", "B", "C"))).containsExactly("A", "B", "C");

    }

    @Test
    public void simple_int_merge() {

        Assertions.assertThat(merge(seq(1, 2, 3), NO_INT)).containsExactly(1, 2, 3);
        Assertions.assertThat(merge(NO_INT, seq(1, 2, 3))).containsExactly(1, 2, 3);
        Assertions.assertThat(merge(NO_INT, NO_INT)).containsExactly();
        Assertions.assertThat(merge(seq(1), seq(2))).containsExactly(1, 2);
        Assertions.assertThat(merge(seq(2), seq(1))).containsExactly(1, 2);
        Assertions.assertThat(merge(seq(1, 2), seq(1))).containsExactly(1, 2);
        Assertions.assertThat(merge(seq(1, 2), seq(1, 2))).containsExactly(1, 2);
        Assertions.assertThat(merge(seq(2), seq(1, 2))).containsExactly(1, 2);
        Assertions.assertThat(merge(seq(1, 2), seq(2))).containsExactly(1, 2);
        Assertions.assertThat(merge(seq(1), seq(2, 3))).containsExactly(1, 2, 3);
        Assertions.assertThat(merge(seq(1, 2), seq(2, 3))).containsExactly(1, 2, 3);
        Assertions.assertThat(merge(seq(1, 2, 3), seq(2, 3))).containsExactly(1, 2, 3);
        Assertions.assertThat(merge(seq(1, 2, 3), seq(1, 2, 3))).containsExactly(1, 2, 3);

    }

    @Test
    public void simple_int_merge_with_comparator() {

        Assertions.assertThat(merge(seq(3, 2, 1), NO_INT, ICMP)).containsExactly(3, 2, 1);
        Assertions.assertThat(merge(NO_INT, seq(3, 2, 1), ICMP)).containsExactly(3, 2, 1);
        Assertions.assertThat(merge(NO_INT, NO_INT, ICMP)).containsExactly();
        Assertions.assertThat(merge(seq(1), seq(2), ICMP)).containsExactly(2, 1);
        Assertions.assertThat(merge(seq(2), seq(1), ICMP)).containsExactly(2, 1);
        Assertions.assertThat(merge(seq(2, 1), seq(1), ICMP)).containsExactly(2, 1);
        Assertions.assertThat(merge(seq(2, 1), seq(2, 1), ICMP)).containsExactly(2, 1);
        Assertions.assertThat(merge(seq(2), seq(2, 1), ICMP)).containsExactly(2, 1);
        Assertions.assertThat(merge(seq(2, 1), seq(2), ICMP)).containsExactly(2, 1);
        Assertions.assertThat(merge(seq(1), seq(3, 2), ICMP)).containsExactly(3, 2, 1);
        Assertions.assertThat(merge(seq(2, 1), seq(3, 2), ICMP)).containsExactly(3, 2, 1);
        Assertions.assertThat(merge(seq(3, 2, 1), seq(3, 2), ICMP)).containsExactly(3, 2, 1);
        Assertions.assertThat(merge(seq(3, 2, 1), seq(3, 2, 1), ICMP)).containsExactly(3, 2, 1);

    }

    @Test
    public void simple_string_merge_with_comparator() {

        Assertions.assertThat(merge(seq("C", "B", "A"), NO_STR, SCMP)).containsExactly("C", "B", "A");
        Assertions.assertThat(merge(NO_STR, seq("C", "B", "A"), SCMP)).containsExactly("C", "B", "A");
        Assertions.assertThat(merge(NO_STR, NO_STR, SCMP)).containsExactly();
        Assertions.assertThat(merge(seq("A"), seq("B"), SCMP)).containsExactly("B", "A");
        Assertions.assertThat(merge(seq("B"), seq("A"), SCMP)).containsExactly("B", "A");
        Assertions.assertThat(merge(seq("B", "A"), seq("A"), SCMP)).containsExactly("B", "A");
        Assertions.assertThat(merge(seq("B", "A"), seq("B", "A"), SCMP)).containsExactly("B", "A");
        Assertions.assertThat(merge(seq("B"), seq("B", "A"), SCMP)).containsExactly("B", "A");
        Assertions.assertThat(merge(seq("B", "A"), seq("B"), SCMP)).containsExactly("B", "A");
        Assertions.assertThat(merge(seq("A"), seq("C", "B"), SCMP)).containsExactly("C", "B", "A");
        Assertions.assertThat(merge(seq("B", "A"), seq("C", "B"), SCMP)).containsExactly("C", "B", "A");
        Assertions.assertThat(merge(seq("C", "B", "A"), seq("C", "B"), SCMP)).containsExactly("C", "B", "A");
        Assertions.assertThat(merge(seq("C", "B", "A"), seq("C", "B", "A"), SCMP)).containsExactly("C", "B", "A");

    }

    @Test
    public void simple_string_exclude() {

        Assertions.assertThat(exclude(seq("A", "B", "C"), NO_STR)).containsExactly("A", "B", "C");
        Assertions.assertThat(exclude(seq("A", "B", "C"), seq("D"))).containsExactly("A", "B", "C");
        Assertions.assertThat(exclude(NO_STR, seq("A", "B", "C"))).containsExactly();
        Assertions.assertThat(exclude(NO_STR, NO_STR)).containsExactly();
        Assertions.assertThat(exclude(seq("A"), seq("B"))).containsExactly("A");
        Assertions.assertThat(exclude(seq("B"), seq("A"))).containsExactly("B");
        Assertions.assertThat(exclude(seq("A", "B"), seq("A"))).containsExactly("B");
        Assertions.assertThat(exclude(seq("A", "B"), seq("A", "B"))).containsExactly();
        Assertions.assertThat(exclude(seq("B"), seq("A", "B"))).containsExactly();
        Assertions.assertThat(exclude(seq("A", "B"), seq("B"))).containsExactly("A");
        Assertions.assertThat(exclude(seq("A"), seq("B", "C"))).containsExactly("A");
        Assertions.assertThat(exclude(seq("B"), seq("A", "C"))).containsExactly("B");
        Assertions.assertThat(exclude(seq("A", "B"), seq("B", "C"))).containsExactly("A");
        Assertions.assertThat(exclude(seq("A", "B", "C"), seq("B", "C"))).containsExactly("A");
        Assertions.assertThat(exclude(seq("A", "B", "C"), seq("A"))).containsExactly("B", "C");
        Assertions.assertThat(exclude(seq("A", "B", "C"), seq("B"))).containsExactly("A", "C");
        Assertions.assertThat(exclude(seq("A", "B", "C"), seq("C"))).containsExactly("A", "B");
        Assertions.assertThat(exclude(seq("A", "B", "C"), seq("A", "C"))).containsExactly("B");

    }

    @Test
    public void simple_int_exclude() {

        Assertions.assertThat(exclude(seq(1, 2, 3), NO_INT)).containsExactly(1, 2, 3);
        Assertions.assertThat(exclude(seq(1, 2, 3), seq(4))).containsExactly(1, 2, 3);
        Assertions.assertThat(exclude(NO_INT, seq(1, 2, 3))).containsExactly();
        Assertions.assertThat(exclude(NO_INT, NO_INT)).containsExactly();
        Assertions.assertThat(exclude(seq(1), seq(2))).containsExactly(1);
        Assertions.assertThat(exclude(seq(2), seq(1))).containsExactly(2);
        Assertions.assertThat(exclude(seq(1, 2), seq(1))).containsExactly(2);
        Assertions.assertThat(exclude(seq(1, 2), seq(1, 2))).containsExactly();
        Assertions.assertThat(exclude(seq(2), seq(1, 2))).containsExactly();
        Assertions.assertThat(exclude(seq(1, 2), seq(2))).containsExactly(1);
        Assertions.assertThat(exclude(seq(1), seq(2, 3))).containsExactly(1);
        Assertions.assertThat(exclude(seq(2), seq(1, 3))).containsExactly(2);
        Assertions.assertThat(exclude(seq(1, 2), seq(2, 3))).containsExactly(1);
        Assertions.assertThat(exclude(seq(1, 2, 3), seq(2, 3))).containsExactly(1);
        Assertions.assertThat(exclude(seq(1, 2, 3), seq(1))).containsExactly(2, 3);
        Assertions.assertThat(exclude(seq(1, 2, 3), seq(2))).containsExactly(1, 3);
        Assertions.assertThat(exclude(seq(1, 2, 3), seq(3))).containsExactly(1, 2);
        Assertions.assertThat(exclude(seq(1, 2, 3), seq(1, 3))).containsExactly(2);

    }

    @Test
    public void simple_string_exclude_with_comparator() {

        Assertions.assertThat(exclude(seq("C", "B", "A"), NO_STR, SCMP)).containsExactly("C", "B", "A");
        Assertions.assertThat(exclude(seq("C", "B", "A"), seq("D"), SCMP)).containsExactly("C", "B", "A");
        Assertions.assertThat(exclude(NO_STR, seq("C", "B", "A"), SCMP)).containsExactly();
        Assertions.assertThat(exclude(NO_STR, NO_STR, SCMP)).containsExactly();
        Assertions.assertThat(exclude(seq("A"), seq("B"), SCMP)).containsExactly("A");
        Assertions.assertThat(exclude(seq("B"), seq("A"), SCMP)).containsExactly("B");
        Assertions.assertThat(exclude(seq("B", "A"), seq("A"), SCMP)).containsExactly("B");
        Assertions.assertThat(exclude(seq("B", "A"), seq("B", "A"), SCMP)).containsExactly();
        Assertions.assertThat(exclude(seq("B"), seq("B", "A"), SCMP)).containsExactly();
        Assertions.assertThat(exclude(seq("B", "A"), seq("B"), SCMP)).containsExactly("A");
        Assertions.assertThat(exclude(seq("A"), seq("C", "B"), SCMP)).containsExactly("A");
        Assertions.assertThat(exclude(seq("B"), seq("C", "A"), SCMP)).containsExactly("B");
        Assertions.assertThat(exclude(seq("B", "A"), seq("C", "B"), SCMP)).containsExactly("A");
        Assertions.assertThat(exclude(seq("C", "B", "A"), seq("C", "B"), SCMP)).containsExactly("A");
        Assertions.assertThat(exclude(seq("C", "B", "A"), seq("A"), SCMP)).containsExactly("C", "B");
        Assertions.assertThat(exclude(seq("C", "B", "A"), seq("B"), SCMP)).containsExactly("C", "A");
        Assertions.assertThat(exclude(seq("C", "B", "A"), seq("C"), SCMP)).containsExactly("B", "A");
        Assertions.assertThat(exclude(seq("C", "B", "A"), seq("C", "A"), SCMP)).containsExactly("B");

    }

    public static class ReverseComparator implements Comparator<Comparable<Object>> {

        @Override
        public int compare(Comparable<Object> o1, Comparable<Object> o2) {
            return o2.compareTo(o1);
        }
    }
}
