package org.gridkit.jvmtool.stacktrace;

import java.io.IOException;
import java.lang.management.ManagementFactory;

import org.junit.Test;

public class ThreadSamplerCheck {

    @Test
    public void checkNoTraceNoPriming() throws IOException {

        ThreadDumpSampler sampler = new ThreadDumpSampler();
        sampler.enableThreadStackTrace(false);

        sampler.connect(ManagementFactory.getThreadMXBean());

        loop(sampler);
    }

    @Test
    public void checkTraceNoPriming() throws IOException {

        ThreadDumpSampler sampler = new ThreadDumpSampler();
        sampler.enableThreadStackTrace(true);

        sampler.connect(ManagementFactory.getThreadMXBean());

        loop(sampler);
    }

    @Test
    public void checkNoTracePriming() throws IOException {

        ThreadDumpSampler sampler = new ThreadDumpSampler();
        sampler.enableThreadStackTrace(false);

        sampler.connect(ManagementFactory.getThreadMXBean());
        sampler.prime();

        loop(sampler);
    }

    @Test
    public void checkTracePriming() throws IOException {

        ThreadDumpSampler sampler = new ThreadDumpSampler();
        sampler.enableThreadStackTrace(true);

        sampler.connect(ManagementFactory.getThreadMXBean());
        sampler.prime();

        loop(sampler);
    }

    protected void loop(ThreadDumpSampler sampler) throws IOException {
        while(true) {
            sampler.collect(new StackTraceWriter() {

                @Override
                public void write(ThreadSnapshot snap) throws IOException {
                    System.out.println(snap.threadName() + " " + ((snap.stackTrace() == null) ? "No trcae" : (snap.stackTrace().depth() + " frames")));
                }

                @Override
                public void close() {
                }
            });
        }
    }
}
