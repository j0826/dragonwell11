
import com.alibaba.tenant.*;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static jdk.test.lib.Asserts.*;

/* @test
 * @summary unit tests for com.alibaba.tenant.TenantContainer
 * @library /test/lib
 * @compile TestTenantContainer.java
 * @run main/othervm -XX:+UseG1GC -XX:+MultiTenant -XX:+TenantDataIsolation -Xmx600m  -Xms200m
 *                   -Dcom.alibaba.tenant.test.prop=root  TestTenantContainer
 */
public class TestTenantContainer {
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

    public void testTenantSystemProperty() {
        String value = System.getProperty("com.alibaba.tenant.enableMultiTenant");
        assertTrue(value != null && "true".equalsIgnoreCase(value));
    }

    public void testCurrent() throws TenantException {
        // should be in the root tenant at the begging
        assertNull(TenantContainer.current());

        TenantConfiguration tconfig = new TenantConfiguration();
        final TenantContainer tenant = TenantContainer.create(tconfig);
        tenant.run(() -> {
            // run in 'current' tenant
            assertSame(TenantContainer.current(), tenant);
            System.out.println("testCurrent: thread [" + Thread.currentThread().getName() + "] is running in tenant: "
                    + TenantContainer.current().getTenantId());
        });

        // switch back to root tenant.
        assertNull(TenantContainer.current());
        tenant.destroy();
    }

    public void testGetTenantID() throws TenantException {
        TenantConfiguration tconfig = new TenantConfiguration();
        final TenantContainer tenant = TenantContainer.create(tconfig);
        tenant.run(() -> {
            assertEquals(TenantContainer.current().getTenantId(), tenant.getTenantId());
            System.out.println("testGetTenantID: thread [" + Thread.currentThread().getName()
                    + "] is running in tenant: " + TenantContainer.current().getTenantId());
        });
        tenant.destroy();
    }

    public void testGetState() throws TenantException {
        TenantConfiguration tconfig = new TenantConfiguration();
        final TenantContainer tenant = TenantContainer.create(tconfig);
        assertTrue(TenantState.STARTING == tenant.getState());
        tenant.run(() -> {
            assertTrue(TenantState.RUNNING == tenant.getState());
            System.out.println("testGetState: thread [" + Thread.currentThread().getName() + "] is running in tenant: "
                    + TenantContainer.current().getTenantId());
        });
        tenant.destroy();
    }

    public void testGetAttachedThreads() throws TenantException {
        TenantConfiguration tconfig = new TenantConfiguration();
        final TenantContainer tenant = TenantContainer.create(tconfig);
        tenant.run(() -> {
            Thread[] threads = tenant.getAttachedThreads();
            assertEquals(threads.length, 1);
            assertEquals(threads[0].getId(), Thread.currentThread().getId());
            System.out.println("testGetAttachedThreads: thread [" + Thread.currentThread().getName()
                    + "] is running in tenant: " + TenantContainer.current().getTenantId());
        });
        tenant.destroy();
    }

    public void testPropertyIsolation() throws TenantException {
        TenantConfiguration tconfig = new TenantConfiguration();
        final TenantContainer tenant = TenantContainer.create(tconfig);
        final TenantContainer tenant2 = TenantContainer.create(tconfig);

        final String key = "com.alibaba.tenant.test.prop";
        /*Verify the isolation for -Dcom.alibaba.tenant.test.prop=root*/
        //read it from root tenant.
        assertTrue("root".equals(System.getProperty(key)));

        Properties sysProps = System.getProperties();
        //test for setter/getter methods
        tenant.run(() -> {
            assertEquals(sysProps.size(), System.getProperties().size());
            for (Object tmpKey : sysProps.keySet()) {
                assertEquals(sysProps.get(tmpKey), System.getProperty((String) tmpKey));
            }

            assertTrue("root".equals(System.getProperty(key)));
            System.setProperty(key, TenantContainer.current().getName());
            assertTrue(TenantContainer.current().getName().equals(System.getProperty(key)));
        });

        tenant2.run(() -> {
            assertEquals(sysProps.size(), System.getProperties().size());
            for (Object tmpKey : sysProps.keySet()) {
                assertEquals(sysProps.get(tmpKey), System.getProperty((String) tmpKey));
            }

            assertTrue("root".equals(System.getProperty(key)));
            System.setProperty(key, TenantContainer.current().getName());
            assertTrue(TenantContainer.current().getName().equals(System.getProperty(key)));
        });
        //the value in root tenant should not be changed by any tenant.
        assertTrue("root".equals(System.getProperty(key)));

        //test for clearProperty
        tenant.run(() -> {
            String preVal = System.clearProperty(key);
            assertTrue(TenantContainer.current().getName().equals(preVal));

            String defValue = "zbc$#d";
            assertTrue(defValue.equals(System.getProperty(key, defValue)));
        });

        tenant2.run(() -> {
            String preVal = System.clearProperty(key);
            assertTrue(TenantContainer.current().getName().equals(preVal));

            String defValue = "123$#4";
            assertTrue(defValue.equals(System.getProperty(key, defValue)));
        });
        //the value in root tenant should not be changed by any tenant.
        assertTrue("root".equals(System.getProperty(key)));

        //test for setProperties
        tenant.run(() -> {
            Properties props = new Properties();
            String tmpKey = "tenant-id";
            String tmpVal = TenantContainer.current().getTenantId() + "";
            props.put(tmpKey, tmpVal);

            System.setProperties(props);

            assertTrue(tmpVal.equals(System.getProperty(tmpKey)));
        });

        tenant2.run(() -> {
            Properties props = new Properties();
            String tmpKey = "tenant-id";
            String tmpVal = TenantContainer.current().getTenantId() + "";
            props.put(tmpKey, tmpVal);
            System.setProperties(props);

            assertTrue(tmpVal.equals(System.getProperty(tmpKey)));
        });

        // test for System.getProperties
        tenant.run(() -> {
            System.setProperty("foo", "bar");
            Properties props = System.getProperties();
            assertTrue("bar".equals(props.getProperty("foo")));
        });

        //make sure we don't add any addition property in root tenant.
        assertTrue(null == System.getProperty("foo"));
        //the value in root tenant should not be changed by any tenant.
        assertTrue("root".equals(System.getProperty(key)));
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

    public void testTenantInheritance() throws TenantException {
        TenantConfiguration tconfig = new TenantConfiguration();
        final TenantContainer tenant = TenantContainer.create(tconfig);
        tenant.run(() -> {
            Thread thread = new Thread(() -> {
                TenantContainer tc = TenantContainer.current();
                assertSame(tc, tenant);
                Thread[] threads = tc.getAttachedThreads();
                assertTrue(threads.length == 2);
                assertTrue(Thread.currentThread().getId() == threads[0].getId() ||
                        Thread.currentThread().getId() == threads[1].getId());

                System.out.println("testTenantInheritance: thread [" + Thread.currentThread().getName()
                        + "] is running in tenant: " + TenantContainer.current().getTenantId());
            });

            thread.start();
            try {
                thread.join();
            } catch (Exception e) {
                e.printStackTrace();
                fail();
            }
            Thread[] threads = tenant.getAttachedThreads();
            assertEquals(threads.length, 1);
            assertEquals(threads[0].getId(), Thread.currentThread().getId());
        });
        tenant.destroy();
    }

    public void testDestroyTenantContainer() throws TenantException {
        int count = 20;
        TenantConfiguration tconfig = new TenantConfiguration();
        TenantContainer[] containers = new TenantContainer[count];

        IntStream.range(0, count).forEach(idx -> {
            try {
                containers[idx] = TenantContainer.create(tconfig);
                containers[idx].run(() -> new Object());
            } catch (TenantException e) {
                e.printStackTrace();
                fail();
            }
        });

        // try to get value of 'TenantContainer.allocationContext' filed via reflection
        Field allocationContextField = null;
        try {
            allocationContextField = TenantContainer.class.getDeclaredField("allocationContext");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            fail();
        }
        assertNotNull(allocationContextField);
        allocationContextField.setAccessible(true);

        /**
         * For now, what we are able to check is whether the pointer value
         * stored in 'allocationContext' jlong field equals to '0L', so far I
         * have not discovered an effective way to determine if certain memory
         * address is allocated correctly on the correct place of CHeap, or
         * reclaimed from CHeap.
         */

        //pre-destroy checking
        for (int i = 0; i < count; ++i) {
            try {
                long context = (Long) allocationContextField.get(containers[i]);

                if (TenantGlobals.isHeapIsolationEnabled()) {
                    // allocation context pointer should not be NULL if
                    // -XX:+TenantHeapThrottling is enabled
                    assertFalse(context == 0l);
                } else {
                    // allocation context pointer should be NULL if
                    // -XX:+TenantHeapThrottling is disabled (default)
                    assertEquals(context, 0l);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                fail();
            }
        }

        // explicitly destroy native resource
        Arrays.stream(containers).forEach(tenant -> tenant.destroy());

        // post-destroy checking
        for (int i = 0; i < count; ++i) {
            try {
                long context = (Long) allocationContextField.get(containers[i]);
                assertEquals(context, 0l); // should always be NULL after
                // destroy
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                fail();
            }
        }

        for (int i = 0; i < count; ++i) {
            containers[i].destroy();
        }
    }


    public static void main(String[] args) throws Exception {
        TestTenantContainer test = new TestTenantContainer();
        test.testCurrent();
        test.testGetTenantID();
        test.testGetState();
        test.testTenantSystemProperty();
        test.testPropertyIsolation();
        test.testGetAttachedThreads();
        test.testTenantInheritance();
        // TODO: enable below tests after porting TenantHeapIsolation
        // test.testDestroyTenantContainer();
    }
}
