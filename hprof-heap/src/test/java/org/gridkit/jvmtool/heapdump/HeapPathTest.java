package org.gridkit.jvmtool.heapdump;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.Test;

public class HeapPathTest {

    @Test
    public void parse_path() {

        String text;
        text = Arrays.toString(HeapPathWalker.parsePath("inputsByName.table[*].value"));
        assertThat(text).isEqualTo("[inputsByName, table, [*], value]");

        text = Arrays.toString(HeapPathWalker.parsePath("inputsByName.table[*].value(**.String)"));
        assertThat(text).isEqualTo("[inputsByName, table, [*], value, (**.String)]");

    }

}
