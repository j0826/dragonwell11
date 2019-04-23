import static jdk.test.lib.Asserts.*;
import java.util.concurrent.atomic.AtomicBoolean;
import com.alibaba.tenant.TenantConfiguration;
import com.alibaba.tenant.TenantContainer;
import com.alibaba.tenant.TenantException;
import com.alibaba.tenant.TenantState;

/* @test
 * @summary shutdown hook related unit tests
 * @library /test/lib
 * @compile TestShutdownHooks.java
 * @run main/othervm -XX:+MultiTenant -XX:+TenantDataIsolation -XX:+UseG1GC -Xmx600m -Xms600m TestShutdownHooks
 *
 */
public class TestShutdownHooks {

    interface IShutdownHooks {
        void    addShutdownHook(Thread hook);
        boolean removeShutdownHook(Thread hook);
    }

    private void verifyShutdownHook(IShutdownHooks hooks) throws TenantException {
        final AtomicBoolean  result  = new AtomicBoolean(false);
        TenantConfiguration tconfig  = new TenantConfiguration();
        final TenantContainer tenant = TenantContainer.create(tconfig);
        //Verify the the shutdown hook was called after tenant destroy.
        tenant.run(() -> {
            Thread hook = new Thread(()->{
                assertSame(TenantContainer.current(), tenant);
                //indicate the hook was called successfully.
                result.set(true);
            });
            assertTrue(tenant == TenantContainer.current());
            hooks.addShutdownHook(hook);
        });
        tenant.destroy();
        assertTrue(result.get());
        assertTrue(tenant.getState() == TenantState.DEAD);

        /**
         *Verify the hook isolation
         * (1) the tenant can't see the hook added in root.
         *
         */
        Thread rootHook = new Thread(()->{
            assertSame(TenantContainer.current(), null);
        });
        hooks.addShutdownHook(rootHook);

        final TenantContainer tenant2 = TenantContainer.create(tconfig);
        tenant2.run(() -> {
            assertFalse(hooks.removeShutdownHook(rootHook));
        });

        /**
         * (2) the root can't see the hook added in tenant.
         */
        final Thread[]  tenantHook = new Thread[1];
        tenant2.run(() -> {
            tenantHook[0] = new Thread(()->{
                assertSame(TenantContainer.current(), tenant2);
            });
            hooks.addShutdownHook(tenantHook[0]);
        });
        assertTrue(tenantHook[0] != null);
        // TODO: enable this after porting TenantHeapIsolation
        // assertSame(TenantContainer.containerOf(tenantHook[0]), tenant2);
        assertFalse(hooks.removeShutdownHook(tenantHook[0]));

        //can remove tenantHook in tenant2 successfully
        tenant2.run(() -> {
            assertTrue(hooks.removeShutdownHook(tenantHook[0]));
        });

        //destroy tenant2 whose hook list is empty.
        tenant2.destroy();
    }

    public void testRuntimeShutdownHook() throws TenantException {
        verifyShutdownHook(new IShutdownHooks() {
            @Override
            public void addShutdownHook(Thread hook) {
                Runtime.getRuntime().addShutdownHook(hook);
            }
            @Override
            public boolean removeShutdownHook(Thread hook) {
                return Runtime.getRuntime().removeShutdownHook(hook);
            }
        });
    }

    public void testTenantShutdownHook() throws TenantException {
        verifyShutdownHook(new IShutdownHooks() {
            @Override
            public void addShutdownHook(Thread hook) {
                if(null == TenantContainer.current()) {
                    Runtime.getRuntime().addShutdownHook(hook);
                } else {
                    TenantContainer.current().addShutdownHook(hook);
                }
            }
            @Override
            public boolean removeShutdownHook(Thread hook) {
                if(null == TenantContainer.current()) {
                    return Runtime.getRuntime().removeShutdownHook(hook);
                } else {
                    return TenantContainer.current().removeShutdownHook(hook);
                }
            }
        });
    }

    public void testShutdownHook() throws TenantException {
        verifyShutdownHook(new IShutdownHooks() {
            @Override
            public void addShutdownHook(Thread hook) {
               Runtime.getRuntime().addShutdownHook(hook);
            }
            @Override
            public boolean removeShutdownHook(Thread hook) {
                if(null == TenantContainer.current()) {
                    return Runtime.getRuntime().removeShutdownHook(hook);
                } else {
                    return TenantContainer.current().removeShutdownHook(hook);
                }
            }
        });
    }

    public void testShutdownHook2() throws TenantException {
        verifyShutdownHook(new IShutdownHooks() {
            @Override
            public void addShutdownHook(Thread hook) {
                if(null == TenantContainer.current()) {
                    Runtime.getRuntime().addShutdownHook(hook);
                } else {
                    TenantContainer.current().addShutdownHook(hook);
                }
            }
            @Override
            public boolean removeShutdownHook(Thread hook) {
                return Runtime.getRuntime().removeShutdownHook(hook);
            }
        });
    }

    public static void main(String[] args) {
        TestShutdownHooks test = new TestShutdownHooks();
        try {
            test.testRuntimeShutdownHook();
            test.testTenantShutdownHook();
            test.testShutdownHook();
            test.testShutdownHook2();
        } catch (Throwable t) {
            System.out.println("Failed in runnig TestShutdownHooks test.");
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }
}
