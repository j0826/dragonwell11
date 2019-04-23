
/*
 * @test
 * @summary Test ending multi-tenant jvm process while JDWP options enabled
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm/timeout=20 TestKillThreadWithJDWP
 *
 */

import com.alibaba.tenant.TenantConfiguration;
import com.alibaba.tenant.TenantContainer;
import com.alibaba.tenant.TenantException;
import java.util.concurrent.CountDownLatch;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import static jdk.test.lib.Asserts.*;

public class TestKillThreadWithJDWP {

    // To test set/clear of property com.alibaba.tenant.printUncaughtTenantKillThreadException
    public static class TenantRunnerTask {
        public static void main(String[] args) throws TenantException, InterruptedException {
            TenantConfiguration config = new TenantConfiguration(1024, 64 * 1024 * 1024);
            TenantContainer tenant = TenantContainer.create(config);
            Thread[] t = new Thread[1];
            CountDownLatch c = new CountDownLatch(1);
            tenant.run(() -> {
                t[0] = new Thread(()-> {
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

    public static void main(String[] args) throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                "-XX:+UnlockDiagnosticVMOptions",
                "-XX:+IgnoreUnrecognizedVMOptions",
                "-XX:+MultiTenant",
                "-XX:+TenantThreadStop",
                "-XX:+TenantHeapThrottling",
                "-XX:+UseG1GC",
                "-agentlib:jdwp=server=y,suspend=n,transport=dt_socket,address=19987",
                TenantRunnerTask.class.getName());

        OutputAnalyzer out = new OutputAnalyzer(pb.start());
        assertEquals(0, out.getExitValue());
    }
}
