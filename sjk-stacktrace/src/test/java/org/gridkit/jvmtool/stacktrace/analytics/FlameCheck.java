package org.gridkit.jvmtool.stacktrace.analytics;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;

import org.gridkit.jvmtool.codec.stacktrace.ThreadSnapshotEventPojo;
import org.gridkit.jvmtool.stacktrace.StackTraceCodec;
import org.gridkit.jvmtool.stacktrace.StackTraceReader;
import org.gridkit.jvmtool.stacktrace.analytics.flame.FlameGraphGenerator;
import org.junit.Test;

public class FlameCheck {


    public StackTraceReader read() throws FileNotFoundException, IOException {
        String file = "src/test/resources/jboss-10k.std";
        return StackTraceCodec.newReader(new FileInputStream(new File(file)));
    }

    @Test
    public void check() throws IOException {
        FlameGraphGenerator fg = new FlameGraphGenerator();
        StackTraceReader r = read();
        if (!r.isLoaded()) {
            r.loadNext();
        }
        while(r.isLoaded()) {
            ThreadSnapshotEventPojo pojo = new ThreadSnapshotEventPojo();
            pojo.stackTrace(r.getStackTrace());
            fg.feed(pojo);
            r.loadNext();
        }

        StringWriter sw = new StringWriter();
        fg.renderSVG("Flame Graph", 1200, sw);

        FileWriter fw = new FileWriter(new File("target/flame.svg"));
        fw.append(sw.getBuffer());
        fw.close();
    }

}
