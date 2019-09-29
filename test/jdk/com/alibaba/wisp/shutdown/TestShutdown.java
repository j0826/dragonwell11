/*
 * @test
 * @summary TestShutdown
 * @library /lib/testlibrary
 * @run main/othervm  -XX:+MultiTenant -XX:+TenantDataIsolation -XX:+TenantThreadStop -XX:+UseG1GC -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true TestShutdown
 */

import com.alibaba.wisp.engine.WispEngine;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static jdk.testlibrary.Asserts.assertEQ;

public class TestShutdown {
    public static void main(String[] args) throws Exception {
        CountDownLatch poison = new CountDownLatch(1);
        AtomicReference<WispEngine> engine = new AtomicReference<>();

        AtomicInteger n = new AtomicInteger();

        new Thread(() -> {
            engine.set(WispEngine.current());
            try {
                poison.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "the_engine").start();

        while (engine.get() == null) continue;

        for (int i = 0; i < 78; i++) {
            engine.get().execute(() -> {
                n.incrementAndGet();
                try {
                    sleep(1000000);
                } finally {
                    n.decrementAndGet();
                }
            });
        }

        while (n.get() != 78) continue;

        long start = System.nanoTime();

        engine.get().shutdown();
        poison.countDown();
        engine.get().awaitTermination(10, TimeUnit.SECONDS);

        System.out.println(System.nanoTime() - start);

        assertEQ(n.get(), 0);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
