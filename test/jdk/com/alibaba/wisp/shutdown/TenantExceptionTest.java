/*
 * @test
 * @summary TenantExceptionTest
 * @library /lib/testlibrary
 * @run main/othervm  -XX:+MultiTenant -XX:+TenantDataIsolation -XX:+TenantThreadStop -XX:+UseG1GC -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true TenantExceptionTest
 * @run main/othervm  -XX:+MultiTenant -XX:+TenantDataIsolation -XX:+TenantThreadStop -XX:+UseG1GC -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.wisp.version=2 TenantExceptionTest
*/

import com.alibaba.tenant.TenantDeathException;

import java.dyn.Coroutine;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static jdk.testlibrary.Asserts.assertFalse;
import static jdk.testlibrary.Asserts.assertTrue;

public class TenantExceptionTest {
    public static void main(String[] args) throws Exception {
        testThrInFinal();
        testSwitchInFinal();
        testThrInFinal();
    }

    private static void testThrInFinal() {
        try {
            try {
                throw new TenantDeathException();
            } catch (Throwable t) {

            } finally {
                throw new IOException();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("== not killed ==");
    }


    private static void testSwitchInFinal() {
        AtomicBoolean hasFinal2 = new AtomicBoolean();
        Coroutine[] co = new Coroutine[2];

        co[0] = new Coroutine(() -> {
            try {
                throw new TenantDeathException();
            } catch (Throwable t) {
                // skip
            } finally {
                Coroutine.yieldTo(co[1]);
                System.out.println("co[0] finally");
                hasFinal2.set(true);
            }

            System.out.println("co[0] out");
        });

        co[1] = new Coroutine(() -> {
            System.out.println("co[1]");
            Coroutine.yieldTo(co[0]);
        });

        Coroutine.yieldTo(co[0]);

        assertTrue(hasFinal2.get());
    }

    private static void testCatch() throws Exception {
        AtomicBoolean hasCatched = new AtomicBoolean();
        AtomicBoolean hasFinal = new AtomicBoolean();

        new Thread(() -> {
            try {
                throw new TenantDeathException();
            } catch (Throwable t) {
                hasCatched.set(true);
            } finally {
                hasFinal.set(true);
            }
        }).start();

        Thread.sleep(100);

        assertFalse(hasCatched.get());
        assertTrue(hasFinal.get());
    }
}
