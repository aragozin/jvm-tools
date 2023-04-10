package org.gridkit.jvmtool.stacktrace.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;

import org.gridkit.jvmtool.stacktrace.StackTraceCodec;
import org.gridkit.jvmtool.stacktrace.StackTraceWriter;
import org.gridkit.jvmtool.stacktrace.ThreadSnapshot;
import org.gridkit.jvmtool.stacktrace.util.LinuxPerfImporter.PerfTrace;
import org.junit.Test;

public class LinuxPerfImporterTest {

    @Test
    public void smokeParse() throws FileNotFoundException {

        Reader reader = new FileReader(new File("src/test/resources/perf3.txt"));

        int n = 0;
        Iterator<PerfTrace> it = LinuxPerfImporter.parse(reader);
        while (it.hasNext()) {
            it.next();
            ++n;
        }

        System.out.println("Read " + n + " traces");
    }

    @Test
    public void smokeConvert() throws FileNotFoundException {

        Reader reader = new FileReader(new File("src/test/resources/perf3.txt"));

        int n = 0;
        Iterator<ThreadSnapshot> it = LinuxPerfImporter.parseAndConvert(reader, "cycles", 2, 0);
        while (it.hasNext()) {
            it.next();
            ++n;
        }

        System.out.println("Produced " + n + " synthetic traces");
    }

    @Test
    public void smokeExport() throws IOException {

        Reader reader = new FileReader(new File("src/test/resources/perf3.txt"));

        int n = 0;
        Iterator<ThreadSnapshot> it = LinuxPerfImporter.parseAndConvert(reader, "cycles", 3, 0);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        StackTraceWriter writer = StackTraceCodec.newWriter(bos);

        while (it.hasNext()) {
            ThreadSnapshot pt = it.next();
            writer.write(pt);
            ++n;
        }
        writer.close();

        System.out.println("Produced " + n + " synthetic traces, serialized into " + bos.size() + " bytes");
    }

    @Test
    public void smokeExport2() throws IOException {

        Reader reader = new FileReader(new File("src/test/resources/perf2.txt"));

        int n = 0;
        Iterator<ThreadSnapshot> it = LinuxPerfImporter.parseAndConvert(reader, null, 3, 0);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        StackTraceWriter writer = StackTraceCodec.newWriter(bos);

        while (it.hasNext()) {
            ThreadSnapshot pt = it.next();
            writer.write(pt);
            ++n;
        }
        writer.close();

        System.out.println("Produced " + n + " synthetic traces, serialized into " + bos.size() + " bytes");
    }

    @Test
    public void frameParseTest1() {
        System.out.println(LinuxPerfImporter.convertFrame(LinuxPerfImporter.parseFrame("\t ffffffff9c076a3a [unknown] ([kernel.kallsyms])")));
    }

    @Test
    public void frameParseTest2() {
        System.out.println(LinuxPerfImporter.convertFrame(LinuxPerfImporter.parseFrame("\t     7f4650e966d6 do_futex_wait.constprop.1+0x36 (/lib/x86_64-linux-gnu/libpthread-2.27.so)")));
    }

    @Test
    public void frameParseTest3() {
        System.out.println(LinuxPerfImporter.convertFrame(LinuxPerfImporter.parseFrame("\t                0 [unknown] ([unknown])")));
    }

    @Test
    public void frameParseTest4() {
        System.out.println(LinuxPerfImporter.convertFrame(LinuxPerfImporter.parseFrame("\t     7f464fe51601 Mutex::lock_without_safepoint_check+0x21 (/home/jdk-17/lib/server/libjvm.so)")));
    }

    @Test
    public void frameParseTest5() {
        System.out.println(LinuxPerfImporter.convertFrame(LinuxPerfImporter.parseFrame("\t     7f464fea3671 thread_native_entry+0xe1 (/home/jdk-17/lib/server/libjvm.so)")));
    }

    @Test
    public void frameParseTest6() {
        System.out.println(LinuxPerfImporter.convertFrame(LinuxPerfImporter.parseFrame("\t     7f4650e8d6db start_thread+0xdb (/lib/x86_64-linux-gnu/libpthread-2.27.so)")));
    }

    @Test
    public void frameParseTest7() {
        System.out.println(LinuxPerfImporter.convertFrame(LinuxPerfImporter.parseFrame("\t     7f46501edc2f VMThread::run+0xbf (/home/jdk-17/lib/server/libjvm.so)")));
    }

    @Test
    public void frameParseTest8() {
        System.out.println(LinuxPerfImporter.convertFrame(LinuxPerfImporter.parseFrame("\t     7f4638f98fc3 java.lang.String CryptoBench.crypt(java.lang.String)+0x283 (/tmp/perf-16839.map)")));
    }

    @Test
    public void frameParseTest9() {
        System.out.println(LinuxPerfImporter.convertFrame(LinuxPerfImporter.parseFrame("\t     7f46314be23e Interpreter+0x6be (/tmp/perf-16839.map)")));
    }

    @Test
    public void frameParseTest10() {
        System.out.println(LinuxPerfImporter.convertFrame(LinuxPerfImporter.parseFrame("\t     7f46314b5cc9 StubRoutines (1)+0xc9 (/tmp/perf-16839.map)")));
    }

    @Test
    public void frameParseTest11() {
        System.out.println(LinuxPerfImporter.convertFrame(LinuxPerfImporter.parseFrame("\t     7f46510ad579 ThreadJavaMain+0x9 (/home/jdk-17/lib/libjli.so)")));
    }

    @Test
    public void frameParseTest12() {
        System.out.println(LinuxPerfImporter.convertFrame(LinuxPerfImporter.parseFrame("\t     7f46315b1faa _new_array_Java+0x2a (/tmp/perf-16839.map)")));
    }

    @Test
    public void frameParseTest13() {
        System.out.println(LinuxPerfImporter.convertFrame(LinuxPerfImporter.parseFrame("\t     7f465021552e [unknown] (/home/jdk-17/lib/server/libjvm.so)")));
    }

}
