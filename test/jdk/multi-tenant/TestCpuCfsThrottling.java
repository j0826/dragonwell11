
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import com.alibaba.tenant.TenantConfiguration;
import com.alibaba.tenant.TenantContainer;
import com.alibaba.tenant.TenantException;
import jdk.test.lib.TestUtils;
import static jdk.test.lib.Asserts.*;
import static jdk.test.lib.tenant.JGroupMirror.*;

/*
 * @test
 * @summary Test for cpu.cfs controller
 * @library /test/lib
 * @build TestCpuCfsThrottling jdk.test.lib.tenant.JGroupMirror
 * @run main/othervm -Xint -XX:+MultiTenant -XX:+TenantCpuThrottling -XX:+TenantCpuAccounting
 *                   --add-opens java.base/com.alibaba.tenant=ALL-UNNAMED
 *                   --illegal-access=permit
 *                   -Xmx200m -Xms200m TestCpuCfsThrottling
 */

public class TestCpuCfsThrottling {


    private void testCpuCfsQuotas() {
        // limit whole JVM process to be running on one CPU core
        Path jvmCpusetCpusPath = Paths.get(
                rootPathOf("cpu"),
                JVM_GROUP_PATH,
                "cpuset.cpus");
        try {
            Files.write(jvmCpusetCpusPath, "0".getBytes() /* limit jvm process to CPU core-#0 */);
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }

        // create two tenants and start two threads running on same core, and fight for CPU resources
        int cfsPeriods[] = {1_000_000, 800_000};
        int cfsQuotas[] = {1_000_000, 300_000};
        TenantConfiguration configs[] = {
                new TenantConfiguration().limitCpuCfs(cfsPeriods[0], cfsQuotas[0]).limitCpuSet("0"),
                new TenantConfiguration().limitCpuCfs(cfsPeriods[1], cfsQuotas[1]).limitCpuSet("0")
        };
        TenantContainer tenants[] = new TenantContainer[configs.length];
        for (int i = 0; i < configs.length; ++i) {
            tenants[i] = TenantContainer.create(configs[i]);
        }

        try {
            long counters[] = new long[configs.length];

            // verify CGroup configurations
            for (int i = 0; i < configs.length; ++i) {
                int actual = Integer.parseInt(getTenantConfig(tenants[i], "cpu.cfs_period_us"));
                assertEquals(cfsPeriods[i], actual);

                actual = Integer.parseInt(getTenantConfig(tenants[i], "cpu.cfs_quota_us"));
                assertEquals(cfsQuotas[i], actual);
            }

            // Start two counter threads in different TenantContainers
            runSerializedCounters(tenants, counters, 10_000);

            // check results
            assertGreaterThan(counters[0], 0L);
            assertGreaterThan(counters[1], 0L);
            assertGreaterThan(counters[0], counters[1]);
            assertGreaterThan(counters[0], counters[1] * 2);
        } finally {
            Stream.of(tenants).forEach(TenantContainer::destroy);
        }
    }

    private void testAdjustCpuCfsQuotas() {
        // limit whole JVM process to be running on one CPU core
        Path jvmCpusetCpusPath = Paths.get(
                rootPathOf("cpu"),
                JVM_GROUP_PATH,
                "cpuset.cpus");
        try {
            Files.write(jvmCpusetCpusPath, "0".getBytes() /* limit jvm process to CPU core-#0 */);
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }

        // create two tenants and start two threads running on same core, and fight for CPU resources
        TenantConfiguration configs[] = {
                new TenantConfiguration().limitCpuCfs(1_000_000, 800_000).limitCpuSet("0"),
                new TenantConfiguration().limitCpuCfs(1_000_000, 300_000).limitCpuSet("0")
        };
        TenantContainer tenants[] = new TenantContainer[configs.length];
        for (int i = 0; i < configs.length; ++i) {
            tenants[i] = TenantContainer.create(configs[i]);
        }
        long counters[] = new long[configs.length];

        try {
            // Start two counter threads in different TenantContainers
            runSerializedCounters(tenants, counters, 10_000);

            // check results
            assertGreaterThan(counters[0], 0L);
            assertGreaterThan(counters[1], 0L);
            assertGreaterThan(counters[0], counters[1]);
            assertGreaterThan(counters[0], counters[1] * 2);

            // cfs limitation adjustment needs time to take effect, here we sleep for a while!
            try {
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                fail();
            }

            // ======== round 2 testing, after modify resouce limitations ==========
            tenants[0].update(configs[0].limitCpuCfs(1_000_000, 300_000));
            tenants[1].update(configs[1].limitCpuCfs(1_000_000, 800_000));
            counters[0] = 0L;
            counters[1] = 0L;
            runSerializedCounters(tenants, counters, 10_000);
            // check results
            assertGreaterThan(counters[0], 0L);
            assertGreaterThan(counters[1], 0L);
            assertGreaterThan(counters[1], counters[0]);
            assertGreaterThan(counters[1], counters[0] * 2);
        } finally {
            Stream.of(tenants).forEach(TenantContainer::destroy);
        }
    }

    // run several counters in concurrent threads
    private static void runConcurrentCounters(TenantContainer[] tenants,
                                    long[] counters,
                                    long milliLimit) {
        if (tenants == null || counters == null) {
            throw new IllegalArgumentException("Bad args");
        }
        // Start two counter threads in different TenantContainers
        CountDownLatch startCounting = new CountDownLatch(1);
        AtomicBoolean stopCounting = new AtomicBoolean(false);
        Thread threadRefs[] = new Thread[2];
        for (int i = 0; i < tenants.length; ++i) {
            TenantContainer tenant = tenants[i];
            int idx = i;
            try {
                // start a single non-root tenant thread to execute counter
                tenant.run(()-> {
                    threadRefs[idx] = new Thread(()->{
                        try {
                            startCounting.await();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            fail();
                        }
                        while (!stopCounting.get()) {
                            ++counters[idx];
                        }
                    });
                    threadRefs[idx].start();
                });
            } catch (TenantException e) {
                e.printStackTrace();
                fail();
            }
        }

        // ROOT tenant thread will serve as monitor & controller
        startCounting.countDown();
        try {
            // the absolute exec time limits for two tenant threads are same
            Thread.sleep(milliLimit);
            stopCounting.set(true);
            for (Thread t : threadRefs) {
                t.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail();
        }
    }

    // execute an action after certain milli seconds, clock start ticking after notified by startLatch
    private static void runTimedTask(long afterMillis, CountDownLatch startLatch, Runnable action) {
        Thread timer = new Thread(() -> {
            if (startLatch != null) {
                try {
                    startLatch.await();
                } catch (InterruptedException e) {
                    // ignore
                }
            }

            long start = System.currentTimeMillis();
            long passed = 0;
            while (passed < afterMillis) {
                try {
                    Thread.sleep(afterMillis - passed);
                } catch (InterruptedException e) {
                    //ignore
                } finally {
                    passed = System.currentTimeMillis() - start;
                }
            }
            // performan action after specific time
            action.run();
        });
        timer.start();
    }

    private static void runSerializedCounters(TenantContainer[] tenants,
                                    long[] counters,
                                    long milliLimit) {
        if (tenants == null || counters == null) {
            throw new IllegalArgumentException("Bad args");
        }

        AtomicBoolean stopCounting = new AtomicBoolean(false);

        for (int i = 0; i < tenants.length; ++i) {
            int idx = i;
            stopCounting.set(false);
            CountDownLatch startCounting = new CountDownLatch(1);
            TenantContainer tenant = tenants[i];
            runTimedTask(milliLimit, startCounting, ()-> {
                stopCounting.set(true);
            });
            try {
                // start a single non-root tenant thread to execute counter
                tenant.run(() -> {
                    startCounting.countDown();
                    while (!stopCounting.get()) {
                        ++counters[idx];
                    }
                });
            } catch (TenantException e) {
                e.printStackTrace();
                fail();
            }
        }
    }

    public static void main(String[] args) {
        TestUtils.runWithPrefix("test",
                TestCpuCfsThrottling.class,
                new TestCpuCfsThrottling());
    }
}
