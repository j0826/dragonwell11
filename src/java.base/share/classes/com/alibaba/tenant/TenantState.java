package com.alibaba.tenant;

/**
 * Defines the state used by TenantContainer
 */
public enum TenantState {
    /**
     * Created, but {@code TenantContainer.run()} has not been executed
     */
    STARTING,

    /**
     * After invoking {@code TenantContainer.run()}, the tenant will enter {@code RUNNING} state permanently.
     */
    RUNNING,

    /**
     * After invoking {@code TenantContainer.stop()}, no new tasks can be executed in {@code TenantContainer.run}.
     */
    STOPPING,

    /**
     * Resources of the tenant have been released.
     */
    DEAD
}
