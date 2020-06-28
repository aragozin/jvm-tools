package org.gridkit.jvmtool.spi.parsers;

import java.io.IOException;
import java.io.InputStream;

/**
 * A simple interface for {@link InputStream} which can re reopen.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public interface InputStreamSource {

    public InputStream open() throws IOException;
}
