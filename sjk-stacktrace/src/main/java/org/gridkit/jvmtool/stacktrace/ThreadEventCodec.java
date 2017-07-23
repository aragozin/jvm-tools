package org.gridkit.jvmtool.stacktrace;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import org.gridkit.jvmtool.codec.stacktrace.ThreadSnapshotExpander;
import org.gridkit.jvmtool.event.Event;
import org.gridkit.jvmtool.event.EventReader;
import org.gridkit.jvmtool.event.UniversalEventWriter;

public class ThreadEventCodec {

	public static List<String> listSupportedFormats() {
		return Arrays.asList(
				"SJK Thread Dump (Magic: TRACEDUMP_1, TRACEDUMP_2)",
				"SJK Genereic Event Dump (Magic: EVENTDUMP_1)"
				);
	}
	
    public static UniversalEventWriter createEventWriter(OutputStream os) throws IOException {
        os.write(StackTraceCodec.MAGIC4);
        StackTraceEventWriterV4 writer = new StackTraceEventWriterV4(os);
        return writer;
    }

    public static EventReader<Event> createEventReader(InputStream is) throws IOException {
        byte[] magic = MagicReader.readMagic(is);
        return createEventReader(magic, is);
    }

    public static EventReader<Event> createEventReader(byte[] magic, InputStream is) throws IOException {
        if (Arrays.equals(magic, StackTraceCodec.MAGIC)) {
            StackTraceReader lreader = new StackTraceReaderV1(is);
            EventReader<Event> reader = new LegacyThreadEventReader(lreader);
            return reader;
        }
        if (Arrays.equals(magic, StackTraceCodec.MAGIC2)) {
            StackTraceReader lreader = new StackTraceReaderV2(is);
            EventReader<Event> reader = new LegacyThreadEventReader(lreader);
            return reader;
        }
        // MAGIC3 is not used
        else if (Arrays.equals(magic, StackTraceCodec.MAGIC4)) {
            EventReader<Event> reader = new StackTraceEventReaderV4(is).morph(new ThreadSnapshotExpander());
            return reader;
        }
        else {
            throw new IOException("Unknown magic '" + new String(magic) + "'");
        }
    }
 }
