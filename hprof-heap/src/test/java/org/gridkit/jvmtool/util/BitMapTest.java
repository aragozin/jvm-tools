package org.gridkit.jvmtool.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;

public class BitMapTest {

    @Test
    public void simple_test() {
        PagedBitMap pbm = new PagedBitMap();
        int len = 4 << 20;
        for(int i = 0; i != len; ++i) {
            pbm.set(3 * i, true);
        }

        for(int i = 0; i != len; ++i) {
            Assert.assertTrue(pbm.get(3 * i));
            Assert.assertFalse(pbm.get(3 * i + 1));
            Assert.assertFalse(pbm.get(3 * i + 2));
        }

        for(int i = len; i != 2 * len; ++i) {
            Assert.assertFalse(pbm.get(3 * i));
            Assert.assertFalse(pbm.get(3 * i + 1));
            Assert.assertFalse(pbm.get(3 * i + 2));
        }

        long n = 0;
        while(true) {
            if (n == 31) {
                new String();
            }
            long m = pbm.seekNext(n);
            if (m < 0) {
                Assert.assertTrue(n == 3 * len - 2);
                break;
            }
            if (m == n) {
                Assert.assertTrue(n == 0);
            }
            else {
                Assert.assertTrue(m == n + 2);
            }
            n = m + 1;
        }

        for(int i = 0; i != len; ++i) {
            pbm.set(1000l * len + 3 * i, true);
        }

        for(int i = 0; i != len; ++i) {
            Assert.assertTrue(pbm.get(1000l * len + 3 * i));
            Assert.assertFalse(pbm.get(1000l * len + 3 * i + 1));
            Assert.assertFalse(pbm.get(1000l * len + 3 * i + 2));
        }
    }

    @Test
    public void delete_test() {
        PagedBitMap pbm = new PagedBitMap();
        pbm.set(20, true);
        pbm.set(40, true);
        pbm.set(60, true);
        pbm.set(80, true);
        pbm.set(100, true);
        assertThat(pbm.get(20)).isTrue();
        assertThat(pbm.get(40)).isTrue();
        assertThat(pbm.get(60)).isTrue();
        assertThat(pbm.get(80)).isTrue();
        assertThat(pbm.get(100)).isTrue();
        pbm.set(40, false);
        assertThat(pbm.get(20)).isTrue();
        assertThat(pbm.get(40)).isFalse();
        assertThat(pbm.get(60)).isTrue();
        assertThat(pbm.get(80)).isTrue();
        assertThat(pbm.get(100)).isTrue();

        assertThat(pbm.seekNext(0)).isEqualTo(20);
        assertThat(pbm.seekNext(21)).isEqualTo(60);
        assertThat(pbm.seekNext(41)).isEqualTo(60);
        assertThat(pbm.seekNext(61)).isEqualTo(80);
        assertThat(pbm.seekNext(81)).isEqualTo(100);
        assertThat(pbm.seekNext(101)).isEqualTo(-1);

        assertThat(pbm.getAndSet(60, false)).isTrue();
        assertThat(pbm.getAndSet(40, true)).isFalse();
        assertThat(pbm.get(40)).isTrue();
        assertThat(pbm.get(60)).isFalse();
    }

}
