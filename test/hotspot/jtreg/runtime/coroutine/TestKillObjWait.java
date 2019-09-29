/*
 * @test
 * @summary TestKillObjWait
 * @run main/timeout=30/othervm  -XX:+MultiTenant -XX:+TenantDataIsolation -XX:+TenantThreadStop -XX:+UseG1GC -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true TestKillObjWait
 * @run main/timeout=30/othervm  -XX:+MultiTenant -XX:+TenantDataIsolation -XX:+TenantThreadStop -XX:+UseG1GC -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.wisp.version=2 TestKillObjWait
 */

import com.alibaba.tenant.TenantConfiguration;
import com.alibaba.tenant.TenantContainer;

public class TestKillObjWait {
    // TestKillObjWait will hang infinitely without patch D651277
    public static void main(String[] args) throws Exception {
        TenantContainer tenant = TenantContainer.create(new TenantConfiguration());
        Object lock = new Object();
        tenant.run(() -> new Thread(() -> {
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                }
            }
        }).start());

        Thread.sleep(100);
        tenant.destroy();
    }
}
