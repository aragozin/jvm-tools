package org.gridkit.jvmtool.bstub;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

class StreamHelper {

    public static byte[] readFile(File file) {
        try {
            if (file.length() > 1 << 30) {
                throw new ArrayIndexOutOfBoundsException("File is too big");
            }
            byte[] data = new byte[(int)file.length()];
            FileInputStream fis = new FileInputStream(file);
            try {
                int n = 0;
                while(n < data.length) {
                    int m = fis.read(data, n, data.length - n);
                    if (m < 0) {
                        throw new RuntimeException("Cannot read file: " + file.getCanonicalPath());
                    }
                    n += m;
                }
            }
            finally {
                try {
                    fis.close();
                }
                catch(IOException e) {
                    // ignore
                }
            }
            return data;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } 
    }
}
