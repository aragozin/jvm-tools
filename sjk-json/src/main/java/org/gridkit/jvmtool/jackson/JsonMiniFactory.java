package org.gridkit.jvmtool.jackson;

import java.io.Writer;

import org.gridkit.jvmtool.jackson.JsonGenerator.Feature;

public class JsonMiniFactory {

    public static JsonGenerator createJsonGenerator(Writer writer) {
        WriterBasedGenerator gen = new WriterBasedGenerator(Feature.collectDefaults(), writer);
        return gen;
    }

}
