package org.gridkit.jvmtool.stacktrace.codec.json;

import java.io.IOException;
import java.lang.Thread.State;

import org.assertj.core.api.Assertions;
import org.gridkit.jvmtool.codec.stacktrace.ThreadSnapshotEvent;
import org.gridkit.jvmtool.event.CommonEvent;
import org.gridkit.jvmtool.spi.parsers.JsonEventSource;
import org.gridkit.jvmtool.stacktrace.StackFrame;
import org.gridkit.jvmtool.util.json.JsonStreamWriter;
import org.junit.Test;

public class JfrEventParserTest {

    @Test
    public void verify_parsing_ExecutionSample() throws IOException {

        JfrEventParser parser = new JfrEventParser();

        CommonEvent event = parser.parseNextEvent(new JsonEventSource() {

            boolean hasNext = true;

            @Override
            public boolean readNext(JsonStreamWriter writer) throws IOException {

                jostart(writer);
                    jput(writer, "eventType", "jdk.ExecutionSample");
                    jput(writer, "startTime", 1561542999864014126l);
                    jostart(writer, "sampledThread");
                        jput(writer, "osThreadId", 20152);
                        jput(writer, "javaName", "main");
                        jput(writer, "javaThreadId", 1);
                        jostart(writer, "group");
                            jput(writer, "parent", null);
                            jput(writer, "name", "main");
                        joend(writer);
                    joend(writer);
                    jostart(writer, "stackTrace");
                        jput(writer, "truncated", true);
                        jastart(writer, "frames");
                            jframe(writer,
                                    "org.eclipse.jdt.internal.ui.javaeditor.breadcrumb.BreadcrumbViewer",
                                    "getDropDownShell",
                                    239, 42, "JIT compiled"
                            );
                            jframe(writer,
                                    "org.eclipse.jdt.internal.ui.javaeditor.breadcrumb.EditorBreadcrumb",
                                    "isBreadcrumbEvent",
                                    472, 27, "JIT compiled"
                                    );
                        jaend(writer);
                    joend(writer);
                    jput(writer, "state", "STATE_RUNNABLE");
                joend(writer);

                return hasNext | (hasNext = false);
            }
        });

        ThreadSnapshotEvent tevent = (ThreadSnapshotEvent) event;

        Assertions.assertThat(tevent.threadName()).isEqualTo("main");
        Assertions.assertThat(tevent.threadId()).isEqualTo(1l);
        Assertions.assertThat(tevent.threadState()).isEqualTo(State.RUNNABLE);
        Assertions.assertThat(tevent.tags().firstTagFor("jfr.typeId")).isEqualTo("jdk.ExecutionSample");

        Assertions.assertThat(tevent.stackTrace().depth()).isEqualTo(2);
        Assertions.assertThat(tevent.stackTrace().frameAt(0))
            .isEqualTo(new StackFrame("", "org.eclipse.jdt.internal.ui.javaeditor.breadcrumb.BreadcrumbViewer", "getDropDownShell", "java", 239));
        Assertions.assertThat(tevent.stackTrace().frameAt(1))
            .isEqualTo(new StackFrame("", "org.eclipse.jdt.internal.ui.javaeditor.breadcrumb.EditorBreadcrumb", "isBreadcrumbEvent", "java", 472));
    }

    @Test
    public void verify_parsing_NativeMethodSample() throws IOException {

        JfrEventParser parser = new JfrEventParser();

        CommonEvent event = parser.parseNextEvent(new JsonEventSource() {

            boolean hasNext = true;

            @Override
            public boolean readNext(JsonStreamWriter writer) throws IOException {

                jostart(writer);
                    jput(writer, "eventType", "jdk.NativeMethodSample");
                    jput(writer, "startTime", 1561544945483325579l);
                    jostart(writer, "sampledThread");
                        jput(writer, "osThreadId", 14064);
                        jput(writer, "javaName", "(JDP Packet Listener)");
                        jput(writer, "javaThreadId", 37);
                        jostart(writer, "group");
                            jput(writer, "parent", null);
                            jput(writer, "name", "JDP Client");
                        joend(writer);
                    joend(writer);
                    jostart(writer, "stackTrace");
                        jput(writer, "truncated", false);
                        jastart(writer, "frames");
                            jframe(writer,
                                    "java.net.TwoStacksPlainDatagramSocketImpl",
                                    "receive0",
                                    0, 0, "Native"
                            );
                            jframe(writer,
                                    "java.net.TwoStacksPlainDatagramSocketImpl",
                                    "receive",
                                    0, 2, "Interpreted"
                                    );
                            jframe(writer,
                                    "java.net.DatagramSocket",
                                    "receive",
                                    0, 322, "Interpreted"
                                    );
                        jaend(writer);
                    joend(writer);
                    jput(writer, "state", "STATE_RUNNABLE");
                joend(writer);

                return hasNext | (hasNext = false);
            }
        });

        ThreadSnapshotEvent tevent = (ThreadSnapshotEvent) event;

        Assertions.assertThat(tevent.threadName()).isEqualTo("(JDP Packet Listener)");
        Assertions.assertThat(tevent.threadId()).isEqualTo(37l);
        Assertions.assertThat(tevent.threadState()).isEqualTo(State.RUNNABLE);
        Assertions.assertThat(tevent.tags().firstTagFor("jfr.typeId")).isEqualTo("jdk.NativeMethodSample");

        Assertions.assertThat(tevent.stackTrace().depth()).isEqualTo(3);
        Assertions.assertThat(tevent.stackTrace().frameAt(0))
            .isEqualTo(new StackFrame("", "java.net.TwoStacksPlainDatagramSocketImpl", "receive0", "java", -2));
        Assertions.assertThat(tevent.stackTrace().frameAt(1))
            .isEqualTo(new StackFrame("", "java.net.TwoStacksPlainDatagramSocketImpl", "receive", null, -1));
        Assertions.assertThat(tevent.stackTrace().frameAt(2))
            .isEqualTo(new StackFrame("", "java.net.DatagramSocket", "receive", null, -1));
    }

    @Test
    public void verify_parsing_JavaExceptionThrow() throws IOException {

        JfrEventParser parser = new JfrEventParser();

        CommonEvent event = parser.parseNextEvent(new JsonEventSource() {

            boolean hasNext = true;

            @Override
            public boolean readNext(JsonStreamWriter writer) throws IOException {

                jostart(writer);
                    jput(writer, "eventType", "jdk.JavaExceptionThrow");
                    jput(writer, "startTime", 1561544945483325579l);
                    jostart(writer, "eventThread");
                        jput(writer, "osThreadId", 14804);
                        jput(writer, "javaName", "Local Descriptor Scanner");
                        jput(writer, "javaThreadId", 36);
                        jostart(writer, "group");
                            jput(writer, "parent", null);
                            jput(writer, "name", "main");
                        joend(writer);
                    joend(writer);
                    jostart(writer, "stackTrace");
                        jput(writer, "truncated", false);
                        jastart(writer, "frames");
                            jframe(writer,
                                    "java.lang.Throwable",
                                    "<init>",
                                    272, 35, "JIT compiled"
                            );
                            jframe(writer,
                                    "java.lang.Exception",
                                    "<init>",
                                    66, 2, "JIT compiled"
                                    );
                            jframe(writer,
                                    "java.net.URISyntaxException",
                                    "<init>",
                                    62, 2, "JIT compiled"
                                    );
                        jaend(writer);
                    joend(writer);
                    jput(writer, "message", "Malformed IPv4 address");
                    jostart(writer, "thrownClass");
                        jput(writer, "className", "java.net.URISyntaxException");
                    joend(writer);
                joend(writer);

                return hasNext | (hasNext = false);
            }
        });

        ThreadSnapshotEvent tevent = (ThreadSnapshotEvent) event;

        Assertions.assertThat(tevent.threadName()).isEqualTo("Local Descriptor Scanner");
        Assertions.assertThat(tevent.threadId()).isEqualTo(36l);
        Assertions.assertThat(tevent.threadState()).isEqualTo(State.RUNNABLE);
        Assertions.assertThat(tevent.tags().firstTagFor("jfr.typeId")).isEqualTo("jdk.JavaExceptionThrow");

        Assertions.assertThat(tevent.stackTrace().depth()).isEqualTo(3);
        Assertions.assertThat(tevent.stackTrace().frameAt(0))
            .isEqualTo(new StackFrame("", "java.lang.Throwable", "<init>", "java", 272));
        Assertions.assertThat(tevent.stackTrace().frameAt(1))
            .isEqualTo(new StackFrame("", "java.lang.Exception", "<init>", "java", 66));
        Assertions.assertThat(tevent.stackTrace().frameAt(2))
            .isEqualTo(new StackFrame("", "java.net.URISyntaxException", "<init>", "java", 62));
    }

    @Test
    public void verify_parsing_ObjectAllocationInNewTLAB() throws IOException {

        JfrEventParser parser = new JfrEventParser();

        CommonEvent event = parser.parseNextEvent(new JsonEventSource() {

            boolean hasNext = true;

            @Override
            public boolean readNext(JsonStreamWriter writer) throws IOException {

                jostart(writer);
                    jput(writer, "eventType", "jdk.ObjectAllocationInNewTLAB");
                    jput(writer, "startTime", 1561544945483325579l);
                    jostart(writer, "eventThread");
                        jput(writer, "osThreadId", 19524);
                        jput(writer, "javaName", "Worker-4: Recording My Recording");
                        jput(writer, "javaThreadId", 76);
                        jostart(writer, "group");
                            jput(writer, "parent", null);
                            jput(writer, "name", "main");
                        joend(writer);
                    joend(writer);
                    jostart(writer, "stackTrace");
                        jput(writer, "truncated", false);
                        jastart(writer, "frames");
                            jframe(writer,
                                    "java.util.Arrays",
                                    "copyOfRange",
                                    4030, 40, "JIT compiled"
                            );
                            jframe(writer,
                                    "java.lang.StringLatin1",
                                    "newString",
                                    715, 9, "JIT compiled"
                                    );
                            jframe(writer,
                                    "java.lang.StringLatin1",
                                    "trim",
                                    541, 68, "JIT compiled"
                                    );
                            jframe(writer,
                                    "java.lang.String",
                                    "trim",
                                    2644, 11, "JIT compiled"
                                    );
                        jaend(writer);
                    joend(writer);
                    jostart(writer, "objectClass");
                        jput(writer, "className", "byte[]");
                    joend(writer);
                    jput(writer, "allocationSize", 24);
                    jput(writer, "tlabSize", 15560);
                joend(writer);

                return hasNext | (hasNext = false);
            }
        });

        ThreadSnapshotEvent tevent = (ThreadSnapshotEvent) event;

        Assertions.assertThat(tevent.threadName()).isEqualTo("Worker-4: Recording My Recording");
        Assertions.assertThat(tevent.threadId()).isEqualTo(76l);
        Assertions.assertThat(tevent.threadState()).isEqualTo(State.RUNNABLE);
        Assertions.assertThat(tevent.tags().firstTagFor("jfr.typeId")).isEqualTo("jdk.ObjectAllocationInNewTLAB");

        Assertions.assertThat(tevent.stackTrace().depth()).isEqualTo(4);
        Assertions.assertThat(tevent.stackTrace().frameAt(0))
            .isEqualTo(new StackFrame("", "java.util.Arrays", "copyOfRange", "java", 4030));
        Assertions.assertThat(tevent.stackTrace().frameAt(1))
            .isEqualTo(new StackFrame("", "java.lang.StringLatin1", "newString", "java", 715));
        Assertions.assertThat(tevent.stackTrace().frameAt(2))
            .isEqualTo(new StackFrame("", "java.lang.StringLatin1", "trim", "java", 541));
        Assertions.assertThat(tevent.stackTrace().frameAt(3))
            .isEqualTo(new StackFrame("", "java.lang.String", "trim", "java", 2644));
    }

    private static void jframe(JsonStreamWriter writer, String clazz, String method, Integer lineNumber, Integer byteCodeIndex, String frameType) throws IOException {
        jostart(writer);
            jostart(writer, "method");
                jput(writer, "class", clazz);
                jput(writer, "method", method);
            joend(writer);
            if (lineNumber != null) {
                jput(writer, "lineNumber", lineNumber);
            }
            if (byteCodeIndex != null) {
                jput(writer, "bytecodeIndex", byteCodeIndex);
            }
            if (frameType != null) {
                jput(writer, "type", frameType);
            }
        joend(writer);
    }

    private static void jostart(JsonStreamWriter writer) throws IOException {
        writer.writeStartObject();
    }

    private static void jostart(JsonStreamWriter writer, String field) throws IOException {
        writer.writeFieldName(field);
        writer.writeStartObject();
    }

    private static void joend(JsonStreamWriter writer) throws IOException {
        writer.writeEndObject();
    }

    private static void jastart(JsonStreamWriter writer, String field) throws IOException {
        writer.writeFieldName(field);
        writer.writeStartArray();
    }

    private static void jaend(JsonStreamWriter writer) throws IOException {
        writer.writeEndArray();
    }

    private static void jput(JsonStreamWriter writer, Object... values) throws IOException {
        for(int i = 0; i != values.length; ++i) {
            writer.writeFieldName((String) values[i]);
            Object val = values[++i];
            if (val == null) {
                writer.writeNull();
            }
            else if (val instanceof Number) {
                writer.writeNumber(val.toString());
            }
            else if (val instanceof Boolean) {
                writer.writeBoolean(Boolean.TRUE.equals(val));
            }
            else {
                writer.writeString(val.toString());
            }
        }
    }

}
