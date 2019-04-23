/*
 * @test
 * @library /test/lib
 * @summary Test finalizer of objects in non-root tenants
 * @run main/othervm -XX:+MultiTenant -XX:+TenantDataIsolation -XX:+UseG1GC TestTenantFinalizer
 *
 */

import static jdk.test.lib.Asserts.*;
import com.alibaba.tenant.TenantConfiguration;
import com.alibaba.tenant.TenantContainer;
import com.alibaba.tenant.TenantException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class TestTenantFinalizer {

    public static void main(String[] args) throws TenantException, InterruptedException {
        TestTenantFinalizer test = new TestTenantFinalizer();
        test.testFinalizerBasic();
    }

    public void testFinalizerBasic() throws TenantException, InterruptedException {
        TenantConfiguration config = new TenantConfiguration();
        TenantContainer tenant = TenantContainer.create(config);
        AtomicBoolean testFinalized = new AtomicBoolean(false);
        CountDownLatch countDown = new CountDownLatch(2);

        Thread[] finalizers = new Thread[2];

        // trying to trigger finalize
        tenant.run(()-> {
            new Object() {
                protected void finalize() {
                    try {
                        System.err.println("Finalizer 1 thread:" + Thread.currentThread());
                        finalizers[1] = Thread.currentThread();
                        assertNotNull(TenantContainer.current());
                        assertFalse(testFinalized.get());
                        testFinalized.set(true);
                    } catch (Throwable t) {
                        t.printStackTrace();
                        fail();
                    } finally {
                        countDown.countDown();
                    }
                }
            };
        });
        // root tenant's finalizer should still be executed in root tenant;
        new Object() {
            protected  void finalize() {
                System.err.println("Finalizer 2 thread:" + Thread.currentThread());
                finalizers[0] = Thread.currentThread();
                try {
                    assertNull(TenantContainer.current());
                } catch (Throwable t) {
                    t.printStackTrace();
                    fail();
                } finally {
                    countDown.countDown();
                }
            }
        };
        System.gc();

        // wait for full GC operation to finish
        countDown.await(10, TimeUnit.SECONDS);
        assertTrue(0 == countDown.getCount());

        // finalizer should have been executed!
        tenant.destroy();

        assertTrue(testFinalized.get());
        assertNotNull(finalizers[0]);
        assertNotNull(finalizers[1]);
        assertNotEquals(finalizers[0], finalizers[1]);

    }

    private static void fail() {
        assertTrue(false);
    }
}
