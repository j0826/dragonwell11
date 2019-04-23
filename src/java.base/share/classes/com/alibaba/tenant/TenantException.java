package com.alibaba.tenant;

/**
 * Exception indicates a multi-tenant related problem has happened.
 */
public class TenantException extends Exception {

    private static final long serialVersionUID = -1L;

    public TenantException(String msg) {
        super(msg);
    }

}
