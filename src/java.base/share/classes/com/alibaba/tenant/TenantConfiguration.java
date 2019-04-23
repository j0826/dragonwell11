package com.alibaba.tenant;

import jdk.internal.misc.SharedSecrets;
import jdk.internal.misc.VM;
import sun.security.action.GetPropertyAction;
import java.security.AccessController;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * The configuration used by tenant
 */
public class TenantConfiguration {

    // Property names
    private static final String PROP_THREAD_INHERITANCE = "com.alibaba.tenant.threadInheritance";
    private static final String PROP_ALLOW_PER_THREAD_INHERITANCE = "com.alibaba.tenant.allowPerThreadInheritance";

    // Property values
    private static final boolean THREAD_INHERITANCE;
    private static final boolean ALLOW_PER_THREAD_INHERITANCE;

    static {
        if (!VM.isBooted()) {
            throw new IllegalStateException("TenantConfiguration must be initialized after VM.booted()");
        }

        THREAD_INHERITANCE = Boolean.parseBoolean(
                AccessController.doPrivileged(new GetPropertyAction(PROP_THREAD_INHERITANCE, "true")));
        ALLOW_PER_THREAD_INHERITANCE = Boolean.parseBoolean(
                AccessController.doPrivileged(new GetPropertyAction(PROP_ALLOW_PER_THREAD_INHERITANCE, "true")));

        // default value is 'true', update system threads if 'false'
        if (!THREAD_INHERITANCE) {
            ThreadGroup tg = Thread.currentThread().getThreadGroup();
            while (tg.getParent() != null) {
                tg = tg.getParent();
            }
            Thread threads[] = new Thread[tg.activeCount() << 1];
            int nofThreads = tg.enumerate(threads);
            for (int i = 0; i < nofThreads; ++i) {
                SharedSecrets.getJavaLangAccess()
                        .setChildShouldInheritTenant(threads[i], THREAD_INHERITANCE);
            }
        }
    }

    /*
     * @return true if newly created threads should inherit parent's {@code TenantContainer}, otherwise false
     */
    static boolean threadInheritance() {
        return THREAD_INHERITANCE;
    }

    /*
     * @return true if allow each thread to modify its policy of
     *         inherited {@code TenantContainer} by newly created children threads
     */
    static boolean allowPerThreadInheritance() {
        return ALLOW_PER_THREAD_INHERITANCE;
    }

    /*
     * Resource throttling configurations
     */
    private Map<ResourceType, ResourceLimit> limits = new HashMap<>();

    /**
     * Create an empty TenantConfiguration, no limitations on any resource
     */
    public TenantConfiguration() {
    }

    /**
     * @param maxCPUPercent
     * @param cpuShare
     * @param maxHeap
     */
    @Deprecated
    public TenantConfiguration(int maxCPUPercent, int cpuShare, long maxHeap) {
        // TODO
    }

    /**
     * @param cpuShare
     * @param maxHeapBytes
     */
    @Deprecated
    public TenantConfiguration(int cpuShare, long maxHeapBytes) {
        // TODO
    }

    /**
     * @param maxHeapBytes
     */
    @Deprecated
    public TenantConfiguration(long maxHeapBytes) {
        // TODO
    }

    /*
     * @return all resource limits specified by this configuration
     */
    ResourceLimit[] getAllLimits() {
        if (limits.size() == 0) {
            return null;
        } else {
            return limits.values().toArray(new ResourceLimit[limits.size()]);
        }
    }

    /*
     * @param type  resource type
     * @return  resource {@type}'s limit specified by this configuration
     */
    ResourceLimit getLimit(ResourceType type) {
        return limits.get(type);
    }

    /*
     * Set limmit
     * @param type  resource type to be limited
     * @param limit value of the limit
     */
    void setLimit(ResourceType type, ResourceLimit limit) {
        limits.put(type, limit);
    }
}
