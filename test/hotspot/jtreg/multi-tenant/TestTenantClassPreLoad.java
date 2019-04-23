package test.com.alibaba.tenant;

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import com.alibaba.tenant.TenantConfiguration;
import com.alibaba.tenant.TenantContainer;
import com.alibaba.tenant.TenantException;
import com.alibaba.tenant.TenantGlobals;
import com.alibaba.tenant.TenantState;

/* @test
 * @summary unit tests to verify the tenant related classes are preloaded
 * @library /test/lib
 * @compile TestTenantClassPreLoad.java
 * @run main/othervm  test.com.alibaba.tenant.TestTenantClassPreLoad
 */
public class TestTenantClassPreLoad {
    public static void main(String[] args) throws Exception {
        TestTenantClassPreLoad test = new TestTenantClassPreLoad();
        test.testClassLoadingOutputWithMT();
        test.testClassLoadingOutput();

    }

    /**
     * Verify the output of -XX:+TraceClassLoading when the -XX:+MultiTenant is present.
     * @throws Exception when the output is not correct
     */
    void testClassLoadingOutputWithMT() throws Exception {
        System.out.println("TestTenantClassPreLoad.testClassLoadingOutputWithMT:");
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-XX:+MultiTenant", "-XX:+TraceClassLoading", "-version");
        OutputAnalyzer output = new OutputAnalyzer(pb.start());

        /* make sure the tenant related classes are loaded. */
        output.shouldContain(TenantGlobals.class.getCanonicalName());
        output.shouldContain(TenantConfiguration.class.getCanonicalName());
        output.shouldContain(TenantState.class.getCanonicalName());
        output.shouldContain(TenantException.class.getCanonicalName());
        output.shouldContain(TenantContainer.class.getCanonicalName());
        output.shouldHaveExitValue(0);
    }

    /**
     * Verify the output of -XX:+TraceClassLoading when without -XX:+MultiTenant.
     * @throws Exception when the output is not correct
     */
    void testClassLoadingOutput() throws Exception {
        System.out.println("TestTenantClassPreLoad.testClassLoadingOutput:");
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-XX:+TraceClassLoading", "-version");
        OutputAnalyzer output = new OutputAnalyzer(pb.start());

        /* only the TenantGlobals is loaded. */
        output.shouldContain(TenantGlobals.class.getCanonicalName());
        output.shouldNotContain(TenantConfiguration.class.getCanonicalName());
        output.shouldNotContain(TenantState.class.getCanonicalName());
        output.shouldNotContain(TenantException.class.getCanonicalName());
        output.shouldNotContain(TenantContainer.class.getCanonicalName());
        output.shouldHaveExitValue(0);
    }
}
