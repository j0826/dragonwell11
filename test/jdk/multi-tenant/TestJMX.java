
import static jdk.test.lib.Asserts.*;
import java.lang.management.ManagementFactory;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import com.alibaba.tenant.TenantConfiguration;
import com.alibaba.tenant.TenantContainer;
import com.alibaba.tenant.TenantException;
import java.util.Set;

/* @test
 * @summary JMX related unit tests
 * @library /test/lib
 * @compile TestJMX.java
 * @run main/othervm -XX:+MultiTenant -XX:+TenantDataIsolation -XX:+UseG1GC -Xmx600m -Xms600m TestJMX
 */
public class TestJMX {

    public interface MXBean {
        String getName();
    }

    public class MXBeanImpl implements MXBean {
        public String getName() {
             return "test";
        }
     }

    private void registerAndVerifyMBean(MBeanServer mbs) {
        try {
            ObjectName myInfoObj = new ObjectName("com.alibaba.tenant.mxbean:type=MyTest");
            MXBeanImpl myMXBean = new MXBeanImpl();
            StandardMBean smb = new StandardMBean(myMXBean, MXBean.class);
            mbs.registerMBean(smb, myInfoObj);

            assertTrue(mbs.isRegistered(myInfoObj));

            //call the method of MXBean
            MXBean mbean =
                    (MXBean)MBeanServerInvocationHandler.newProxyInstance(
                         mbs,new ObjectName("com.alibaba.tenant.mxbean:type=MyTest"), MXBean.class, true);
            assertTrue("test".equals(mbean.getName()));

            Set<ObjectInstance> instances = mbs.queryMBeans(new ObjectName("com.alibaba.tenant.mxbean:type=MyTest"), null);
            ObjectInstance instance = (ObjectInstance) instances.toArray()[0];
            assertTrue(myMXBean.getClass().getName().equals(instance.getClassName()));

            MBeanInfo info = mbs.getMBeanInfo(myInfoObj);
            assertTrue(myMXBean.getClass().getName().equals(info.getClassName()));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    public void testMBeanServerIsolation() {
        //verify in root.
        registerAndVerifyMBean(ManagementFactory.getPlatformMBeanServer());

        TenantConfiguration tconfig   = new TenantConfiguration();
        final TenantContainer tenant  = TenantContainer.create(tconfig);
        final TenantContainer tenant2 = TenantContainer.create(tconfig);

        try {
            tenant.run(() -> {
                //verify in the tenant 1.
                registerAndVerifyMBean(ManagementFactory.getPlatformMBeanServer());
            });
        } catch (TenantException e) {
            e.printStackTrace();
           fail();
        }

        try {
            tenant2.run(() -> {
                //verify in the tenant 1.
                registerAndVerifyMBean(ManagementFactory.getPlatformMBeanServer());
            });
        } catch (TenantException e) {
            e.printStackTrace();
            fail();
        }
    }

    public static void main(String[] args) {
        new TestJMX().testMBeanServerIsolation();
    }
}
