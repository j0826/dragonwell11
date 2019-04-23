import com.alibaba.tenant.TenantConfiguration;
import com.alibaba.tenant.TenantContainer;
import com.alibaba.tenant.TenantGlobals;
import static jdk.test.lib.Asserts.*;

/* @test
 * @summary unit tests for com.alibaba.tenant.TenantGlobals
 * @library /test/lib
 * @compile TestTenantGlobals.java
 * @run main/othervm TestTenantGlobals
 *
 */
public class TestTenantGlobals {

    public void testIfTenantIsDisabled() {
        assertFalse(TenantGlobals.isTenantEnabled());

        String value = System.getProperty("com.alibaba.tenant.enableMultiTenant");
        boolean bTenantIsEnabled = false;
        if(value != null && "true".equalsIgnoreCase(value)) {
            bTenantIsEnabled = true;
        }
        assertFalse(bTenantIsEnabled);

        TenantConfiguration tconfig = new TenantConfiguration();
        try {
            TenantContainer.create(tconfig);
            fail(); // should not reach here.
        } catch (UnsupportedOperationException exception) {
            // Expected
            System.out.println("Can not create tenant without -XX:+MultiTenant enabled.");
            exception.printStackTrace();
        }
        try {
            TenantContainer.create("tenant0", tconfig);
            fail(); // should not reach here.
        } catch (UnsupportedOperationException exception) {
            // Expected
            System.out.println("Can not create tenant without -XX:+MultiTenant enabled.");
            exception.printStackTrace();
        }
        try {
            TenantContainer.current();
            fail(); // should not reach here.
        } catch (UnsupportedOperationException exception) {
            // Expected
            System.out.println("Can not get current tenant without -XX:+MultiTenant enabled.");
            exception.printStackTrace();
        }
    }

    public static void main(String[] args) {
        TestTenantGlobals test = new TestTenantGlobals();
        test.testIfTenantIsDisabled();
    }

}
