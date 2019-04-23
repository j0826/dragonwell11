package jdk.internal.misc;

public interface TenantAccess {

    /**
     * Register a thread to {@code tenant}'s service thread list.
     * At present, a service thread is either a registered shutdown hook thread or Finalizer thread.
     *
     * @param tenant  The teant container to register thread with
     * @param thread  Thread to be registered
     */
    void registerServiceThread(Object tenant, Thread thread);

    /**
     * Use may use {@code TenantContainer.setCurrentThreadInheritance} to set status of each Java thread separately
     * @return by default, if newly created threads should inherit parent thread's TenantContainer.
     */
    boolean threadInheritance();
}
