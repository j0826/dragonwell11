
/*
 * @test
 * @summary Test destroy of tenant allocation contexts
 * @library /test/lib
 * @build TestTenantDestroy
 * @run main/othervm -XX:+MultiTenant -XX:+TenantDataIsolation -XX:+UseG1GC -Xmx600m  -Xms200m TestTenantDestroy
 */
import static jdk.test.lib.Asserts.*;
import com.alibaba.tenant.TenantException;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import com.alibaba.tenant.TenantConfiguration;
import com.alibaba.tenant.TenantContainer;
import com.alibaba.tenant.TenantState;
import java.util.stream.IntStream;

public class TestTenantDestroy {

    public static void main(String[] args) throws Exception {
        TestTenantDestroy test = new TestTenantDestroy();
        test.testNormalDestroy();
        test.testExceptionAfterDestroy();
        test.testRuntimeExceptionDuringDestroy();
    }

    private void testRuntimeExceptionDuringDestroy() {
        TenantContainer tenant = TenantContainer.create(new TenantConfiguration());
        try {
            tenant.run(()->
                Runtime.getRuntime().addShutdownHook(new Thread() {
                    public synchronized void start() {
                        throw new RuntimeException("Oops");
                    }
                }));
        } catch (TenantException e) {
            e.printStackTrace();
            fail();
        }

        tenant.destroy();

        assertEquals(TenantState.DEAD, tenant.getState());
        assertNull(TenantContainer.getTenantContainerById(tenant.getTenantId()));
    }

    private void testNormalDestroy() throws Exception {
        int count = 16;
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                "-XX:+UnlockDiagnosticVMOptions",
                "-XX:+IgnoreUnrecognizedVMOptions",
                "-XX:+MultiTenant",
                "-XX:+TenantHeapIsolation",
                "-XX:+TraceG1TenantAllocationContext",
                "-XX:+UseG1GC",
                TenantWrapper.class.getName(),
                count + "");

        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        if (isDebugBuild()) {
            output.shouldContain("Destroy G1TenantAllocationContext");
        }
        for (int i = 0; i < count; ++i) {
            output.shouldContain("Creating tenant[" + i + "]");
            output.shouldContain("Inside tenant[" + i + "]");
        }
        output.shouldNotContain("Exception from tenant code");
        output.shouldHaveExitValue(0);
    }

    // class to be launched as child process
    public static class TenantWrapper implements  Runnable {
        public static void main(String[] args) {
            assertTrue(args.length >= 1, "need at least a length parameter");
            int count = Integer.parseInt(args[0]);
            new TenantWrapper(count).run();
        }

        private int tenantCount;

        TenantWrapper(int count) {
            tenantCount = count;
            assertTrue(count > 0, "cannot create <=0 tenants");
        }

        @Override
        public void run() {
            IntStream.range(0, tenantCount)
                    .forEach(i -> {
                                System.out.println("Creating tenant[" + i + "]");

                                TenantConfiguration config = new TenantConfiguration();
                                TenantContainer tenant = TenantContainer.create(config);
                                try {
                                    tenant.run(() -> {
                                        System.out.println("Inside tenant[" + i + "]");
                                    });
                                } catch (TenantException e) {
                                    e.printStackTrace();
                                    assertTrue(false, "Exception from tenant code");
                                }
                                tenant.destroy();
                                assertEquals(tenant.getState(), TenantState.DEAD);
                            }
                    );

            System.gc();
            // all tenant containers and all allocation context objects should be reclaimed
        }
    }

    private void testExceptionAfterDestroy() {
        TenantConfiguration config = new TenantConfiguration();
        final TenantContainer tenant = TenantContainer.create(config);
        try {
            tenant.run(() -> {
                System.out.println("Inside tenant");
            });
        } catch (TenantException e) {
            e.printStackTrace();
            assertTrue(false, "Exception from tenant code");
        }

        tenant.destroy();
        assertEquals(tenant.getState(), TenantState.DEAD);

        try {
                tenant.run(()->{ System.out.println("run after destroy"); });
                assertTrue(false, "Cannot run inside a dead tenant");
        } catch (TenantException e) {
                // expected
        }
    }

    private static boolean isDebugBuild() {
        return System.getProperty("java.vm.version").toLowerCase().contains("debug");
    }
}
