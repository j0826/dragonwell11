/*
 * @test
 * @summary Test function of killing tenants' threads with Wisp
 * @library /test/lib
 * @run main/othervm/timeout=20 -XX:+MultiTenant -XX:+TenantThreadStop -Dcom.alibaba.tenant.KillThreadInterval=1000 -XX:+TraceTenantThreadStop
 *                              -XX:+EnableCoroutine -XX:-UseBiasedLocking -XX:+UseWispMonitor -Dcom.alibaba.transparentAsync=true -Dcom.alibaba.wisp.version=2 -Dcom.alibaba.wisp.enableHandOff=false
 *                              TestKillThreadWithWisp
 */

import static jdk.test.lib.Asserts.*;
import com.alibaba.tenant.TenantConfiguration;
import com.alibaba.tenant.TenantContainer;
import com.alibaba.wisp.engine.Wisp2Group;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TestKillThreadWithWisp {

    private static final int THREADS_NUM_IN_GROUP = 4;
    private static final int TASKS_NUM = 1000;

    private static void awaitLatch(CountDownLatch cdl) {
        try {
            cdl.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void sleepInfinitely() {
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void fail() {
        assertTrue(false, "Failed!");
    }

    public static void main(String[] args) {

        TenantConfiguration config = new TenantConfiguration();
        TenantContainer tenant = TenantContainer.create(config);
        try {
            tenant.run(() -> {
                CountDownLatch cdl = new CountDownLatch(TASKS_NUM);
                Wisp2Group delegated = Wisp2Group.createGroup(THREADS_NUM_IN_GROUP, new ThreadFactory() {
                    AtomicInteger seq = new AtomicInteger();
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "Wisp2-Group-Test-Carrier-" + seq.getAndIncrement());
                        t.setDaemon(true);
                        return t;
                    }
                });
                for (int i = 0; i < TASKS_NUM; i++) {
                    delegated.execute(() -> {
                        cdl.countDown();
                        sleepInfinitely();
                    });
                }
                awaitLatch(cdl);
                delegated.shutdown();
                try {
                    boolean res = delegated.awaitTermination(60, TimeUnit.SECONDS);
                    assertTrue(res);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    fail();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }

        System.out.println("succeeded!");
        tenant.destroy();

    }


}
