package com.alibaba.tenant;

/**
 * This class defines the constants used by multi-tenant JDK
 */
public class TenantGlobals {
    /**
     * Retrieves the flags used by multi-tenant module.
     * @return the flags
     */
    private native static int getTenantFlags();

    private final static int flags = getTenantFlags();

    /**** Be careful: the following bit definitions must be consistent with
     **** the ones defined in prims/tenantenv.cpp **/

    /**
     * Bit to indicate that if the multi-tenant feature is enabled.
     */
    public static final int TENANT_FLAG_MULTI_TENANT_ENABLED      = 0x1;

    /**
     * Bit to indicate that if heap throttling feature is enabled
     */
    public static final int TENANT_FLAG_HEAP_THROTTLING_ENABLED   = 0x2;

    /**
     * Bit to indicate that if cpu throttling feature is enabled
     */
    public static final int TENANT_FLAG_CPU_THROTTLING_ENABLED    = 0x4;
    /**
     * Bit to indicate that if data isolation feature is enabled
     */
    public static final int TENANT_FLAG_DATA_ISOLATION_ENABLED    = 0x8;

    /**
     * Bit to indicate that if spawned threads will be killed at TenantContainer.destroy()
     */
    public static final int TENANT_FLAG_THREAD_STOP_ENABLED       = 0x10;

    /**
     * Bit to indicate that if IO handle reclaiming feature is enabled
     */
    public static final int TENANT_FLAG_IOHANDLE_RECLAIMING_ENABLED    = 0x20;

    /**
     * Bit to indicate that if cpu accounting feature is enabled
     */
    public static final int TENANT_FLAG_CPU_ACCOUNTING_ENABLED    = 0x40;


    /**
     * Bit to indicate that if heap isolation feature is enabled
     */
    public static final int TENANT_FLAG_HEAP_ISOLATION_ENABLED    = 0x80;

    /*
     * Bit to indicate that if DirectTenureAlloc feature is enabled
     */
    public static final int TENANT_FLAG_DIRECT_TENURED_ALLOC_ENABLED = 0x100;

    /**
     * Bit to indicate that if UseGCIH is enabled
     */
    public static final int TENANT_FLAG_USE_TENANT_GCIH = 0x200;

    /**
     * Bit to indicate that if socket throttling feature is enabled
     */
    public static final int TENANT_FLAG_SOCKET_THROTTLING_ENABLED = 0x400;

    private TenantGlobals() { }

    /**
     * Test if multi-tenant feature is enabled.
     * @return true if enabled otherwise false
     */
    public static boolean isTenantEnabled() {
       return 0 != (flags & TENANT_FLAG_MULTI_TENANT_ENABLED);
    }

    /**
     * Test if heap throttling feature is enabled.
     * @return true if enabled otherwise false
     */
    public static boolean isHeapThrottlingEnabled() {
        return 0 != (flags & TENANT_FLAG_HEAP_THROTTLING_ENABLED);
    }

    /**
     * Test if heap isolation feature is enabled.
     * @return true if enabled otherwise false
     */
    public static boolean isHeapIsolationEnabled() {
        return 0 != (flags & TENANT_FLAG_HEAP_ISOLATION_ENABLED);
    }


    /**
     * Test if cpu throttling feature is enabled.
     * @return true if enabled otherwise false
     */
    public static boolean isCpuThrottlingEnabled() {
        return 0 != (flags & TENANT_FLAG_CPU_THROTTLING_ENABLED);
    }

    /**
     * Test if cpu accounting feature is enabled.
     * @return true if enabled otherwise false
     */
    public static boolean isCpuAccountingEnabled() {
        return 0 != (flags & TENANT_FLAG_CPU_ACCOUNTING_ENABLED);
    }

    /**
     * Test if data isolation feature is enabled.
     * @return true if enabled otherwise false
     */
    public static boolean isDataIsolationEnabled() {
        return 0 != (flags & TENANT_FLAG_DATA_ISOLATION_ENABLED);
    }

    /**
     * Test if thread stop feature is enabled.
     * @return true if enabled otherwise false
     */
    public static boolean isThreadStopEnabled() {
        return 0 != (flags & TENANT_FLAG_THREAD_STOP_ENABLED);
    }

    /**
     * Test if UseGCIH is enabled.
     * @return true if enabled otherwise false
     */
    public static boolean isTenantGCIHEnabled() {
        return 0 != (flags & TENANT_FLAG_USE_TENANT_GCIH);
    }

     /**
     * Test if IO handle reclaiming feature is enabled.
     * @return true if enabled otherwise false
     */
    public static boolean isIOHandleReclaimingEnabled() {
        return 0 != (flags & TENANT_FLAG_IOHANDLE_RECLAIMING_ENABLED);
    }

    /**
     * Test if DirectTenuredAlloc feature is enabled.
     * @return true if enabled otherwise false
     */
    public static boolean isDirectTenuredAllocEnabled() {
        return 0 != (flags & TENANT_FLAG_DIRECT_TENURED_ALLOC_ENABLED);
    }

    /**
     * Test if TenantSocketThrottling feature is enabled.
     * @return true if enable otherwise false
     */
    public static boolean isSocketThrottlingEnabled() {
        return 0 != (flags & TENANT_FLAG_SOCKET_THROTTLING_ENABLED);
    }
}
