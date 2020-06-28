package org.gridkit.jvmtool.win32;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Properties;

public class SjkWinHelper {
    static {
        try {
            loadNative();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
//        System.load("C:/WarZone/spaces/_rust_jni/rust-jni-test/target/release/hello.dll");
//        System.load("C:/WarZone/spaces/_rust_jni/rust-jni-test/src/lib.dll");
//        System.load("C:/WarZone/spaces/_rust_jni/rust-jni-test/src/sjkwinh.dll");
    }

    private static void loadNative() throws IOException {
        File tmp = File.createTempFile("java", "").getAbsoluteFile().getParentFile();
        if (tmp.getPath().startsWith("/")) {
            // this is not a windows
            throw new RuntimeException("Cannot load on non Windows OS");
        }
        if ("x86".equals(ManagementFactory.getOperatingSystemMXBean().getArch())) {
            load(tmp, "32");
        }
        else {
            load(tmp, "64");
        }
    }

    private static void load(File tmp, String arch) throws IOException {
        InputStream is = SjkWinHelper.class.getClassLoader().getResourceAsStream("sjkwinh.prop");
        Properties prop = new Properties();
        prop.load(is);

        String dllName = "sjkwinh" + arch + "." + prop.getProperty("dll" + arch + ".hash") + ".dll";
        int len = Integer.valueOf(prop.getProperty("dll" + arch + ".len"));

        File tgt = new File(tmp, dllName);
        if (tgt.isFile() && tgt.length() == len) {
            // dll is present
        }
        else {
            tgt.delete();
            InputStream dllis = SjkWinHelper.class.getClassLoader().getResourceAsStream("sjkwinh" + arch + ".dll");
            byte[] buf = new byte[len];
            int n = dllis.read(buf);
            if (n != buf.length) {
                throw new RuntimeException("Failed extract dll, size mismatch");
            }
            FileOutputStream fos = new FileOutputStream(tgt);
            fos.write(buf);
            fos.close();
        }

        System.load(tgt.getPath());
    }

    private native int GetProcessTimes(int pid, int[] buf);
    private native int GetThreadTimes(int pid, int[] buf);
    private native long QueryProcessCycleTime(int pid);
    private native long QueryThreadCycleTime(int pid);
    private native int EnumThreads(int pid, int[] buf);

    int[] callBuf = new int[8];

    /**
     * Call kernel32::GetProcessTimes.
     * If successful kernel and user times are set to
     * first two slots in array.
     * <p>
     * Time units are microseconds.
     *
     * @param pid
     * @return <code>false</code> is not successful
     */
    public synchronized boolean getProcessCpuTimes(int pid, long[] result) {
        int rc = GetProcessTimes(pid, callBuf);
        if (rc == 0) {
            long ktime = (0xFFFFFFFFl & callBuf[4]) | ((long)callBuf[5]) << 32;
            long utime = (0xFFFFFFFFl & callBuf[6]) | ((long)callBuf[7]) << 32;

            result[0] = ktime / 10;
            result[1] = utime / 10;
            return true;
        }
        else {
            System.out.println("Error code: " + rc);
            return false;
        }
    }

    /**
     * Call kernel32::GetThreadTimes.
     * If successful kernel and user times are set to
     * first two slots in array.
     * <p>
     * Time units are microseconds.
     *
     * @param pid
     * @return <code>false</code> is not successful
     */
    public synchronized boolean getThreadCpuTimes(int pid, long[] result) {
        int rc = GetThreadTimes(pid, callBuf);
        if (rc == 0) {
            long ktime = (0xFFFFFFFFl & callBuf[4]) | ((long)callBuf[5]) << 32;
            long utime = (0xFFFFFFFFl & callBuf[6]) | ((long)callBuf[7]) << 32;

            result[0] = ktime / 10;
            result[1] = utime / 10;
            return true;
        }
        else {
            System.out.println("Error code: " + rc);
            return false;
        }
    }

    public synchronized long getProcessCpuCycles(int pid) {
        return QueryProcessCycleTime(pid);
    }

    public synchronized long getThreadCpuCycles(int pid) {
        return QueryThreadCycleTime(pid);
    }

    public synchronized int[] enumThreads(int pid) {
        int[] buf = new int[128];
        int n = EnumThreads(pid, buf);
        if (n < 0) {
            throw new RuntimeException("Cannot enum threads. WinError: " + (-n));
        }
        while (n > buf.length) {
            buf = new int[n + 16];
            n = EnumThreads(pid, buf);
            if (n < 0) {
                throw new RuntimeException("Cannot enum threads. WinError: " + (-n));
            }
        }
        return buf.length == n ? buf : Arrays.copyOf(buf, n);
    }
}
