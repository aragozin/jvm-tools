package org.gridkit.util.formating;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.gridkit.jvmtool.jackson.DefaultPrettyPrinter;
import org.gridkit.jvmtool.jackson.JsonGenerationException;
import org.gridkit.jvmtool.jackson.JsonGenerator;
import org.gridkit.jvmtool.util.json.JsonWriter;

public class GridExportHelper implements GridSink {

    private final List<List<Object>> data = new ArrayList<List<Object>>();
    private List<Object> row;

    @Override
    public GridExportHelper append(Object value) {
        if (row == null) {
            row = new ArrayList<Object>();
            data.add(row);
        }
        row.add(value);
        return this;
    }

    @Override
    public GridExportHelper endOfRow() {
        row = null;
        return this;
    }

    private String[] toStrings(List<Object> row) {
        String[] trow = new String[row.size()];
        for (int i = 0; i != trow.length; ++i) {
            trow[i] = row.get(i) == null ? "" : String.valueOf(row.get(i));
        }
        return trow;
    }

    public void exportAsCsv(PrintStream printer) {
        TextTable tt = new TextTable();
        for (List<Object> row: data) {
            String[] trow = toStrings(row);
            tt.addRow(trow);
        }
        try {
            tt.formatCsv(printer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void exportAsJson(PrintStream printer) {
        try {
            String[] hdr = toStrings(data.get(0));
            OutputStreamWriter osw = new OutputStreamWriter(printer, Charset.forName("utf8"));
            JsonWriter writer = new JsonWriter(osw, new GridPrettyPrinter());
            writer.writeStartArray();
            for (int i = 1; i < data.size(); ++i) {
                writer.writeStartObject();
                List<Object> row = data.get(i);
                for (int n = 0; n != row.size(); ++n) {
                    Object val = row.get(n);
                    if (val != null) {
                        writer.writeFieldName(hdr[n]);
                        writer.writeString(String.valueOf(val));
                    }
                }
                writer.writeEndObject();
            }
            writer.writeEndArray();
            writer.flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final class GridPrettyPrinter extends DefaultPrettyPrinter {

        public GridPrettyPrinter() {
            _arrayIndenter = new NopIndenter();
            _nesting = 100;
            _objectIndenter = new FixedSpaceIndenter();
            _spacesInObjectEntries = false;
        }

        @Override
        public void writeStartObject(JsonGenerator jg) throws IOException, JsonGenerationException {
            jg.writeRaw('\n');
            super.writeStartObject(jg);
        }

        @Override
        public void writeEndArray(JsonGenerator jg, int nrOfValues) throws IOException, JsonGenerationException {
            jg.writeRaw('\n');
            super.writeEndArray(jg, nrOfValues);
        }
    }
}
