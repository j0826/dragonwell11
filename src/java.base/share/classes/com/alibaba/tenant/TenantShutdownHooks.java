package com.alibaba.tenant;

import java.util.Collection;
import java.util.IdentityHashMap;

/*
 * Class to track and run tenant level shutdown hooks registered through
 * <tt>{@link Runtime#addShutdownHook Runtime.addShutdownHook}</tt> or
 * <tt>{@link TenantContainer#addShutdownHook TenantContainer.addShutdownHook}</tt>
 *
 */
class TenantShutdownHooks {

    private IdentityHashMap<Thread, Thread> hooks = new IdentityHashMap<>();

    private Collection<Thread> threads = null;

    TenantShutdownHooks() {
    }

    //Add a new shutdown hook.
    synchronized void add(Thread hook) {
        if(hooks == null) {
            throw new IllegalStateException("Shutdown in progress");
        }
        if (hook.isAlive()) {
            throw new IllegalArgumentException("Hook already running");
        }
        if (hooks.containsKey(hook)) {
            throw new IllegalArgumentException("Hook previously registered");
        }
        hooks.put(hook, hook);
    }

    //Remove a previously-registered hook.
    synchronized boolean remove(Thread hook) {
        if(hooks == null) {
            throw new IllegalStateException("Shutdown in progress");
        }
        if (hook == null) {
            throw new NullPointerException();
        }
        return hooks.remove(hook) != null;
    }

    /* Iterates over all hooks creating a new thread for each
     * to run in. Hooks are running concurrently and this method waits for
     * them to finish. This function will be called by the tenant destroying
     * thread at the beginning of {@code TenantContainer.destroy}.
     */
    void runHooks() {
        synchronized(this) {
            threads = hooks.keySet();
            hooks = null;
        }

        for (Thread hook : threads) {
            hook.start();
        }
        for (Thread hook : threads) {
            try {
                hook.join();
            } catch (InterruptedException x) { }
        }
    }
}
