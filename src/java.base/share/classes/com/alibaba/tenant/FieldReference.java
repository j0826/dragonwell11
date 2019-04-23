package com.alibaba.tenant;

/*
 * FieldReference represents a reference to a Java object from another object's field.
 * It is used to isolate static field references from shared class to per-tenant objects.
 *
 * @param <T>
 */
class FieldReference<T> {

    private T referent;

    /**
     * Constructs a new FieldReference with r as the referent
     * @param r The object to which this FieldReference points
     */
    FieldReference(T r) {
        this.referent = r;
    }

    /**
     * Returns this FieldReference referent
     * @return The referent
     */
    T get() {
        return referent;
    }

    /**
     * Sets {@code referent}
     * @param t The new referent value
     */
    @SuppressWarnings("unchecked")
    void set(Object t) {
        referent = (T)t;
    }
}
