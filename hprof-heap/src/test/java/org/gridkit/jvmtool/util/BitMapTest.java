package org.gridkit.jvmtool.util;

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
}
