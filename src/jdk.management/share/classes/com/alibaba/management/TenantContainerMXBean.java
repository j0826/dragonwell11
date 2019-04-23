package com.alibaba.management;

import java.lang.management.PlatformManagedObject;
import java.util.List;

/**
 * Management interface for multi-tenant module
 */
public interface TenantContainerMXBean extends PlatformManagedObject {

    /**
     * @return IDs of all live TenantContainer
     */
    List<Long> getAllTenantIds();

    /**
     * Only works with VM option -XX:+TenantCpuAccounting
     * @param id Tenant ID retrieved by {@code TenantContainer.getTenantd()}
     * @return  Total accumulated process time (in milliseconds) has been consumed by threads attached to this {@code TenantContainer}
     */
    long getTenantProcessCpuTimeById(long id);

    /**
     * Only works with VM option -XX:+TenantHeapIsolation
     * @param id Tenant ID retrieved by {@code TenantContainer.getTenantId()}
     * @return  Total accumulated memory size (in bytes) has been allocated by threads attached to this {@code TenantContainer}
     */
    long getTenantAllocatedMemoryById(long id);

    /**
     * Only works with VM option -XX:+TenantHeapIsolation
     * @param id Tenant ID retrieved by {@code TenantContainer.getTenantId()}
     * @return  Memory size (in bytes) is now being occupied by objects belong to this {@code TenantContainer}
     */
    long getTenantOccupiedMemoryById(long id);

    /**
     *
     * @param  id Tenant ID retrieved by {@code TenantContainer.getTenantId()}
     * @return  Names of the {@code TenantContainer} represented by {@code id}
     */
    String getTenantNameById(long id);
}
