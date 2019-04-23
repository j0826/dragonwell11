package com.alibaba.tenant;

import java.lang.UnsupportedOperationException;

/**
 * JGroup is a java mirror of Linux control group
 */
class JGroup {

    // Create a JGroup and initialize necessary underlying cgroup configurations
    JGroup(TenantContainer tenant) {
        throw new UnsupportedOperationException();
    }

    /*
     * Attach the current thread into this jgroup.
     * @return 0 if successful
     */
    void attach() {
        throw new UnsupportedOperationException();
    }

    /**
     * Detach the current thread from this jgroup
     * @return 0 if successful
     */
    void detach() {
        throw new UnsupportedOperationException();
    }

    /**
     * Get cpuacct usage of this group
     * @return cpu usage in nano seconds
     */
    long getCpuTime() {
        throw new UnsupportedOperationException();
    }
}
