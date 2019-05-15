
import com.alibaba.tenant.TenantConfiguration;
import com.alibaba.tenant.TenantContainer;
import com.alibaba.tenant.TenantException;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

import static jdk.test.lib.Asserts.*;

/*
 * @test
 * @summary Test JGroup
 * @library /test/lib
 * @build jdk.test.lib.tenant.JGroupMirror TestJGroup
 * @run main/othervm -XX:+MultiTenant -XX:+TenantCpuAccounting -XX:+TenantCpuThrottling
 *                   --add-opens java.base/com.alibaba.tenant=ALL-UNNAMED
 *                   --illegal-access=permit
 *                   -XX:+UseG1GC -Xmx200m -Xms200m -Dcom.alibaba.tenant.DebugJGroup=true
 *                   TestJGroup
 */

public class TestJGroup {

    private static CountDownLatch childRunning;

    private static class JGroupWorker implements Runnable{
        private long id;
        public long count = 0;
        public TenantContainer tenant;

        JGroupWorker(long id){
            this.id = id;
        }

        public void run(){
            tenant = TenantContainer.create(
                    new TenantConfiguration()
                            .limitCpuShares((int) id * 512 + 512)
                            .limitCpuSet("0")); // limit all tasks to one CPU to forcefully create contention
            try {
                tenant.run(()->{
                    childRunning.countDown();
                    long msPre = System.currentTimeMillis();
                    while(System.currentTimeMillis() - msPre < (20000 - 1000 * id)){
                        count++;
                    }
                });
            } catch (TenantException e) {
                e.printStackTrace();
                fail();
            }
        }

        public void destroy() {
            tenant.destroy();
        }
    }

    private static class JGroupsWorker implements Runnable{
        private TenantContainer tenants[];
        private long[] counts;

        JGroupsWorker(JGroupWorker ... workers){
            assertTrue(workers.length >= 3, "must be");
            tenants = new TenantContainer[workers.length];
            for (int i = 0; i < tenants.length; ++i) {
                tenants[i] = workers[i].tenant;
            }
            counts = new long[tenants.length];
        }

        public void run(){
            try {
                for (int i = 0; i < tenants.length; ++i) {
                    int idx = i;
                    tenants[i].run(() -> {
                        long msPre = System.currentTimeMillis();
                        while (System.currentTimeMillis() - msPre < (5000 - idx * 1000) /* 5000, 4000, 3000 */) {
                            counts[idx]++;
                        }
                    });
                }
            } catch (TenantException e) {
                e.printStackTrace();
                fail();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        int dimension = 3;
        long[] usages = new long[dimension];
        JGroupWorker[] jws = new JGroupWorker[dimension];
        Thread[] threads = new Thread[dimension];
        childRunning = new CountDownLatch(dimension);

        for (int i = 0; i < dimension; ++i) {
            usages[i] = 0;
            jws[i] = new JGroupWorker(i);
            threads[i] = new Thread(jws[i]);
        }

        Stream.of(threads).forEach(Thread::start);

        childRunning.await();

        JGroupsWorker js = new JGroupsWorker(jws);

        Thread t4 =new Thread(js);
        t4.start();
        try {
            t4.join();
        } catch (InterruptedException e) {
            System.out.println("Interreupted...");
            fail();
        }

        for (int i = 0; i < dimension; ++i) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                System.out.println("Interreupted...");
                fail();
            }
            usages[i] = jws[i].tenant.getProcessCpuTime();
            jws[i].destroy();
            assertTrue(usages[i] > 0, "Invalid cpu usage, usage[i]: " + usages[i]);
        }

        assertTrue(!((js.counts[2] < js.counts[1]) || (js.counts[1] < js.counts[0])),
                "com.alibaba.tenant.JGroupsWorker test failed!"
                                + " counts[0] = " + js.counts[0]
                                + " counts[1] = " + js.counts[1]
                                + " counts[2] = " + js.counts[2]);

        assertTrue(!((jws[2].count < jws[1].count) || (jws[1].count < jws[0].count)),
                "Three com.alibaba.tenant.JGroupWorker test failed!"
                                + " jws[0] count = " + jws[0].count
                                + " jws[1] count = " + jws[1].count
                                + " jws[2] count = " + jws[2].count);

        assertTrue(!((usages[2] < usages[1]) || (usages[1] < usages[0])),
                "Three com.alibaba.tenant.JGroupWorker test failed!"
                                + " jws[0] usage = " + usages[0]
                                + " jws[1] usage = " + usages[1]
                                + " jws[2] usage = " + usages[2]);
    }
}
