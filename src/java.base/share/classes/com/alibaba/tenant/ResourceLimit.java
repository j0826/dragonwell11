package com.alibaba.tenant;

/**
 * Quota of one specific resource
 */
interface ResourceLimit {

    /**
     * Type of the resource
     * @return
     */
    ResourceType type();

    /**
     * Flush the quota configuration to jgroup controllers
     * @param jgroup
     */
    void sync(JGroup jgroup);
}
