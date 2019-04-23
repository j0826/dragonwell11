
/*
 * @test
 * @summary Test function of killing tenants' threads
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm/timeout=20 -XX:+UnlockDiagnosticVMOptions -XX:+IgnoreUnrecognizedVMOptions
 *                              -XX:+TraceTenantKillThreads -XX:+MultiTenant -XX:+TenantThreadStop TestUncaughtTenantDeathException
 *
 */

import com.alibaba.tenant.TenantConfiguration;
import com.alibaba.tenant.TenantContainer;
import com.alibaba.tenant.TenantException;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import java.util.concurrent.CountDownLatch;
import static jdk.test.lib.Asserts.*;

public class TestUncaughtTenantDeathException {

    final String keyAllowDispatchingUncaught = "com.alibaba.tenant.AllowDispatchingTenantDeathException";
    final String keyAllowPrintUncaught = "com.alibaba.tenant.PrintUncaughtTenantDeathException";

    // To test set/clear of property com.alibaba.tenant.PrintUncaughtTenantDeathException
    public static class TenantRunnerTask {
        public static void main(String[] args) throws TenantException, InterruptedException {
            TenantConfiguration config = new TenantConfiguration(1024, 64 * 1024 * 1024);
            TenantContainer tenant = TenantContainer.create(config);
            Thread[] t = new Thread[1];
            CountDownLatch c = new CountDownLatch(1);
            tenant.run(() -> {
                t[0] = new Thread(()-> {
                    System.err.println("child: uce handler="+Thread.currentThread().getUncaughtExceptionHandler());
                    System.err.println("child: threadGroup=" + Thread.currentThread().getThreadGroup());
                    long l = 0;
                    c.countDown();
                    while (true) { if (++l > 0xFFFFFF) l = 0; }
                });
                t[0].start();
            });
            c.await();
            tenant.destroy();
            assertFalse(t[0].isAlive());
            t[0].join();
            assertEquals(t[0].getState(), Thread.State.TERMINATED);
        }
    }

    private void testUncaughtExceptionOutput() throws Exception {


        // test PrintUncaughtTenantDeathException enabled
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                "-XX:+UnlockDiagnosticVMOptions",
                "-XX:+IgnoreUnrecognizedVMOptions",
                "-XX:+MultiTenant",
                "-XX:+TenantThreadStop",
                "-XX:+UseG1GC",
                "-D" + keyAllowDispatchingUncaught + "=true",
                "-D" + keyAllowPrintUncaught + "=true",
                TenantRunnerTask.class.getName());

        OutputAnalyzer output_detail = new OutputAnalyzer(pb.start());
        System.err.println(output_detail.getStderr());
        System.out.println(output_detail.getStdout());
        output_detail.shouldContain("Exception in thread");
        output_detail.shouldContain("com.alibaba.tenant.TenantDeathException");
        output_detail.shouldHaveExitValue(0);

        // test printUncaughtTenantKillThreadException disabled
        pb = ProcessTools.createJavaProcessBuilder(
                "-XX:+UnlockDiagnosticVMOptions",
                "-XX:+IgnoreUnrecognizedVMOptions",
                "-XX:+MultiTenant",
                "-XX:+TenantThreadStop",
                "-XX:+UseG1GC",
                "-D" + keyAllowDispatchingUncaught + "=false",
                "-D" + keyAllowPrintUncaught + "=false",
                TenantRunnerTask.class.getName());

        output_detail = new OutputAnalyzer(pb.start());
        output_detail.shouldNotContain("Exception in thread");
        output_detail.shouldNotContain("com.alibaba.tenant.TenantDeathException");
        output_detail.shouldHaveExitValue(0);
    }

    public static void main(String[] args) throws Exception {
        TestUncaughtTenantDeathException test = new TestUncaughtTenantDeathException();
        test.testUncaughtExceptionOutput();
    }
}
