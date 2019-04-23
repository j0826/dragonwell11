package com.alibaba.tenant;

/**
 * A special exception class used primarily by {@code TenantContainer.prepareForTenantDestroy}
 * to safely unwind the stack frames of a thread in {@code TenantState.STOPPING} phase of a {@code TenantContainer}.
 *
 */
public class TenantDeathException extends Throwable {

    private static final long serialVersionUID = -1L;

}
