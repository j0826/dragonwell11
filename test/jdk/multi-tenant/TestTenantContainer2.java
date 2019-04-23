
import com.alibaba.tenant.TenantContainer;
import com.alibaba.tenant.TenantException;
import com.alibaba.tenant.TenantConfiguration;
import com.alibaba.tenant.TenantGlobals;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import static jdk.test.lib.Asserts.*;

/* @test
 * @summary unit tests for com.alibaba.tenant.TenantContainer
 * @library /test/lib
 * @compile TestTenantContainer.java
 * @run main/othervm -XX:+UseG1GC -XX:+MultiTenant -XX:+TenantDataIsolation -Xmx600m  -Xms200m
 *                   -Dcom.alibaba.tenant.test.prop=root  TestTenantContainer2
 */
public class TestTenantContainer2 {
    static private final int MAP_SIZE = 128;
    static private final int MAP_ARRAY_LENGTH = 40;

    private Map<Integer, String> populateMap(int size) {
        Map<Integer, String> map = new HashMap<Integer, String>();
        for (int i = 0; i < size; i += 1) {
            String valStr = "value is [" + i + "]";
            map.put(i, valStr);
        }
        return map;
    }

    public void testRunInRootTenant() throws TenantException {
        TenantConfiguration tconfig = new TenantConfiguration();
        final TenantContainer tenant = TenantContainer.create(tconfig);
        tenant.run(() -> {
            assertSame(TenantContainer.current(), tenant);
            TenantContainer.primitiveRunInRoot(() -> {
                //should be in root tenant.
                assertNull(TenantContainer.current());
            });
        });
    }

    public void testRun() {
        TenantConfiguration tconfig = new TenantConfiguration();
        final TenantContainer tenant = TenantContainer.create(tconfig);
        final TenantContainer tenant2 = TenantContainer.create(tconfig);
        try {
            tenant.run(() -> {
                // it is not allowed to run into tenant 2 from tenant
                try {
                    tenant2.run(() -> {
                        // should not reach here.
                        fail();
                    });
                } catch (TenantException e) {
                    // Expected
                }
                // it is allowed to run into same tenant
                try {
                    tenant.run(() -> {
                        assertEquals(TenantContainer.current().getTenantId(), tenant.getTenantId());
                        System.out.println("testRun: thread [" + Thread.currentThread().getName()
                                + "] is running in tenant: " + TenantContainer.current().getTenantId());
                    });
                } catch (TenantException e) {
                    fail();
                }

            });
        } catch (TenantException e) {
            fail();
        }
        tenant.destroy();
        tenant2.destroy();
    }

    class TenantWorker implements Runnable {
        public Tenant tenant;
        public long cpuTime;

        public long getCpuTime() {
            ThreadMXBean bean = ManagementFactory.getThreadMXBean();
            return bean.isCurrentThreadCpuTimeSupported() ? bean
                    .getCurrentThreadCpuTime() : 0L;
        }

        TenantWorker(Tenant t) {
            tenant = t;
        }

        public void run() {
            while (System.currentTimeMillis() - tenant.pre_ms < 3000) {
                tenant.nextCount();
            }
            cpuTime = getCpuTime();
        }
    }

    class Tenant implements Runnable {
        public AtomicLong count = new AtomicLong(0);
        public long pre_ms = 0;
        TenantWorker workers[];
        int max_cpu;
        Thread threads[];
        long time = 0;

        public long nextCount() {
            return count.incrementAndGet();
        }

        public void run() {
            max_cpu = Runtime.getRuntime().availableProcessors();
            workers = new TenantWorker[max_cpu];
            threads = new Thread[max_cpu];
            pre_ms = System.currentTimeMillis();
            for (int i = 0; i < max_cpu; i++) {
                workers[i] = new TenantWorker(this);
                threads[i] = new Thread(workers[i]);
                threads[i].start();
            }
        }

        public long getcpuTime() {
            for (int i = 0; i < max_cpu; i++) {
                try {
                    threads[i].join();
                    time += workers[i].cpuTime;
                } catch (InterruptedException e) {
                    System.out.println("Interreupted...");
                }
            }
            return time;
        }

    }

    /*
    public void testTenantCpuThrottling() throws TenantException {
        if (!TenantGlobals.isCpuThrottlingEnabled()) {
            return;
        }

        long time0 = 0;
        long time1 = 0;
        // put two tenants on same CPU core
        TenantConfiguration tconfig0 = new TenantConfiguration().limitCpuShares(512).limitCpuSet("0");
        TenantConfiguration tconfig1 = new TenantConfiguration().limitCpuShares(1024).limitCpuSet("0");
        TenantContainer container0 = TenantContainer.create(tconfig0);
        TenantContainer container1 = TenantContainer.create(tconfig1);
        Tenant tenant0 = new Tenant();
        Tenant tenant1 = new Tenant();

        container0.run(tenant0);
        container1.run(tenant1);

        time1 = tenant1.getcpuTime();
        time0 = tenant0.getcpuTime();

        System.out.println("testTenantCpuThrottling tenant0 count: " + tenant0.count + " tenant0 CpuTime:" + time0
                + " tenant1 count: " + tenant1.count + " tenant1 CpuTime:" + time1);
        double rate = 0.0;
        if (TenantGlobals.isCpuThrottlingEnabled()) {
            rate = (double) time1 / (time0 * 2);
            if ((rate > 1.1) || (rate < 0.9)) {
                fail();
            }
        } else {
            rate = (double) time1 / time0;
        }
        System.out.println("CPU throttling enabled: " + TenantGlobals.isCpuThrottlingEnabled() + ", rate = " + rate);
    }
    */

    public void testCurrentWithGC() throws TenantException {
        // should be in the root tenant at the begging
        assertNull(TenantContainer.current());

        TenantConfiguration tconfig = new TenantConfiguration();
        final TenantContainer tenant = TenantContainer.create(tconfig);
        tenant.run(() -> {
            int arrayIndex = 0;
            Object[] array = new Object[MAP_ARRAY_LENGTH];
            while (arrayIndex < MAP_ARRAY_LENGTH * 20) {
                // run in 'current' tenant
                assertSame(TenantContainer.current(), tenant);
                Map<Integer, String> map = populateMap(MAP_SIZE);
                array[arrayIndex % MAP_ARRAY_LENGTH] = map;
                arrayIndex++;

                Thread.yield();
                System.gc();
            }

            System.out.println("testCurrent: thread [" + Thread.currentThread().getName() + "] is running in tenant: "
                    + TenantContainer.current().getTenantId());

        });

        // switch back to root tenant.
        assertNull(TenantContainer.current());
        tenant.destroy();
    }

    /*
    public void testTenantGetAllocatedMemory() throws TenantException, InterruptedException {
        TenantConfiguration tconfig = new TenantConfiguration();
        final TenantContainer tenant = TenantContainer.create(tconfig);

        CountDownLatch p1 = new CountDownLatch(1);
        CountDownLatch p2 = new CountDownLatch(1);
        CountDownLatch p3 = new CountDownLatch(1);
        CountDownLatch p4 = new CountDownLatch(1);
        CountDownLatch startTest = new CountDownLatch(2);
        CountDownLatch endTest = new CountDownLatch(1);

        long allocatedMem0 = 0;
        long allocatedMem1 = 0;
        long allocatedMem2 = 0;
        long allocatedMem3 = 0;

        assertTrue(0 == tenant.getAllocatedMemory());
        tenant.run(() -> {
            Thread thread1 = new Thread(() -> {
                assertTrue(TenantContainer.current() == tenant);
                System.out.println("thread1 started");
                try {
                    byte[] byteArray0 = new byte[10240];
                    startTest.countDown();
                    p1.await();
                    byte[] byteArray1 = new byte[10240];
                    endTest.await();
                } catch (InterruptedException ee) {
                    ee.printStackTrace();
                    fail();
                } finally {
                    System.out.println("thread1 ended");
                }
            });
            thread1.start();
        });

        Thread thread2 = new Thread(() -> {
            System.out.println("thread2 started");
            try {
                startTest.countDown();

                p2.await();

                tenant.run(() -> {
                    byte[] byteArray0 = new byte[10240];
                });

                p3.await();

                byte[] byteArray1 = new byte[10240];

                tenant.run(() -> {
                    byte[] byteArray0 = new byte[10240];
                });
                byte[] byteArray2 = new byte[10240];
                endTest.await();

            } catch (InterruptedException | TenantException ee) {
                ee.printStackTrace();
                fail();
            } finally {
                System.out.println("thread2 ended");
            }
        });
        thread2.start();

        startTest.await();

        allocatedMem0 = tenant.getAllocatedMemory();
        System.out.println("allocatedMem0 = " + allocatedMem0);
        assertTrue(allocatedMem0 >= 10240);

        p1.countDown();
        Thread.sleep(1000);
        allocatedMem1 = tenant.getAllocatedMemory();
        System.out.println("allocatedMem1 = " + allocatedMem1);
        assertTrue((allocatedMem1 - allocatedMem0) >= 10240 && (allocatedMem1 - allocatedMem0) < 11240);

        p2.countDown();
        Thread.sleep(1000);
        allocatedMem2 = tenant.getAllocatedMemory();
        System.out.println("allocatedMem2 = " + allocatedMem2);
        assertTrue((allocatedMem2 - allocatedMem1) >= 10240 && (allocatedMem2 - allocatedMem1) < 11240);

        p3.countDown();
        Thread.sleep(1000);
        allocatedMem3 = tenant.getAllocatedMemory();
        System.out.println("allocatedMem3 = " + allocatedMem3);
        assertTrue((allocatedMem3 - allocatedMem2) >= 10240 && (allocatedMem3 - allocatedMem2) < 11240);

        p4.countDown();
        Thread.sleep(1000);
        tenant.destroy();

        endTest.countDown();
        System.out.println("testTenantGetAllocatedMemory passed!");
    }
    */

    public static void main(String[] args) throws Exception {
        TestTenantContainer2 test = new TestTenantContainer2();
        test.testRun();
        test.testCurrentWithGC();
        test.testRunInRootTenant();
        // TODO: enable below tests after porting TenantCpuThrotting
        //test.testTenantCpuThrottling();
        // TODO: enable below tests after porting TenantHeapIsolation
        //test.testTenantGetAllocatedMemory();
    }
}
