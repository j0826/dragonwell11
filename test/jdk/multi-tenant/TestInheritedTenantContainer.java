
/*
 * @test
 * @summary Test policy for children threads to inherit parent's TenantContainer
 * @library /test/lib
 * @run main/othervm -XX:+MultiTenant --illegal-access=permit --add-opens java.base/com.alibaba.tenant=ALL-UNNAMED
 *                                    TestInheritedTenantContainer true true
 *
 * @run main/othervm -XX:+MultiTenant -Dcom.alibaba.tenant.threadInheritance=false
 *                                    --illegal-access=permit --add-opens java.base/com.alibaba.tenant=ALL-UNNAMED
 *                                    TestInheritedTenantContainer false true
 *
 * @run main/othervm -XX:+MultiTenant -Dcom.alibaba.tenant.allowPerThreadInheritance=false
 *                                    --illegal-access=permit --add-opens java.base/com.alibaba.tenant=ALL-UNNAMED
 *                                    TestInheritedTenantContainer true false
 *
 * @run main/othervm -XX:+MultiTenant -Dcom.alibaba.tenant.threadInheritance=false -Dcom.alibaba.tenant.allowPerThreadInheritance=false
 *                                    --illegal-access=permit --add-opens java.base/com.alibaba.tenant=ALL-UNNAMED
 *                                    TestInheritedTenantContainer false false
 *
 */

import com.alibaba.tenant.TenantConfiguration;
import com.alibaba.tenant.TenantContainer;
import com.alibaba.tenant.TenantException;
import com.alibaba.tenant.TenantGlobals;

import java.lang.reflect.Field;
import static jdk.test.lib.Asserts.*;

public class TestInheritedTenantContainer {

    private static final boolean SHOULD_INHERIT;
    private static final boolean ALLOW_PER_THREAD_INHERIT;

    static {
        try {
            Class tcClass = TenantConfiguration.class;
            Field f = tcClass.getDeclaredField("THREAD_INHERITANCE");
            f.setAccessible(true);
            SHOULD_INHERIT = f.getBoolean(null);
            f = tcClass.getDeclaredField("ALLOW_PER_THREAD_INHERITANCE");
            f.setAccessible(true);
            ALLOW_PER_THREAD_INHERIT = f.getBoolean(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        assertTrue(TenantGlobals.isTenantEnabled());
        assertEquals(args.length, 2);
        final boolean paraInheritDefault = Boolean.parseBoolean(args[0]);
        final boolean paraPerThread = Boolean.parseBoolean(args[1]);
        assertEquals(paraInheritDefault, SHOULD_INHERIT);
        assertEquals(paraPerThread, ALLOW_PER_THREAD_INHERIT);

        TenantContainer tenant = TenantContainer.create(new TenantConfiguration());

        Runnable defaultPolicyTask = ()->{
            TenantContainer childTenant = TenantContainer.current();
            if (SHOULD_INHERIT) {
                assertEquals(tenant, childTenant, "Should be same tenant");
            } else {
                assertNull(childTenant);
            }
        };

        Runnable nonDefaultPolicyTask = ()->{
            TenantContainer childTenant = TenantContainer.current();

            if (ALLOW_PER_THREAD_INHERIT) {
                if (!SHOULD_INHERIT) {
                    assertEquals(tenant, childTenant, "Should be same tenant");
                } else {
                    assertNull(childTenant);
                }
            } else {
                if (SHOULD_INHERIT) {
                    assertEquals(tenant, childTenant, "Should be same tenant");
                } else {
                    assertNull(childTenant);
                }

                // default policy should not be changed if does not allow per-thread changing
                Thread thr = new Thread(defaultPolicyTask);
                thr.start();
                try { thr.join(); } catch (InterruptedException e) { fail(); }
            }
        };

        try {
            tenant.run(()->{
                Thread thr = new Thread(()->{
                    if (SHOULD_INHERIT) {
                        assertEquals(TenantContainer.current(), tenant);
                    } else {
                        assertNotEquals(TenantContainer.current(), tenant);
                    }
                    defaultPolicyTask.run();

                    Thread thrInner = new Thread(defaultPolicyTask);
                    thrInner.start();
                    try { thrInner.join(); } catch (InterruptedException e) { fail(); }
                });
                thr.start();
                try { thr.join(); } catch (InterruptedException e) { fail(); }

                // changed to reverted policy
                TenantContainer.setCurrentThreadInheritance(!SHOULD_INHERIT);

                thr = new Thread(nonDefaultPolicyTask);
                thr.start();
                try { thr.join(); } catch (InterruptedException e) { fail(); }

                // try to restore, should be same as initial state
                TenantContainer.setCurrentThreadInheritance(SHOULD_INHERIT);
                thr = new Thread(()->{
                    assertTrue(SHOULD_INHERIT
                            ? TenantContainer.current() == tenant : TenantContainer.current() != tenant);
                    defaultPolicyTask.run();

                    Thread thrInner = new Thread(defaultPolicyTask);
                    thrInner.start();
                    try { thrInner.join(); } catch (InterruptedException e) { fail(); }
                });
                thr.start();
                try { thr.join(); } catch (InterruptedException e) { fail(); }

            });
        } catch (TenantException e) {
            e.printStackTrace();
            fail();
        }
    }
}
