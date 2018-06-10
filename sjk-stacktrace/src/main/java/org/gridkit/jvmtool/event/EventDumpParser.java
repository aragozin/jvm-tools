package org.gridkit.jvmtool.event;

import java.io.IOException;
import java.io.InputStream;
import java.util.ServiceLoader;

/**
 * This is SPI interface for dump parsers discovered via {@link ServiceLoader}.
 *  
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public interface EventDumpParser {

	public boolean isFunctional();
	
	/**
	 * @return <code>null</code> if cannot open dump or {@link EventReader}
	 * @throws IOException
	 */
	public EventReader<Event> open(InputStreamSource source) throws IOException;
	
	interface InputStreamSource {
		
		public InputStream open() throws IOException;		
	}
}
