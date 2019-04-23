
import com.alibaba.tenant.TenantContainer;
import com.alibaba.tenant.TenantException;
import com.alibaba.tenant.TenantConfiguration;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import static jdk.test.lib.Asserts.*;

/* @test
 * @summary test mask tenant thread shutdown when primitiveRunInRoot'
 * @library /test/lib
 * @compile TestMaskTenantShutdown.java
 * @run main/othervm -Dcom.alibaba.tenant.DebugTenantShutdown=true -Dcom.alibaba.tenant.ShutdownSTWSoftLimit=500
  *                              -XX:+MultiTenant -XX:+TenantThreadStop -Xmx600m -Xms200m TestMaskTenantShutdown
 */

public class TestMaskTenantShutdown {

    public static void main(String[] args) {
        TestMaskTenantShutdown test = new TestMaskTenantShutdown();
        test.testMaskThroughPrimitiveRunInRoot();
        test.testMaskThroughAPIs();
    }

    private void testMaskThroughPrimitiveRunInRoot() {
        BiConsumer<TenantContainer, Runnable> primiTask = (tenant, task)->{
            assertEquals(tenant, TenantContainer.current());
            tenant.primitiveRunInRoot(task);
        };
        checkMaskShutDownApproach(2, primiTask);
        checkMaskShutDownApproach(Runtime.getRuntime().availableProcessors(),
                primiTask);
    }

    private void testMaskThroughAPIs() {
        BiConsumer<TenantContainer, Runnable> primiTask = (tenant, task)->{
            assertEquals(tenant, TenantContainer.current());
            tenant.maskTenantShutdown();
            try {
                assertEquals(tenant, TenantContainer.current());
                task.run();
            } finally {
                tenant.unmaskTenantShutdown();
            }
        };
        checkMaskShutDownApproach(2, primiTask);
        checkMaskShutDownApproach(Runtime.getRuntime().availableProcessors(),
                primiTask);
    }

    /*
     * Represents a piece of code, it has the ability to check if the code execution has been
     * terminated early before.
     */
    private class UninterruptibleTask implements Runnable {
        private CountDownLatch blocker;
        private CountDownLatch startMarker;
        private CountDownLatch endMarker;
        private AtomicBoolean finished;

        // the flag is used to check if operation has successfully finished!
        private int flag;
        private static final int INTERRUPTED_VAL = 0x10;
        private static final int UNINTERRUPTED_VAL = 0x20;

        UninterruptibleTask(CountDownLatch blocker, CountDownLatch starter, CountDownLatch ender) {
            this.blocker = blocker;
            this.startMarker = starter;
            this.endMarker = ender;
            flag = INTERRUPTED_VAL;
            finished = new AtomicBoolean(false);
        }

        @Override public void run() {
            startMarker.countDown();
            try {
                // wait for TenantContainer.destroy to happen
                blocker.await();
                System.out.println("thread1 alive");
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } finally {
                flag = UNINTERRUPTED_VAL;
                finished.set(true);
                endMarker.countDown();
            }
        }

        void assertUninterrupted() {
            if (finished.get()) {
                assertEquals(flag, UNINTERRUPTED_VAL, "Operation was interrupted! please check!");
            }
        }
    }

    /**
     * Inplementation class to examine if a piece of code has been interrupted by {@code TenantContainer.destroy}
     * @param paraLevel         How many children threads to be started
     * @param maskApproach      The mechanism to do masking
     */
    private void checkMaskShutDownApproach(int paraLevel,
                                           BiConsumer<TenantContainer, Runnable> maskApproach) {
        TenantContainer tenant = TenantContainer.create(new TenantConfiguration());
        CountDownLatch startCounter = new CountDownLatch(paraLevel);
        CountDownLatch endCounter = new CountDownLatch(paraLevel);
        CountDownLatch blockers[] = new CountDownLatch[paraLevel];
        Thread threads[] = new Thread[paraLevel];
        UninterruptibleTask tasks[] = new UninterruptibleTask[paraLevel];

        // initialization
        for (int i = 0; i < paraLevel; ++i) {
            blockers[i] = new CountDownLatch(1);
            tasks[i] = new UninterruptibleTask(blockers[i], startCounter, endCounter);
        }

        // spawn threads and test shutdown interruption
        try {
            tenant.run(() -> {
                for (int i = 0; i < paraLevel; ++i) {
                    int idx = i;
                    threads[i] = new Thread(() -> {
                        System.out.println(Thread.currentThread().getName() + " started!");
                        try {
                            maskApproach.accept(tenant, tasks[idx]);
                        } finally {
                            System.out.println(Thread.currentThread().getName() + " finished!");
                        }
                    });
                    threads[i].start();
                }
            });

            // wait for all children to be started
            startCounter.await();

            tenant.destroy();

            Arrays.stream(blockers)
                    .forEach(CountDownLatch::countDown);

            endCounter.await(5, TimeUnit.SECONDS);

        } catch (TenantException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            // wait for all children threads to terminate
            Arrays.stream(threads)
                    .forEach(t-> {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        Arrays.stream(tasks)
                .forEach(UninterruptibleTask::assertUninterrupted);
    }
}
