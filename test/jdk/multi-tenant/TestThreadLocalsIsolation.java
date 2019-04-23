/*
 * @test
 * @summary Test isolation of threadLocals
 * @library /test/lib
 * @run main/othervm -XX:+MultiTenant -XX:+UseG1GC -XX:+TenantDataIsolation TestThreadLocalsIsolation
 */

import static jdk.test.lib.Asserts.*;
import com.alibaba.tenant.TenantConfiguration;
import com.alibaba.tenant.TenantContainer;
import com.alibaba.tenant.TenantException;
import java.lang.ref.WeakReference;

public class TestThreadLocalsIsolation {

    public static void main(String[] args) throws Exception {
        testIsolation();
        testInheritableThreadLocals();
        testThreadObjectLeak();
    }

    private static void testIsolation() throws Exception {
        ThreadLocal<Integer> localInt = new ThreadLocal<Integer>() {
            @Override protected Integer initialValue() {
                return 0;
            }
        };

        assertEquals(0, localInt.get());
        localInt.set(1);
        assertEquals(1, localInt.get());

        TenantConfiguration config = new TenantConfiguration();
        TenantContainer tenant = TenantContainer.create(config);
        tenant.run(() -> {
            assertEquals(0, localInt.get());
            localInt.set(2);
            assertEquals(2, localInt.get());
        });

        assertEquals(1, localInt.get());
        localInt.set(3);
        assertEquals(3, localInt.get());

        tenant.run(() -> {
            assertEquals(2, localInt.get());
            localInt.set(4);
            assertEquals(4, localInt.get());
        });

        assertEquals(3, localInt.get());
    }

    private static void testInheritableThreadLocals() throws Exception {
        ThreadLocal<Integer> localInt = new InheritableThreadLocal<Integer>() {
            @Override
            protected Integer initialValue() {
                return 0;
            }
        };

        assertEquals(0, localInt.get());
        localInt.set(1);
        assertEquals(1, localInt.get());

        // ROOT tenant thread
        Thread t1 = new Thread(() -> {
            assertNull(TenantContainer.current());
            // thread in root tenant should inherit inheritibleThreadLocals
            assertEquals(1, localInt.get());
        });
        t1.start();
        t1.join();

        TenantConfiguration config = new TenantConfiguration();
        TenantContainer tenant = TenantContainer.create(config);
        tenant.run(() -> {
            // Tenant thread
            Thread t2 = new Thread(() -> {
                // Non-root tenant thread should not inherit any inheritibleThreadLocals
                assertEquals(1, localInt.get());
                localInt.set(2);
                assertEquals(2, localInt.get());

                assertNotNull(TenantContainer.current());
                TenantContainer.primitiveRunInRoot(() -> {
                    assertNull(TenantContainer.current());
                    // ROOT tenant thread
                    Thread t3 = new Thread(() -> {
                        assertNull(TenantContainer.current());
                        // running from non-root to root code, should not inherit any inheritibleThreadLocals
                        assertEquals(0, localInt.get());   // default initial value should be used
                    });
                    t3.start();
                    try {
                        t3.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        fail();
                    }
                });

                // Create non-root tenant thread from non-root tenant thread
                TenantContainer curTenant = TenantContainer.current();
                assertNotNull(curTenant);
                try {
                    curTenant.run(() -> {
                        Thread t4 = new Thread(() -> {
                            assertNotNull(TenantContainer.current());
                            assertTrue(TenantContainer.current() == curTenant);
                            assertEquals(2, localInt.get());
                        });
                        t4.start();
                        try {
                            t4.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            fail();
                        }
                    });
                } catch (TenantException e) {
                    e.printStackTrace();
                    fail();
                }
            });
            t2.start();
            try {
                t2.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
                fail();
            }
        });

        assertEquals(1, localInt.get());
        localInt.set(3);
        assertEquals(3, localInt.get());

        // check again
        tenant.run(() -> {
            assertEquals(0, localInt.get());
            localInt.set(4);
            assertEquals(4, localInt.get());
        });

        assertEquals(3, localInt.get());
    }

    private static final int M = 1024 * 1024;

    // TenantData reference should not prevent target Thread Object from being GCed
    private static void testThreadObjectLeak() {
        TenantConfiguration config = new TenantConfiguration();
        // strong ref: TenantContainer -> TenantData -> Thread
        TenantContainer tenant = TenantContainer.create(config);

        ThreadLocal<Integer> localInt = new ThreadLocal<Integer>() {
            @Override
            protected Integer initialValue() {
                return 0;
            }
        };

        // t is the new Tenant Thread who create a new entry in TenantData's map
        Thread t = new Thread(()-> {
            try {
                tenant.run(() -> {
                    assertEquals(0, localInt.get());
                    localInt.set(1);
                    assertEquals(1, localInt.get());
                });
            } catch (TenantException e) {
                fail();
            }
        });
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            fail();
        }
        assertEquals(0, localInt.get());

        // clear the strong ref from this method, tenantData should not hold any other strong references any more
        WeakReference<Thread> ref = new WeakReference<Thread>(t);
        assertNotNull(ref.get());
        t = null;
        System.gc();
        assertNull(ref.get());
    }
}
