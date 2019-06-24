
/*
 * @test
 * @summary Test dumping thread stacks
 * @library /test/lib
 * @run main/othervm TestDumpTenantThreadStacks
 */

import com.alibaba.tenant.TenantConfiguration;
import com.alibaba.tenant.TenantContainer;
import com.alibaba.wisp.engine.WispEngine;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import static jdk.test.lib.Asserts.*;

// TODO: enable Wisp code

public class TestDumpTenantThreadStacks {

    static {
        int nofProcs = Runtime.getRuntime().availableProcessors();
        TEST_PARALLELISM = (nofProcs > 2 ? (nofProcs >> 1) : nofProcs);
    }

    // property name to check if -XX:+EnableCoroutine is specified
    private static final String WISP_ENABLED_PROP = "test.wispEnabled";

    // worker threads to start
    private static final int TEST_PARALLELISM;

    public static void main(String[] args) throws Exception {
        //
        // The configurations are organized as array of String[],
        // Each neighboring pair from an even index is in form of
        // [ ..., [ child JVM arguments ], [ expected outputs ], ... ]
        //
        String[][] configs = {
                // basic testing
                new String[] {
                        "-XX:+MultiTenant",
                        // to allow reflection access to com.alibaba.tenant
                        "--illegal-access=permit",
                        "--add-opens",
                        "java.base/com.alibaba.tenant=ALL-UNNAMED",
                        Observed.class.getName()
                },
                new String[] {
                        "Observed-0",
                        "Observed-" + (TEST_PARALLELISM - 1),
                        "at " + Observed.class.getName() + ".foo",
                        "at " + Observed.class.getName() + ".blocker"
                },
                // basic testing with coroutine
                new String[] {
                        "-XX:+MultiTenant",
                        "-XX:+EnableCoroutine",
                        // to allow reflection access to com.alibaba.tenant
                        "--illegal-access=permit",
                        "--add-opens",
                        "java.base/com.alibaba.tenant=ALL-UNNAMED",
                        "-D" + WISP_ENABLED_PROP + "=true",
                        Observed.class.getName()
                },
                new String[] {
                        "Observed-0",
                        "Observed-" + (TEST_PARALLELISM - 1),
                        "at " + Observed.class.getName() + ".foo",
                        "at " + Observed.class.getName() + ".blocker",
                        "- Coroutine",
                        "waiting on condition"
                },
                // synchronous killing
                new String[] {
                        "-XX:+MultiTenant",
                        "-XX:+TenantThreadStop",
                        // to allow reflection access to com.alibaba.tenant
                        "--illegal-access=permit",
                        "--add-opens",
                        "java.base/com.alibaba.tenant=ALL-UNNAMED",
                        // has to specify this to enable async mode, otherwise test will hang infinitely.
                        // please note the time is total STW time, not real time.
                        "-Dcom.alibaba.tenant.ShutdownSTWSoftLimit=1000",
                        "-Dcom.alibaba.tenant.PrintStacksOnTimeoutDelay=500",
                        "-Dcom.alibaba.tenant.DebugTenantShutdown=true",
                        TenantDieHard.class.getName()
                },
                new String[] {
                        "TenantDieHardThread-0",
                        "TenantDieHardThread-" + (TEST_PARALLELISM - 1),
                        "at " + TenantDieHard.class.getName() + ".bar",
                },
                // asynchronous killing
                new String[] {
                        "-XX:+MultiTenant",
                        "-XX:+TenantThreadStop",
                        // to allow reflection access to com.alibaba.tenant
                        "--illegal-access=permit",
                        "--add-opens",
                        "java.base/com.alibaba.tenant=ALL-UNNAMED",
                        "-Dcom.alibaba.tenant.ShutdownSTWSoftLimit=10",
                        "-Dcom.alibaba.tenant.PrintStacksOnTimeoutDelay=10000",
                        "-Dcom.alibaba.tenant.DebugTenantShutdown=true",
                        TenantDieHard.class.getName()
                },
                new String[] {
                        "TenantDieHardThread-0",
                        "TenantDieHardThread-" + (TEST_PARALLELISM - 1),
                        "at " + TenantDieHard.class.getName() + ".bar",
                },
                // with coroutine enabled
                new String[] {
                        "-XX:+MultiTenant",
                        "-XX:+TenantThreadStop",
                        // to allow reflection access to com.alibaba.tenant
                        "--illegal-access=permit",
                        "--add-opens",
                        "java.base/com.alibaba.tenant=ALL-UNNAMED",
                        "-Dcom.alibaba.tenant.ShutdownSTWSoftLimit=1000",
                        "-Dcom.alibaba.tenant.PrintStacksOnTimeoutDelay=500",
                        "-Dcom.alibaba.tenant.DebugTenantShutdown=true",
                        "-XX:+EnableCoroutine",
                        "-D" + WISP_ENABLED_PROP + "=true",
                        TenantDieHard.class.getName()
                },
                new String[] {
                        "TenantDieHardThread-0",
                        "TenantDieHardThread-" + (TEST_PARALLELISM - 1),
                        "at " + TenantDieHard.class.getName() + ".bar",
                        "- Coroutine",
                        "waiting on condition"
                }
        };

        for (int i = 0; i < configs.length; i += 2) {
            assertLessThan(i, configs.length);
            String[] childVMArgs = configs[i];
            String[] expectedOutputs = configs[i + 1];
            assertNotNull(childVMArgs);
            assertNotNull(expectedOutputs);

            ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(childVMArgs);
            OutputAnalyzer output = new OutputAnalyzer(pb.start());

            System.out.println("==== TEST LOOP-" + i + " output=====\n" + output.getOutput());

            output.shouldHaveExitValue(0);
            for (String expectedMsg : expectedOutputs) {
                output.shouldContain(expectedMsg);
            }
        }
    }

    private static class Observed {

        private static final boolean WISP_ENABLED = Boolean.parseBoolean(
                System.getProperty(WISP_ENABLED_PROP, "false"));

        private static CountDownLatch workersReady = new CountDownLatch(TEST_PARALLELISM);

        public static void main(String[] args) throws Exception {
            Thread[] threads = new Thread[TEST_PARALLELISM];
            for (int i = 0; i < threads.length; ++i) {
                threads[i] = new Thread(()->{
                    foo(Observed::blocker);
                });
                threads[i].setName("Observed-" + i);
                threads[i].setDaemon(true);
                threads[i].start();
            }

            workersReady.await();

            dumpThreads(threads);
        }

        // fabricate a stack

        private static void foo(Runnable r) {
            if (WISP_ENABLED) {
                for (int i = 0; i < 10; ++i) {
                    WispEngine.dispatch(Observed::blocker);
                }
            } else {
                r.run();
            }
        }

        private static Void blocker() {
            workersReady.countDown();
            blockThread(-1);
            return null;
        }

    }

    // Test for the scenario that failed to kill threads
    private static class TenantDieHard {

        private static final boolean WISP_ENABLED = Boolean.parseBoolean(
                System.getProperty(WISP_ENABLED_PROP, "false"));

        private static CountDownLatch workersReady = new CountDownLatch(TEST_PARALLELISM);

        public static void main(String[] args) throws Exception {
            TenantContainer tenant = TenantContainer.create(new TenantConfiguration());
            Thread[] threads = new Thread[TEST_PARALLELISM];

            // create tenant threads
            tenant.run(()->{
                for (int i = 0; i < TEST_PARALLELISM; ++i) {
                    threads[i] = new Thread(()-> {
                        bar();
                    });
                    threads[i].setName("TenantDieHardThread-" + i);
                    threads[i].setDaemon(true);
                    threads[i].start();
                }
            });

            workersReady.await();

            tenant.destroy();

            long waitForDumpDelay = Math.max(
                    Long.parseLong(System.getProperty("com.alibaba.tenant.ShutdownSTWSoftLimit")),
                    Long.parseLong(System.getProperty("com.alibaba.tenant.PrintStacksOnTimeoutDelay"))) << 1;
            // wait for thread dump to happen
            System.out.println("# wait for " + waitForDumpDelay + "ms");
            blockThread(waitForDumpDelay);
        }

        private static void bar() {
            // masked without unmask, so TenantContainer.destroy() will never kill this thread
            TenantContainer.maskTenantShutdown();
            Runnable r = () -> {
                workersReady.countDown();
                blockThread(-1);
            };
            if (WISP_ENABLED) {
                WispEngine.dispatch(r);
            } else {
                r.run();
            }
        }
    }

    // to expose com.alibaba.tenant.TenantContainer.dumpThreads via reflection
    private static void dumpThreads(Thread[] threads) {
        try {
            Method m = TenantContainer.class.getDeclaredMethod("dumpThreads", Thread[].class);
            m.setAccessible(true);
            m.invoke(null, new Object[] { threads } );
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            fail();
        }
    }

    // block current thread for certain amount of time or infinitely
    private static void blockThread(long blockTime) {
        if (blockTime <= 0) {
            // infinitely
            while (true) {
                try {
                    Thread.sleep(10_000);
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        } else {
            long msStart = System.currentTimeMillis();
            do {
                long remaining = blockTime - (System.currentTimeMillis() - msStart);
                try {
                    Thread.sleep(remaining);
                } catch (InterruptedException e) {
                    // ignore;
                }
            } while (System.currentTimeMillis() < msStart + blockTime);
        }
    }

}
