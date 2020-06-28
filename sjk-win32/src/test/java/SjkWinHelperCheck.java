import org.gridkit.jvmtool.win32.SjkWinHelper;
import org.junit.Test;

public class SjkWinHelperCheck {

    @Test
    public void cpu_by_pid() throws Throwable {
        try {
            //SjkWinHelper.main(new String[0]);
            SjkWinHelper helper = new SjkWinHelper();
            long[] buf = new long[2];
            int pid = 10048;
            for(int i = 0; i != 10; ++i) {
                if (helper.getProcessCpuTimes(pid, buf)) {
                    System.out.println("Proc [" + pid + "]  kernel: " + toSec(buf[0]) + " user: " +  toSec(buf[1]));
                }
                else {
                    System.out.println("Proc [" + pid + "] - no data");
                }
                System.out.println("Proc [" + pid + "] CC " + helper.getProcessCpuCycles(pid));
                Thread.sleep(500);
            }
        }
        catch(Throwable e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    public void cpu_by_tid() throws Throwable {
        try {
            //SjkWinHelper.main(new String[0]);
            SjkWinHelper helper = new SjkWinHelper();
            long[] buf = new long[2];
            int pid = 3164;

            for(int i = 0; i != 10; ++i) {
                if (helper.getThreadCpuTimes(pid, buf)) {
                    System.out.println("Thread [" + pid + "]  kernel: " + toSec(buf[0]) + " user: " +  toSec(buf[1]));
                }
                else {
                    System.out.println("Thread [" + pid + "] - no data");
                }
                System.out.println("Thread [" + pid + "] CC " + helper.getThreadCpuCycles(pid));
                Thread.sleep(500);
            }
        }
        catch(Throwable e) {
            e.printStackTrace();
            throw e;
        }
    }

    private String toSec(long us) {
        return String.format("%f", us * 0.000001d);
    }

    @Test
    public void enum_threads() {
        SjkWinHelper helper = new SjkWinHelper();
        int pid = 10048;

        int[] threads = helper.enumThreads(pid);
        System.out.println("PID " + pid + " threads " + threads.length);
        for(int t : threads) {
            System.out.println("  " + t);
        }
    }
}
