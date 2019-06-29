package org.gridkit.jvmtool.spi.parsers;

import java.io.IOException;

import org.gridkit.jvmtool.util.json.JsonStreamWriter;

public interface JsonEventSource {

	public boolean readNext(JsonStreamWriter writer) throws IOException;	
	
}
