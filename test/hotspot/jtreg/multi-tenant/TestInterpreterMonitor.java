
import com.alibaba.tenant.TenantDeathException;
import sun.hotspot.WhiteBox;
import static jdk.test.lib.Asserts.*;

/*
 * @test
 * @summary Test usage of Java monitors with interpreter
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xint TestInterpreterMonitor
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xint -XX:+MultiTenant -XX:+TenantThreadStop TestInterpreterMonitor
 */

public class TestInterpreterMonitor {

    private static final WhiteBox WB = WhiteBox.getWhiteBox();

    public static void main(String[] args) {
        if (WB.getBooleanVMFlag("MultiTenant") && WB.getBooleanVMFlag("TenantThreadStop")) {
            testTenant();
        } else {
            testNormal();
        }
    }

    private static void testTenant() {
        try {
            WB.callInterpreterRuntimeEntry(WB.IRT_ENTRY_NEW_ILLEGAL_MONITOR_STATE_EXCEPTION,
                    new Object[]{
                            new TenantDeathException()
                    });
        } catch (Throwable e) {
            if (e instanceof TenantDeathException) {
                // ignore
            } else {
                fail();
            }
        }
    }

    private static void testNormal() {
        try {
            WB.callInterpreterRuntimeEntry(WB.IRT_ENTRY_NEW_ILLEGAL_MONITOR_STATE_EXCEPTION,
                    new Object[]{
                            new OutOfMemoryError()
                    });
        } catch (IllegalMonitorStateException e) {
            // ignore
        }
    }
}
