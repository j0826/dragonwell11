/*
 * @test
 * @summary Test destroying tenant while tenant threads doing <clinit>
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm/timeout=100 -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+IgnoreUnrecognizedVMOptions
 *                -XX:+MultiTenant -XX:+TenantThreadStop TestClinitWhileTenantDestroy
 *
 */

import com.alibaba.tenant.TenantConfiguration;
import com.alibaba.tenant.TenantContainer;
import com.alibaba.tenant.TenantException;
import java.util.concurrent.CountDownLatch;
import static jdk.test.lib.Asserts.*;

class MyClass {
    static {
        System.err.println("MyClass::<clinit>");
        TestClinitWhileTenantDestroy.cdl.countDown();
        long startMillis = System.currentTimeMillis();

        // wait 3 seconds for TenantContainer.destory() to happen right during this <clinit>
        // during that period, no external Thread.interrupt() should be called!
        try {
            Thread.sleep(3_000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
            // should not be interrupted!
            TestClinitWhileTenantDestroy.fail();
        }
    }
}

public class TestClinitWhileTenantDestroy {

    static CountDownLatch cdl = new CountDownLatch(1);

    public static void main(String[] args) {
        TenantConfiguration config = new TenantConfiguration(1024, 64 * 1024 * 1024);
        TenantContainer tenant = TenantContainer.create(config);
        Thread[] initializerThread = new Thread[1];

        try {
            tenant.run(()->{
                initializerThread[0] = new Thread(()->{
                    try {
                        Class cls = Class.forName("MyClass");
                        assertNotNull(cls);
                        System.err.println("1st Class.forName, returns " + cls);

                        Object o = cls.newInstance();
                        assertNotNull(o);
                        System.err.println("1st Class.newInstance, returns " + o);
                    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                        e.printStackTrace();
                        fail();
                    }
                });
                initializerThread[0].start();
            });
        } catch (TenantException e) {
            e.printStackTrace();
            fail();
        }

        // wait MyClass.<clinit> to begin
        try {
            cdl.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail();
        }

        tenant.destroy(); // <clinit> of MyClass has been interrupted forcefully.

        assertTrue(!initializerThread[0].isAlive());
        try {
            initializerThread[0].join();
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail();
        }
        assertTrue(initializerThread[0].getState() == Thread.State.TERMINATED);

        // Now trying to get MyClass again from ROOT tenant
        try {
            Class myClazz = Class.forName("MyClass");
            assertNotNull(myClazz);
            System.err.println("1st Class.forName again, returns " + myClazz);

            Object o = myClazz.newInstance();
            assertNotNull(o);
            System.err.println("2st Class.newInstance again, returns " + o);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            fail();
        }
    }

    static void fail() {
        assertTrue(false);
    }
}
