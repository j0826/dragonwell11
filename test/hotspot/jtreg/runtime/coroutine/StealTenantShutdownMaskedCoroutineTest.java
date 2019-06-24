/*
 * @test
 * @summary test stealing shutdown masked coroutine does not cause crash.
 * @run main/othervm -XX:+EnableCoroutine  -XX:+MultiTenant -XX:+TenantDataIsolation -XX:+TenantThreadStop -XX:+UseG1GC StealTenantShutdownMaskedCoroutineTest
 */

import com.alibaba.tenant.TenantContainer;

import java.dyn.Coroutine;
import java.util.concurrent.CountDownLatch;

public class StealTenantShutdownMaskedCoroutineTest {

    public static void main(String[] args) throws Exception {
        Coroutine[] co = new Coroutine[1];
        CountDownLatch latch = new CountDownLatch(1);

        new Thread(() -> {
            co[0] = new Coroutine(() -> {
                TenantContainer.maskTenantShutdown();
                Coroutine.yield();
                TenantContainer.unmaskTenantShutdown();
            });

            Coroutine.yieldTo(co[0]);
            latch.countDown();
            try { // avoid exit
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();


        latch.await();
        co[0].steal(false);
        Coroutine.yieldTo(co[0]);
        // before fix:
        // corotuine call unmaskTenantShutdown(), then crash
    }

}

