
/*
 * @test
 * @summary Test to kill thread which doing tenant masking
 * @library /test/lib
 * @run main/othervm/timeout=100 -Dcom.alibaba.tenant.ShutdownSTWSoftLimit=5000 -Xbootclasspath/a:.
 *                               -XX:+UnlockDiagnosticVMOptions -XX:+IgnoreUnrecognizedVMOptions -XX:+TraceTenantKillThreads
 *                               -XX:+MultiTenant -XX:+TenantThreadStop -XX:+WhiteBoxAPI TestTenantShutdownMaskLevel
 * @run main/othervm/timeout=100 -Dcom.alibaba.tenant.ShutdownSTWSoftLimit=5000 -Xbootclasspath/a:.
 *                               -XX:+UnlockDiagnosticVMOptions -XX:+IgnoreUnrecognizedVMOptions -XX:+TraceTenantKillThreads
 *                               -XX:+MultiTenant -XX:+TenantThreadStop -XX:+WhiteBoxAPI  -XX:-UseBiasedLocking
 *                               -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.transparentAsync=true TestTenantShutdownMaskLevel
 *
 */
import static com.alibaba.tenant.TenantContainer.*;
import static jdk.test.lib.Asserts.*;
import com.alibaba.tenant.TenantConfiguration;
import com.alibaba.tenant.TenantContainer;
import com.alibaba.tenant.TenantException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

//
// The test will only reproduce bug 16915137 with very low chance
//
public class TestTenantShutdownMaskLevel {

    public static void main(String[] args) {
        TestTenantShutdownMaskLevel test = new TestTenantShutdownMaskLevel();
        test.testKillEmbededMasking();
    }

    private void testKillEmbededMasking() {

        TenantContainer tenant = TenantContainer.create(new TenantConfiguration());

        Thread[] victims = new Thread[Runtime.getRuntime().availableProcessors() << 2];
        CountDownLatch allStarted = new CountDownLatch(victims.length);

        try {
            tenant.run(()->{
                for (int i = 0; i < victims.length; ++i) {
                    victims[i] = new Thread(()->{
                        AtomicInteger cnt = new AtomicInteger(0); // just need a mutable object for lambda
                        while (true) {
                            assertEquals(cnt.get(), 0);
                            wearMaskOrQuit(()->{
                                if (cnt.get() > 20) {
                                    runOnce(allStarted::countDown);
                                    cnt.set(0);
                                    return false;
                                } else {
                                    cnt.incrementAndGet();
                                    return true;
                                }
                            });
                        }
                    });
                    victims[i].start();
                }
            });

            allStarted.await();

            Thread.sleep(1000);

            tenant.destroy();

            // there should be no threads left attached
            Thread[] attachedThreads = tenant.getAttachedThreads();

            assertEquals(attachedThreads.length, 0);

        } catch (TenantException | InterruptedException e) {
            e.printStackTrace();
            fail();
        }
    }

    // execute a task on each thread for exactly once
    private static void runOnce(Runnable task) {
        ThreadLocal<Boolean> executed = ThreadLocal.withInitial(()->false);
        if (!executed.get()) {
            executed.set(true);
            task.run();
        }
    }

    private static void wearMaskOrQuit(BooleanSupplier shouldMask) {
        maskTenantShutdown();
        try {
            if (shouldMask.getAsBoolean()) {
                wearMaskOrQuit(shouldMask);
            }
        } finally {
            unmaskTenantShutdown();
        }
    }
}
