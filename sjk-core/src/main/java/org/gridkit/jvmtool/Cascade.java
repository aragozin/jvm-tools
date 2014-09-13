package org.gridkit.jvmtool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * {@link Cascade} represent a part of document using simple identation based
 * formating.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class Cascade {

    public static void parse(Reader source, Object visitor) {
        try {
            CascadeParser parser = new CascadeParser(source);
            parser.parse(visitor);
        }
        catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void parse(String source, Object visitor) {
        try {
            CascadeParser parser = new CascadeParser(new StringReader(source));
            parser.parse(visitor);
        }
        catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    public @interface Command {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Section {
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Text {
    }

    private static class CascadeParser {

        private BufferedReader reader;
        private String unread;
        private int lineCounter;
        private List<String> block = new ArrayList<String>();

        public CascadeParser(Reader reader) {
            BufferedReader br = null;
            if (reader instanceof BufferedReader) {
                br = (BufferedReader) reader;
            } else {
                br = new BufferedReader(reader);
            }
            this.reader = br;
        }

        public void parse(Object sink) throws Exception {

            while (true) {
                fetchBlock();
                if (block.size() == 0) {
                    break;
                } else {
                    feedBlock(block, new Sink(sink));
                }
            }
        }

        private void feedBlock(List<String> block, Sink sink) throws IOException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
            Sink rest = sink.dispatch(block.remove(0));
            while(!block.isEmpty()) {
                List<String> sub = fetchSubblock(block, 4);
                if (!sub.isEmpty()) {
                    feedBlock(sub, rest);
                }
                else {
                    if (!block.isEmpty()) {
                        throw new RuntimeException("Parsing error");
                    }
                }
            }

        }

        private List<String> fetchSubblock(List<String> text, int indentation) throws IOException {
            List<String> subblock = new ArrayList<String>();

            char[] i = new char[indentation];
            Arrays.fill(i, ' ');
            String indent = new String(i);

            String header = null;
            while (!text.isEmpty()) {
                String line = text.remove(0);
                if (line.trim().startsWith("#")) {
                    // ignore comments
                    continue;
                }
                if (line.startsWith(indent)) {
                    if (header == null) {
                        throw new IOException("Indent line is not expected here. [" + line + "]");
                    } else {
                        subblock.add(line.substring(indentation));
                    }
                } else if (line.trim().length() == 0) {
                    if (header == null) {
                        // ignore
                    } else {
                        subblock.add(line);
                    }
                } else if (header != null) {
                    text.add(0, line);
                    break;
                } else {
                    header = line;
                    subblock.add(line);
                }
            }
            return subblock;
        }

        private void fetchBlock() throws IOException {
            block.clear();
            String header = null;
            while (true) {
                String line;
                if (unread != null) {
                    line = unread;
                    unread = null;
                } else {
                    line = reader.readLine();
                    ++lineCounter;
                }
                if (line == null) {
                    break;
                } else {
                    if (line.trim().startsWith("#")) {
                        // ignore comments
                        continue;
                    }
                    if (line.indexOf('\t') >= 0) {
                        throw new IOException("Line " + lineCounter + " conatins tab character which is forbiden. [" + line + "]");
                    }
                    if (line.startsWith("    ")) {
                        if (header == null) {
                            throw new IOException("Line " + lineCounter + " indent line is not expected here. [" + line + "]");
                        } else {
                            block.add(line.substring(4));
                        }
                    } else if (line.trim().length() == 0) {
                        if (header == null) {
                            // ignore
                        } else {
                            block.add(line);
                        }
                    } else if (header != null) {
                        unread = line;
                        return;
                    } else {
                        header = line;
                        block.add(line);
                    }
                }
            }
        }
    }

    private static class Sink {

        Object sink;

        public Sink(Object sink) {
            super();
            this.sink = sink;
        }

        public Sink dispatch(String line) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {

            Method text = null;
            Method section = null;

            String dispatchKey = line.trim();
            for(Method m: sink.getClass().getMethods()) {
                Command d = m.getAnnotation(Command.class);
                if (d != null) {
                    if (dispatchKey.equals(d.value())) {
                        m.setAccessible(true);
                        Object delegate = m.invoke(sink);
                        if (delegate == null) {
                            return new NoSink(sink.getClass(), m);
                        }
                        return new Sink(delegate);
                    }
                }
                if (m.getAnnotation(Text.class) != null) {
                    text = m;
                }
                if (m.getAnnotation(Section.class) != null) {
                    section = m;
                }
            }

            if (text != null && section != null) {
                throw new IllegalArgumentException("Type " + sink.getClass().getSimpleName() + " has both @Text and @Section methods");
            }

            if (text != null) {
                text.setAccessible(true);
                text.invoke(sink, line);
                return new TextSink(sink, text, "    ");
            }

            if (section != null) {
                section.setAccessible(true);
                Object delegate = section.invoke(sink, line);
                if (delegate == null) {
                    return new NoSink(sink.getClass(), section);
                }
                return new Sink(delegate);
            }

            throw new IllegalArgumentException("Cannot dispatch on " + sink.getClass().getSimpleName() + " text line [" + line + "]");
        }
    }

    private static class NoSink extends Sink {

        private Class<?> type;
        private Method m;

        public NoSink(Class<?> type, Method m) {
            super(null);
            this.type = type;
            this.m = m;
        }

        @Override
        public Sink dispatch(String line) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
            throw new IllegalArgumentException("Call " + type.getSimpleName() + "." + m.getName() + " has returned null");
        }
    }

    private static class TextSink extends Sink {

        private Method text;
        private String pref;

        public TextSink(Object sink, Method text, String pref) {
            super(sink);
            this.text = text;
            this.pref = pref;
        }

        @Override
        public Sink dispatch(String line) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
            text.invoke(sink, pref + line);
            return new TextSink(sink, text, pref + "    ");
        }
    }

}
