package org.gridkit.jvmtool.spi.parsers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Parser may try to downcast {@link InputStreamSource} to this implementation
 * if it supports only file based IO.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class FileInputStreamSource implements InputStreamSource {

    private final File file;

    public FileInputStreamSource(File file) {
        this.file = file;
    }

    public File getSourceFile() {
        return file;
    }

    @Override
    public InputStream open() throws IOException {
        return new FileInputStream(file);
    }
}
