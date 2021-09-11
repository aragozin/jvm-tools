package org.gridkit.benchmark.gc;

import java.io.IOException;

import org.gridkit.benchmark.gc.YoungGCPauseBenchmark.TestResult;
import org.junit.Assert;
import org.junit.Test;

public class YoungGcStarter {

    @Test
    public void go() throws IOException {
        YoungGCPauseBenchmark bench = new YoungGCPauseBenchmark();
        bench.headRoom = 64;
        bench.printEvents = true;
        TestResult result = bench.benchmark();
        Assert.assertTrue(result != null);
        System.out.println(result);
    }

}
