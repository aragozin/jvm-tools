import static java.util.Collections.disjoint;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.junit.Test;

public class PropGen {

    public static String digest(byte[] data, String algorithm) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] digest = md.digest(data);
            StringBuilder buf = new StringBuilder();
            for(byte b: digest) {
                buf.append(Integer.toHexString(0xF & (b >> 4)));
                buf.append(Integer.toHexString(0xF & (b)));
            }
            return buf.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public byte[] load(String res) throws IOException {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(res);
        byte[] buf = new byte[128 << 10];
        int n = is.read(buf);
        return Arrays.copyOf(buf, n);
    }

    @Test
    public void printProps() throws IOException {
        byte[] dll32 = load("sjkwinh32.dll");
        String hash32 = digest(dll32, "SHA-256");

        byte[] dll64 = load("sjkwinh64.dll");
        String hash64 = digest(dll64, "SHA-256");

        System.out.println("dll32.hash: " + hash32);
        System.out.println("dll32.len: " + dll32.length);
        System.out.println("dll64.hash: " + hash64);
        System.out.println("dll64.len: " + dll64.length);
    }
}
