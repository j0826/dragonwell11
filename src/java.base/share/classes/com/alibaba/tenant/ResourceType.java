package com.alibaba.tenant;

/**
 * Type of resource that can be throttled
 */
enum ResourceType {
    /**
     * Memory resource type
     */
    MEMORY(false),

    /**
     * Corresponding to 'cpu.shares' in CGroup
     */
    CPU_SHARES(true),

    /**
     * Corresponding to 'cpuset' in CGroup
     */
    CPUSET_CPUS(true),

    /**
     * Corresponding to 'cpu.cfs_quota_us' & 'cpu.cfs_period_us' in CGroup
     */
    CPU_CFS(true),

    /**
     * Socket resource type
     */
    SOCKET(false);

    // if this type of resource is controlled by JGroup
    private boolean isJGroupResource;

    ResourceType(boolean isJGroupRes) {
        this.isJGroupResource = isJGroupRes;
    }

    /**
     * Check if this type of resource is controlled by cgroup
     * @return
     */
    public boolean isJGroupResource() {
        return this.isJGroupResource;
    }
}
