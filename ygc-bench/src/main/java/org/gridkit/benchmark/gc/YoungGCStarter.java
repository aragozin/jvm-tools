package org.gridkit.benchmark.gc;

import java.io.IOException;

import com.beust.jcommander.JCommander;

public class YoungGCStarter {

    public static void main(String[] args) throws IOException {
        JCommander jcmd = new JCommander();
        YoungGCPauseBenchmark bench = new YoungGCPauseBenchmark();
        jcmd.addCommand("ygc", bench);
        jcmd.parse(args);
        String cmd = jcmd.getParsedCommand();
        if (cmd != null) {
            bench.benchmark();
        }
        else {
            jcmd.usage();
        }
    }
}
