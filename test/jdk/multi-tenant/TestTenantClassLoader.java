
/* @test
 * @summary test TenantContainer-ClassLoader associations
 * @library /test/lib
 * @run main/othervm --illegal-access=permit --add-opens java.base/com.alibaba.tenant=ALL-UNNAMED
 *                   --add-opens java.base/jdk.internal.loader=ALL-UNNAMED
 *                   -XX:+MultiTenant -XX:+TenantThreadStop TestTenantClassLoader
 */

import static jdk.test.lib.Asserts.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import com.alibaba.tenant.TenantConfiguration;
import com.alibaba.tenant.TenantContainer;
import com.alibaba.tenant.TenantException;
import com.alibaba.tenant.TenantState;

//
// NOTE: This class depends on implementation of some JDK classes, such as ClassLoader.
//
public class TestTenantClassLoader {

    private static final long M = 1024 * 1024;

    // ensure correct status of system classloaders, which should not be touched by MT changes (by now).
    private void testVerifySystemClassLoaders() {
        Class loaders = null;
        try {
            loaders = Class.forName("jdk.internal.loader.ClassLoaders");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            fail();
        }
        FieldAccessor<ClassLoader> lAcc = new FieldAccessor<>(null, loaders);

        ClassLoader cl = lAcc.getStaticField("APP_LOADER"); // app classloader
        assert (cl != null);
        FieldAccessor<ClassLoader> accessor = new FieldAccessor<>(cl, ClassLoader.class);
        boolean isDead = accessor.getField("isDead");
        assertFalse(isDead);

        cl = cl.getParent(); // extension classloader is parent of app classloader
        assert (cl != null);
        accessor = new FieldAccessor<>(cl, ClassLoader.class);
        isDead = accessor.getField("isDead");
        assertFalse(isDead);
    }

    // Basic testing, just to verify status of dead classloader, etc.
    private void testBasic() throws TenantException {
        TenantConfiguration config = new TenantConfiguration();
        TenantContainer tenant = TenantContainer.create(config);
        FieldAccessor<TenantContainer> accessor = new FieldAccessor(tenant, TenantContainer.class);

        Map<ClassLoader, ClassLoader> tenantLoaders = accessor.getField("tenantLoaders");
        assertTrue(tenantLoaders != null
                    && tenantLoaders.isEmpty());

        ClassLoader[] loaderRefs = new ClassLoader[1];
        tenant.run(() -> {
            try {
                URL[] urls = {
                        new File(".").toPath().toUri().toURL()
                };
                ClassLoader loader = URLClassLoader.newInstance(urls);
                loaderRefs[0] = loader;
                FieldAccessor<ClassLoader> clAcc = new FieldAccessor<>(loader, ClassLoader.class);
                boolean isDead = clAcc.getField("isDead");
                assertFalse(isDead);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                fail();
            }
        });

        assertNotNull(loaderRefs[0]);
        assertTrue(tenantLoaders != null
                    && !tenantLoaders.isEmpty());
        assertTrue(tenantLoaders.keySet().contains(loaderRefs[0]));

        // destroy the tenant
        tenant.destroy();

        assertTrue(tenant.getState() == TenantState.DEAD);
        FieldAccessor<ClassLoader> clAcc = new FieldAccessor<>(loaderRefs[0], ClassLoader.class);
        boolean isDead = clAcc.getField("isDead");
        assertTrue(isDead);
    }

    private void testUsingDeadClassLoader() throws TenantException {
        TenantConfiguration config = new TenantConfiguration();
        TenantContainer tenant = TenantContainer.create(config);

        ClassLoader[] loaderRefs = new ClassLoader[1];
        tenant.run(() -> {
            try {
                URL[] urls = {
                        new File(".").toPath().toUri().toURL()
                };
                ClassLoader loader = URLClassLoader.newInstance(urls);
                loaderRefs[0] = loader;
            } catch (MalformedURLException e) {
                e.printStackTrace();
                fail();
            }
        });


        // call various methods of a dead classloader
        final ClassLoader testedLoader = loaderRefs[0];
        assertNotNull(testedLoader);
        Runnable[] tasks = {
                () -> {
                    try {
                        testedLoader.loadClass("NeverMind");
                    } catch (ClassNotFoundException e) {
                        // ignore
                    }
                },
                () -> testedLoader.getResource("123"),
                () -> testedLoader.getResourceAsStream("456"),
                () -> {
                    try {
                        testedLoader.getResources("123");
                    } catch (IOException e) {
                        // ignore
                    }
                },
        };

        System.out.println("Before TenantContainer.destroy():");
        for (int i = 0; i < tasks.length; ++i) {
            System.out.println("invoking method [" + i + "] of live tenant classloader");
            try {
                tasks[i].run();
                fail();
            } catch (Throwable e) {
                // expect IllegalStateException from a DEAD classloader
                if (e instanceof IllegalStateException) {
                    e.printStackTrace();
                    fail();
                }
            }
        }

        tenant.destroy();
        assertTrue(tenant.getState() == TenantState.DEAD);

        System.out.println("After    TenantContainer.destroy():");
        for (int i = 0; i < tasks.length; ++i) {
            System.out.println("invoking method [" + i + "] of dead tenant classloader");
            try {
                tasks[i].run();
                fail();
            } catch (Throwable e) {
                // expect IllegalStateException from a DEAD classloader
                if (!(e instanceof IllegalStateException)) {
                    e.printStackTrace();
                    fail();
                }
            }
        }
    }

    public static void main(String[] args) throws TenantException {
        TestTenantClassLoader test = new TestTenantClassLoader();
        for (Method m : test.getClass().getDeclaredMethods()) {
            String mName = m.getName();
            if (mName.startsWith("test")) {
                System.out.println("Running test: " + test.getClass().getCanonicalName() + "." + mName);
                m.setAccessible(true);
                try {
                    m.invoke(test);
                    System.out.println("[PASSED]");
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                    fail();
                }
            }
        }
    }

    private static void fail() {
        assertTrue(false);
    }

    // utility class to access private data members of a object using reflection
    private class FieldAccessor<K> {
        private K receiver;
        protected Class klass;

        FieldAccessor(K receiver, Class klass) {
            this.receiver = receiver;
            this.klass = klass;
        }

        public <T> T getField(String field) {
            if (field != null && receiver != null && klass != null) {
                try {
                    Field f = klass.getDeclaredField(field);
                    f.setAccessible(true);
                    return (T)f.get(receiver);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    e.printStackTrace();
                    fail();
                }
            }
            return null;
        }

        public <T> T getStaticField(String staticFieldName) {
            if (staticFieldName != null && klass != null) {
                try {
                    Field f = klass.getDeclaredField(staticFieldName);
                    f.setAccessible(true);
                    return (T) f.get(null);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    e.printStackTrace();
                    fail();
                }
            }
            return null;
        }
    }
}
