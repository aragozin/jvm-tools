package org.gridkit.jvmtool.stacktrace;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class MagicReader {

    private static final String MAGIC_APHLABET = "-_.0123456789ABCDEFHIJKLMNOPQRSTUVWXYZabcdefhijklmnopqrstuvwxyz";

    // reads chars until space
    public static byte[] readMagic(InputStream is) throws IOException {
        byte[] buf = new byte[32];
        int n = 0;
        while(true) {
            int c = is.read();
            if (c < 0) {
                throw new EOFException("Cannot read magic");
            }
            if (n >= buf.length) {
                throw new IOException("Cannot read magic");
            }
            if (c == ' ') {
                buf[n] = (byte) c;
                ++n;
                break;
            }
            else if (MAGIC_APHLABET.indexOf(c) >= 0) {
                buf[n] = (byte) c;
            }
            else {
                throw new IOException("Invalid magic");
            }
            ++n;
        }
        return Arrays.copyOf(buf, n);
    }
}
