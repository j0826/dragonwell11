package com.alibaba.tenant;

import jdk.internal.misc.SharedSecrets;
import jdk.internal.misc.TenantAccess;
import sun.security.action.GetBooleanAction;
import sun.security.action.GetLongAction;

import java.lang.ref.WeakReference;
import java.security.AccessController;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * TenantContainer is a "virtual container" for a tenant of application, the
 * resource consumption of tenant such as CPU, heap is constrained by the policy
 * of this "virtual container". The thread can run in virtual container by
 * calling <code>TenantContainer.run</code>
 *
 */
public class TenantContainer {

    /* Make sure registerNatives is the first thing <clinit> does. */
    private static native void registerNatives();

    static {
        registerNatives();

        DEBUG_SHUTDOWN = AccessController.doPrivileged(
                new GetBooleanAction("com.alibaba.tenant.DebugTenantShutdown"));
        KILL_THREAD_INTERVAL = AccessController.doPrivileged(
                new GetLongAction("com.alibaba.tenant.KillThreadInterval", 20L));
        SHUTDOWN_SOFT_LIMIT = AccessController.doPrivileged(
                new GetLongAction("com.alibaba.tenant.ShutdownSTWSoftLimit", -1L));
        PRINT_STACKS_ON_TIMEOUT_DELAY = AccessController.doPrivileged(
                new GetLongAction("com.alibaba.tenant.PrintStacksOnTimeoutDelay", -1L));
    }

    /*
     * Used to generate the tenant id.
     */
    private static long tenantID = 0 ;

    private static synchronized long nextTenantID() {
        return tenantID++;
    }

    /*
     * Used to hold the mapping from tenant id to TenantContainer object for all
     * tenants
     */
    private static Map<Long, TenantContainer> tenantContainerMap = null;

    // value of property "com.alibaba.tenant.DebugTenantShutdown"
    private static final boolean    DEBUG_SHUTDOWN;

    // value of property "com.alibaba.tenant.KillThreadInterval"
    private static final long       KILL_THREAD_INTERVAL;

    // value of property "com.alibaba.tenant.ShutdownSTWSoftLimit"
    private static final long       SHUTDOWN_SOFT_LIMIT;

    // value of property "com.alibaba.tenant.PrintStacksOnTimeoutDelay"
    private static final long       PRINT_STACKS_ON_TIMEOUT_DELAY;

    /*
     * Holds the threads attached with this tenant container
     */
    private List<Thread> attachedThreads = new LinkedList<>();

    /*
     * Newly created threads which attach to this tenant container
     */
    private List<WeakReference<Thread>> spawnedThreads = Collections.synchronizedList(new ArrayList<>());

    /*
     * Used to contain service threads, including finalizer threads, shutdown hook threads.
     */
    private Map<Thread, Void> serviceThreads = Collections.synchronizedMap(new WeakHashMap<>());

    /*
     * the configuration of this tenant container
     */
    private TenantConfiguration configuration = null;

    /*
     * tenant state
     */
    private volatile TenantState state;

    /*
     * tenant id
     */
    private long tenantId;

    /*
     * tenant name
     */
    private String name;

    /*
     * tenant jgroup
     */
    private volatile JGroup jgroup;

    /*
     * Used to store the system properties per tenant
     */
    private Properties props;

    /*
     * If stacktraces of die-hard threads have been printed after TenantContainer.destroy() timeout.
     * To let thread-dump happen only once.
     */
    private boolean stacksPrintedOnTimeout = false;

    /*
     * Get the tenant container attached with current thread.
     * @return the tenant of the current thread
     */
    private native static TenantContainer current0();

    /*
     * Attach the current thread into the receiver.
     * @return 0 if successful
     */
    private native int attach0();

    /*
     * Gets an array containing the amount of memory allocated on the Java heap for a set of threads (in bytes)
     */
    private native void getThreadsAllocatedMemory(long[] ids, long[] memSizes);

    /*
     * allocated memory of attached threads, which is accumulated for current tenant
     */
    private long accumulatedMemory = 0L;

    /**
     * Get total allocated memory of this tenant.
     * @return the total allocated memory of this tenant.
     */
    public synchronized long getAllocatedMemory() {
        Thread[] threads = getAttachedThreads();
        int size         = threads.length;
        long[] ids       = new long[size];
        long[] memSizes  = new long[size];

        for (int i = 0; i < size; i++) {
            ids[i] = threads[i].getId();
        }

        getThreadsAllocatedMemory(ids, memSizes);

        long totalThreadsAllocatedMemory = 0;
        for (long s : memSizes) {
            totalThreadsAllocatedMemory += s;
        }
        return totalThreadsAllocatedMemory + accumulatedMemory;
    }

    /**
     * Data repository used to store the data isolated per tenant.
     */
    private TenantData tenantData = new TenantData();

    /**
     * Used to track and run tenant shutdown hooks
     */
    private TenantShutdownHooks tenantShutdownHooks = new TenantShutdownHooks();

    /*
     * classloaders associated with this TenantContainer, will not be accessed any more after all tenant threads
     * being terminated.
     */
    private Map<ClassLoader, ClassLoader> tenantLoaders;

    /*
     * the parent tenant contianer.
     */
    private TenantContainer parent;

    /**
     * Retrieves the data repository used by this tenant.
     * @return the data repository associated with this tenant.
     */
    public TenantData getTenantData() {
        return tenantData;
    }

    /**
     * Sets the tenant properties to the one specified by argument.
     * @param props the properties to be set, CoW the system properties if it is null.
     */
    public void setProperties(Properties props) {
        if (props == null) {
            props = new Properties();
            Properties sysProps = System.getProperties();
            for(Object key: sysProps.keySet()) {
                props.put(key, sysProps.get(key));
            }
        }
        this.props = props;
    }

    /**
     * Gets the properties of tenant
     * @return the tenant properties
     */
    public Properties getProperties() {
        return props;
    }

    /**
     * Sets the property indicated by the specified key.
     * @param  key the name of the property.
     * @param  value the value of the property.
     * @return the previous value of the property,
     *         or null if it did not have one.
     */
    public String setProperty(String key, String value) {
        checkKey(key);
        return (String) props.setProperty(key, value);
    }

    /**
     * Gets the property indicated by the specified key.
     * @param  key  the name of the property.
     * @return the  string value of the property,
     *         or null if there is no property with that key.
     */
    public String getProperty(String key) {
        checkKey(key);
        return props.getProperty(key);
    }

    /**
     * Removes the property indicated by the specified key.
     * @param  key  the name of the property to be removed.
     * @return the  previous string value of the property,
     *         or null if there was no property with that key.
     */
    public String clearProperty(String key) {
        checkKey(key);
        return (String) props.remove(key);
    }

    private void checkKey(String key) {
        if (null == key) {
            throw new NullPointerException("key can't be null");
        }
        if ("".equals(key)) {
            throw new IllegalArgumentException("key can't be empty");
        }
    }

    //
    // Used to synchronize between destroy() and runThread()
    private ReentrantReadWriteLock destroyLock = new ReentrantReadWriteLock();

    // Timestamp when destroy() starts
    private long destroyBeginTimestamp = -1;

    /**
     * <p>Destroy this tenant container and release occupied resources including memory, cpu, FD, etc.
     *
     * <p>Properties to control the behavioral details of TenantContainer.shutdown:
     * <ul>
     *     <li><b>com.alibaba.tenant.KillThreadInterval</b> Time interval (in millisecond) between two JVM kill-thread
     *          operations triggered by {@code TenantContainer.destroy}.
     *     <li><b>com.alibaba.tenant.DebugTenantShutdown</b> True to print extra debugging information,
     *          false by default.
     *     <li><b>com.alibaba.tenant.ShutdownSTWSoftLimit</b> Time limit (in millisecond) of accumulated JVM kill-thread
     *          operation time, {@code TenantContainer.destroy} will be blocked during this period;
     *          if there are still live threads after exceeding this limit,
     *          {@code TenantContainer.destroy} will return and a background thread will be started to kill thread in
     *          asynchronous manner.
     *          -1 by default, which will block {@code TenantContainer.destroy()} infinitely
     *          until all spawned threads got killed.
     *     <li><b>com.alibaba.tenant.PrintStacksOnTimeoutDelay</b> If {@code TenantContainer.destroy()} does not finish
     *          within this time limit (in millisecond), stacktraces of all remaining alive threads
     *          will be printed to STDOUT.
     *          -1 by default, which means never print stacktraces.
     * </ul>
     *
     */
    @SuppressWarnings("unchecked")
    public void destroy() {
        if (TenantContainer.current() != null) {
            throw new RuntimeException("Should only call destroy() in ROOT tenant");
        }

        destroyBeginTimestamp = System.currentTimeMillis();

        destroyLock.writeLock().lock();
        try {
            if (state != TenantState.STOPPING && state != TenantState.DEAD) {
                setState(TenantState.STOPPING);

                tenantContainerMap.remove(getTenantId());

                // finish all finalizers
                attach0();
                try {
                    Runtime.getRuntime().runFinalization();
                } finally {
                    detach0();
                }

                // execute all shutdown hooks
                tenantShutdownHooks.runHooks();

                // Kill all threads
                if (TenantGlobals.isThreadStopEnabled()) {
                    serviceThreads.keySet().forEach(k -> spawnedThreads.add(new WeakReference<>(k)));
                    if (killAllThreads(spawnedThreads, true)) {
                        cleanUp();
                    }
                    spawnedThreads = Collections.EMPTY_LIST;
                } else {
                    cleanUp();
                }
            }
        } catch (Throwable t) {
            System.err.println("Exception from TenantContainer.destroy()");
            t.printStackTrace();
        } finally {
            setState(TenantState.DEAD);
            destroyLock.writeLock().unlock();
        }

        debug_shutdown("TenantContainer.destroy() costs " + (System.currentTimeMillis() - destroyBeginTimestamp) + "ms");
    }

    /*
     * Release all native resources and Java references
     * should be the very last step of {@link #destroy()} operation.
     * If cannot kill all threads in {@link #killAllThreads()}, should do this in {@link WatchDogThread}
     *
     */
    private void cleanUp() {

        if (jgroup != null) {
            jgroup.destroy();
            jgroup = null;
        }

        if (TenantGlobals.isThreadStopEnabled()) {
            int cnt = 0;
            for (ClassLoader cl : tenantLoaders.keySet()) {
                if (cl != null) {
                    ++cnt;
                    disposeTenantClassLoader(cl);
                }
            }
            debug_shutdown("Totally " + cnt + " classloaders disposed!");
            tenantLoaders.clear();
            tenantLoaders = null;
        }

        // clear references
        spawnedThreads.clear();
        attachedThreads.clear();
        tenantData.clear();
        tenantShutdownHooks = null;
        parent = null;
    }

    /*
     * Try to kill all {@code threads}
     * @param threads                   List of threads to be killed
     * @param asyncIfExceedsSoftLimit   Create a new WatchDog thread and finish remaining work if exceed
     *                                  {@code com.alibaba.tenant.ShutdownSTWSoftLimit} and value of
     *                                  {@code com.alibaba.tenant.ShutdownSTWSoftLimit} is greater than zero.
     * @return                          True if all {@code threads} killed successfully, otherwise false
     *
     */
    private boolean killAllThreads(List<WeakReference<Thread>> threads,
                                   boolean asyncIfExceedsSoftLimit) {
        Long tries          = 0L;    // number of calls to prepareForDestroy0()
        Long timeSTW        = 0L;    // approximate total stop-the-world time, in ms
        Long maxSTW         = -1L;   // approximate maximum stop-the-world time, in ms
        Long lastTime       = 0L;
        Long timeBegin      = System.currentTimeMillis();
        Boolean result      = true;
        Integer oldPriority = Thread.currentThread().getPriority();

        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

        // Increase priority of dying threads to give them higher possibility to handle exceptions
        if (!asyncIfExceedsSoftLimit) {
            for (WeakReference<Thread> ref : threads) {
                Thread t = ref.get();
                if (t != null) {
                    t.setPriority(Thread.MAX_PRIORITY - 1);
                }
            }
        }

        while (!threads.isEmpty()) {
            long now = System.currentTimeMillis();
            Boolean forcefully = (now - timeBegin > KILL_THREAD_INTERVAL * 10); // if should try to kill thread forcefully
            purgeDeadThreads(threads);

            if (lastTime == 0 || now - lastTime >= KILL_THREAD_INTERVAL) {
                // only enter safepoint when there is unmarked java threads
                boolean anyUnmarkedThreads = false;
                for (WeakReference<Thread> ref : threads) {
                    Thread t = ref.get();
                    if (t != null && !hasTenantDeathException(t)) {
                        anyUnmarkedThreads = true;
                        break;
                    }
                }

                if (anyUnmarkedThreads) {
                    now = System.currentTimeMillis();

                    // prepareForDestory0 will submit a VM operation and wait for it to complete
                    prepareForDestroy0(true);

                    if (DEBUG_SHUTDOWN) {
                        long curSTW = System.currentTimeMillis() - now;
                        if (curSTW > maxSTW) {
                            maxSTW = curSTW;
                        }
                        timeSTW += curSTW;
                        ++tries;
                    }

                    lastTime = System.currentTimeMillis();
                }

                // do waking-up at certain interval to avoid making target thread to process signals all the time
                if (forcefully) {
                    purgeDeadThreads(threads);
                    for (WeakReference<Thread> ref : threads) {
                        Thread t = ref.get();
                        if (t != null) {
                            wakeUpTenantThread(t);
                        }
                    }
                }
            }

            purgeDeadThreads(threads);
            interruptThreads(threads, forcefully);

            // Print stacktraces of die-hard threads
            if (!stacksPrintedOnTimeout
                    && !threads.isEmpty()
                    && PRINT_STACKS_ON_TIMEOUT_DELAY > 0
                    && (System.currentTimeMillis() - destroyBeginTimestamp) > PRINT_STACKS_ON_TIMEOUT_DELAY) {
                Thread[] thrdArray = threads.stream()
                        .map(r -> r.get())
                        .collect(Collectors.toList()).toArray(new Thread[threads.size()]);
                dumpThreads(thrdArray);
                stacksPrintedOnTimeout = true;
            }

            // if cannot kill all threads within time of 'com.alibaba.tenant.ShutdownSTWSoftLimit',
            // start a daemon thread to watch and kill them
            if (asyncIfExceedsSoftLimit && SHUTDOWN_SOFT_LIMIT > 0
                    && !threads.isEmpty()
                    && (timeSTW > SHUTDOWN_SOFT_LIMIT
                        || (System.currentTimeMillis() - timeBegin) > (SHUTDOWN_SOFT_LIMIT << 4))) {
                // spawn a watch dog thread to take care of the remaining threads
                WatchDogThread watchDog = new WatchDogThread(this, threads);
                watchDog.start();

                result = false;
                break;
            }
        }

        Thread.currentThread().setPriority(oldPriority);

        debug_shutdown("TenantContainer.killThreads() costs " + (System.currentTimeMillis() - timeBegin)
                + "ms, paused " + timeSTW + "ms, tried " + tries + " times, max paused " + maxSTW + "ms.");

        return result;
    }

    // clean up 'dead' threads from thread list
    private static void purgeDeadThreads(List<WeakReference<Thread>> threads) {
        threads.removeIf(ref -> {
            Thread t = ref.get();
            return t == null || !t.isAlive() || t.getState() == Thread.State.TERMINATED;
        });
    }

    /*
     * Call {@code interruptTenantThread} to wake up all threads in {@code threads}
     * @param force     True if caller wants to call {@code interruptTenantThread} unconditionally, otherwise will only
     *                  do that when {@link Thread.#getState()} is in {@link Thread.State.#WAITING}
     *                  or {@link Thread.State.#TIMED_WAITING} status.
     */
    private static void interruptThreads(List<WeakReference<Thread>> threads, boolean force) {
        for (WeakReference<Thread> ref : threads) {
            Thread t = ref.get();
            if (t != null &&
                    (force || t.getState() == Thread.State.WAITING || t.getState() == Thread.State.TIMED_WAITING)) {
                try {
                    interruptTenantThread(t);
                } catch (Throwable ignore) {
                    if (DEBUG_SHUTDOWN) {
                        debug_shutdown("Exception from Thread.interrupt()");
                        ignore.printStackTrace();
                    }
                }
            }
        }
    }

    /*
     * Last resort to kill remaining threads when {@code prepareForDestroy0} exceeds soft STW time limit,
     * which is defined by property {@code com.alibaba.tenant.ShutdownSTWSoftLimit}
     */
    private class WatchDogThread extends Thread {

        private TenantContainer tenant;
        private List<WeakReference<Thread>> threads;

        WatchDogThread(TenantContainer tenant, List<WeakReference<Thread>> threads) {
            this.tenant = tenant;
            this.threads = threads;
            setDaemon(true);
            setPriority(MAX_PRIORITY);
            setName("WatchDog-" + tenant.getName());

            if (DEBUG_SHUTDOWN) {
                debug_shutdown("Failed to kill all threads within soft limit, remaining "
                        + threads.size() + " threads are:");
                for (WeakReference<Thread> ref : threads) {
                    Thread t = ref.get();
                    if (t != null) {
                        debug_shutdown(t.toString() + ",id=" + t.getId() + ",status=" + t.getState());
                    }
                }
                debug_shutdown("Spawning watch dog thread" + this);
            }
        }

        @Override
        public void run() {
            long timeStart = System.currentTimeMillis();
            int remainingCount = threads.size();
            killAllThreads(threads, false);
            debug_shutdown("WatchDogThread costs " + (System.currentTimeMillis() - timeStart)
                    + "ms to kill remaining " + remainingCount + "threads");
            tenant.cleanUp();
        }
    }

    // for debugging purpose
    private static void debug_shutdown(String msg) {
        if (DEBUG_SHUTDOWN) {
            System.err.println("[DEBUG] " + msg);
        }
    }

    /*
     * Interrupt {@code thread} or nop if thread is masked for tenant shutdown
     * @param thread   Thread object to be interrupted
     */
    private static native void interruptTenantThread(Thread thread);

    /*
     * Prepare native data structures for tenant destroy
     *
     * @param osWakeUp  Whether or not to use operating system's thread control facilities to wake up thread.
     */
    private native void prepareForDestroy0(boolean osWakeUp);

    /*
     * Determines if a thread is marked as being killed by {@code TenantContainer.destroy()}
     *
     * @param thread    Thread object to be checked, if {@code null} current thread will be checked
     * @return          True if {@code thread} marked, otherwise false
     */
    private static native boolean hasTenantDeathException(Thread thread);

    /*
     * Tries to wake up a thread using operating system's facilities.
     * The behavior is platform dependant, on Linux a signal (with empty handler)
     * will be sent to interrupt current operation.
     *
     * @param thread    Thread to be waken up
     */
    private static native void wakeUpTenantThread(Thread thread);

    /*
     * Detach current thread from this tenant
     *
     * @return 0 if successful
     */
    private native int detach0();

    private TenantContainer(Long tenantId, String name, TenantConfiguration configuration) {
        this.tenantId = tenantId;
        this.name = name;
        this.configuration = configuration;
        if (TenantGlobals.isThreadStopEnabled()) {
            this.tenantLoaders = Collections.synchronizedMap(new WeakHashMap<>());
        }
    }

    TenantConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * @return the tenant state
     */
    public TenantState getState() {
        return state;
    }

    /*
     * Set the tenant state
     * @param state used to set
     */
    private void setState(TenantState state) {
        this.state = state;
    }

    /**
     * Returns the tenant' id
     * @return tenant id
     */
    public long getTenantId() {
        return tenantId;
    }

    /**
     * Returns this tenant's name.
     * @return this tenant's name.
     */
    public String getName() {
        return name;
    }

    /**
     * @return A collection of all threads attached to the container.
     */
    public synchronized Thread[] getAttachedThreads() {
        return attachedThreads.toArray(new Thread[attachedThreads.size()]);
    }

    /**
     * Get the tenant container by id
     * @param id tenant id.
     * @return the tenant specified by id, null if the id doesn't exist.
     */
    public static TenantContainer getTenantContainerById(long id) {
        checkIfTenantIsEnabled();
        return tenantContainerMap.get(id);
    }

    /**
     * Create tenant container by the configuration
     * @param configuration used to create tenant
     * @return the tenant container
     */
    public static TenantContainer create(TenantConfiguration configuration) {
        return create(TenantContainer.current(), configuration);
    }

    /**
     * Create tenant container by the configuration
     * @param parent parent tenant container
     * @param configuration used to create tenant
     * @return the tenant container
     */
    public static TenantContainer create(TenantContainer parent, TenantConfiguration configuration) {
        checkIfTenantIsEnabled();
        //parameter checking
        if (null == configuration) {
            throw new IllegalArgumentException("Failed to create tenant, illegal arguments: configuration is null");
        }

        long id     = nextTenantID();
        String name = "Tenant-" + id; // default name for tenant
        return create(parent, id, name, configuration);
    }

    /**
     * Create tenant container by the name and configuration
     * @param name the tenant name
     * @param configuration used to create tenant
     * @return the tenant container
     */
    public static TenantContainer create(String name, TenantConfiguration configuration) {
        return create(TenantContainer.current(), name, configuration);
    }

    /**
     * Create tenant container by the name and configuration
     * @param parent parent tenant container
     * @param name the tenant name
     * @param configuration used to create tenant
     * @return the tenant container
     */
    public static TenantContainer create(TenantContainer parent, String name, TenantConfiguration configuration) {
        checkIfTenantIsEnabled();
        //parameter checking
        if (null == name) {
            throw new IllegalArgumentException("Failed to create tenant, illegal arguments: name is null");
        }
        if (null == configuration) {
            throw new IllegalArgumentException("Failed to create tenant, illegal arguments: configuration is null");
        }
        return create(parent, nextTenantID(), name, configuration);
    }

    private static TenantContainer create(TenantContainer parent, Long id, String name, TenantConfiguration configuration) {
        TenantContainer tc = new TenantContainer(id, name, configuration);
        tc.setState(TenantState.STARTING);

        tc.parent = parent;

        //Initialize the tenant properties.
        tc.props = new Properties();

        //copy system properties into tenant
        Properties sysProps = System.getProperties();
        for(Object key: sysProps.keySet()) {
            tc.props.put(key, sysProps.get(key));
        }

        // cgroup for tenant
        if (TenantGlobals.isCpuThrottlingEnabled() || TenantGlobals.isCpuAccountingEnabled()) {
            tc.jgroup = new JGroup(tc);
        }

        tenantContainerMap.put(tc.getTenantId(), tc);
        return tc;
    }

    TenantContainer getParent() {
        return parent;
    }

    JGroup getJGroup() {
        return jgroup;
    }

    /**
     * Gets the tenant id list
     * @return the tenant id list, Collections.emptyList if no tenant exists.
     */
    @SuppressWarnings("unchecked")
    public static List<Long> getAllTenantIds() {
        checkIfTenantIsEnabled();
        if (null == tenantContainerMap) {
            throw new IllegalStateException("TenantContainer class is not initialized !");
        }
        if (tenantContainerMap.size() == 0) {
            return Collections.EMPTY_LIST;
        }

        List<Long> ret = new ArrayList<Long>();
        for(Map.Entry<Long, TenantContainer> entry : tenantContainerMap.entrySet()) {
            ret.add(entry.getValue().getTenantId());
        }
        return ret;
    }

    /**
     * Gets the TenantContainer attached to the current thread.
     * @return The TenantContainer attached to the current thread, null if no
     *         TenantContainer is attached to the current thread.
     */
    public static TenantContainer current() {
        checkIfTenantIsEnabled();
        return current0();
    }

    /**
     * Gets the cpu time consumed by this tenant
     * @return the cpu time used by this tenant, 0 if tenant cpu throttling or accounting feature is disabled.
     */
    public long getProcessCpuTime() {
        if (!TenantGlobals.isCpuAccountingEnabled()) {
            throw new IllegalStateException("-XX:+TenantCpuAccounting is not enabled");
        }
        long cpuTime = 0;
        if ( jgroup != null) {
            cpuTime = jgroup.getCpuTime();
        }
        return cpuTime;
    }

    /**
     * Gets the heap space occupied by this tenant
     * @return heap space occupied by this tenant, 0 if tenant heap isolation is disabled.
     * @throws IllegalStateException if -XX:+TenantHeapIsolation is not enabled.
     */
    public long getOccupiedMemory() {
        if (!TenantGlobals.isHeapIsolationEnabled()) {
            throw new IllegalStateException("-XX:+TenantHeapIsolation is not enabled");
        }
        return getTenantOccupiedMemory0();
    }

    private native long getTenantOccupiedMemory0();

    /**
     * Runs the code in the target tenant container
     * @param runnable the code to run
     */
    public void run(final Runnable runnable) throws TenantException {
        if (state == TenantState.DEAD || state == TenantState.STOPPING) {
            throw new TenantException("Tenant is dead");
        }
        // The current thread is already attached to tenant
        if (this == TenantContainer.current()) {
            runnable.run();
        } else {
            if (TenantContainer.current() != null) {
                throw new TenantException("must be in root tenant before running into non-root tenant.");
            }
            // attach to new tenant
            attach();
            try {
                runnable.run();
            } finally {
                detach();
            }
        }
    }

    /*
     * Get accumulatedMemory value of current thread
     */
    private long getThreadAllocatedMemory() {
        long[] memSizes = new long[1];
        getThreadsAllocatedMemory(null, memSizes);
        return memSizes[0];
    }

    private void attach() {
        // This is the first thread which runs in this tenant container
        if (getState() == TenantState.STARTING) {
            // move the tenant state to RUNNING
            this.setState(TenantState.RUNNING);
        }

        Thread curThread = Thread.currentThread();

        long curAllocBytes = getThreadAllocatedMemory();

        synchronized (this) {
            attachedThreads.add(curThread);

            accumulatedMemory -= curAllocBytes;

            if (jgroup != null) {
                jgroup.attach();
            }

            attach0();
        }
    }

    private void detach() {
        Thread curThread = Thread.currentThread();

        long curAllocBytes = getThreadAllocatedMemory();

        synchronized (this) {
            detach0();

            attachedThreads.remove(curThread);

            accumulatedMemory += curAllocBytes;

            if (jgroup != null) {
                jgroup.detach();
            }
        }
    }

    /*
     * Check if the tenant feature is enabled.
     */
    private static void checkIfTenantIsEnabled() {
        if (!TenantGlobals.isTenantEnabled()) {
            throw new UnsupportedOperationException("The multi-tenant feature is not enabled!");
        }
    }

    /*
     * Invoked by the VM to run a thread in multi-tenant mode.
     *
     * NOTE: please ensure relevant logic has been fully understood before changing any code
     *
     * @throws TenantException
     */
    private void runThread(final Thread thread) throws TenantException {
        if (destroyLock.readLock().tryLock()) {
            if (state != TenantState.STOPPING && state != TenantState.DEAD) {
                spawnedThreads.add(new WeakReference<>(thread));
                this.run(() -> {
                    destroyLock.readLock().unlock();
                    thread.run();
                });
            } else {
                destroyLock.readLock().unlock();
            }

            // try to clean up once
            if (destroyLock.readLock().tryLock()) {
                if (state != TenantState.STOPPING && state != TenantState.DEAD) {
                    spawnedThreads.removeIf(ref -> ref.get() == null || ref.get() == thread);
                }
                destroyLock.readLock().unlock();
            }
        } else {
            // shutdown in progress
            if (serviceThreads.containsKey(thread)) {
                // attach to current thread to run without registering
                attach0();
                try {
                    thread.run();
                } finally {
                    detach0();
                    removeServiceThread(thread);
                }
            }
        }
    }

    /*
     * Initialize the TenantContainer class, called after System.initializeSystemClass by VM.
     */
    private static void initializeTenantContainerClass() {
        //Initialize this field after the system is booted.
        tenantContainerMap = Collections.synchronizedMap(new HashMap<>());

        // initialize TenantAccess
        if (SharedSecrets.getTenantAccess() == null) {
            SharedSecrets.setTenantAccess(new TenantAccess() {
                @Override
                public void registerServiceThread(Object tenant, Thread thread) {
                    if (tenant != null && thread != null
                            && tenant instanceof TenantContainer) {
                        ((TenantContainer)tenant).addServiceThread(thread);
                    }
                }

                @Override
                public boolean threadInheritance() {
                    return TenantConfiguration.threadInheritance();
                }
            });
        }

        try {
            // force initialization of TenantConfiguration
            Class.forName("com.alibaba.tenant.TenantConfiguration");
            //Trigger the initialization of classes
            //we should fix it by modifying JVM, the bug is here: https://k3.alibaba-inc.com/issue/8094601
            if (TenantGlobals.isIOHandleReclaimingEnabled()) {
                Class.forName("sun.nio.ch.FileDispatcherImpl");
                Class.forName("java.net.PlainSocketImpl");
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        // initialize GCIH 2.0 heap eagerly to make it usable
        if (TenantGlobals.isTenantGCIHEnabled()) {
            try {
                Class.forName("com.taobao.gcih.GCInvisibleHeap");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Retrieve the tenant container where <code>obj</code> is allocated in
     * @param obj    object to be searched
     * @return       TenantContainer object whose memory space contains <code>obj</code>,
     *               or null if ROOT tenant container
     */
    public static TenantContainer containerOf(Object obj) {
        if (!TenantGlobals.isHeapIsolationEnabled()) {
            throw new UnsupportedOperationException("containerOf() only works with -XX:+TenantHeapIsolation");
        }
        return obj != null ? containerOf0(obj) : null;
    }

    private static native TenantContainer containerOf0(Object obj);

    /**
     * Gets the field value stored in the data repository of this tenant, which is same to call the
     * {@code TenantData.getFieldValue} on the tenant data object retrieved by {@code TenantContainer.getTenantData}.
     *
     * @param obj           Object the field associates with
     * @param fieldName     Field name
     * @param supplier      Responsible for creating the initial field value
     * @return              Value of field.
     */
    public <K, T> T getFieldValue(K obj, String fieldName, Supplier<T> supplier) {
        return tenantData.getFieldValue(obj, fieldName, supplier);
    }

    /**
     * Gets the field value stored in the data repository of this tenant, which is same to call the
     * {@code TenantData.getFieldValue} on the tenant data object retrieved by {@code TenantContainer.getTenantData}.
     *
     * @param obj           Object the field associates with
     * @param fieldName     Field name
     * @return              Value of field, null if not found
     */
    public <K, T> T getFieldValue(K obj, String fieldName) {
        return getFieldValue(obj, fieldName, () -> null);
    }

    private static native void maskTenantShutdown0();
    private static native void unmaskTenantShutdown0();

    /**
     * Hide current thread from TenantThreadStop request.
     * Should be used with {@code unmaskTenantShutdown} in pairs to mark a code snippet
     * to be immune to {@code TenantContainer.destroy}.
     *
     * A common pattern to use these two APIs would be
     * <pre>
     *     tenant.maskTenantShutdown();
     *     try {
     *         // Uninterruptible operation
     *         ... ...
     *     } finally {
     *         tenant.unmaskTenantShutdown();
     *     }
     * </pre>
     */
    public static void maskTenantShutdown() {
        if (TenantGlobals.isThreadStopEnabled()) {
            maskTenantShutdown0();
        }
    }

    /**
     * Restore current thread from {@code maskTenantShutdown}.
     * If {@code TenantContainer.destroy()} happens between
     * {@code maskTenantShutdown} and {@code unmaskTenantShutdown},
     * the "masked" thread will start external exit protocol
     * immediately after returning from {@code unmaskTenantShutdown}.
     */
    public static void unmaskTenantShutdown() {
        if (TenantGlobals.isThreadStopEnabled()) {
            unmaskTenantShutdown0();
        }
    }

    /**
     * Runs {@code Supplier.get} in the root tenant.
     * @param supplier target used to call
     * @return the result of {@code Supplier.get}
     */
    public static <T> T primitiveRunInRoot(Supplier<T> supplier) {
        // thread is already in root tenant.
        if(null == TenantContainer.current()) {
            return supplier.get();
        } else{
            TenantContainer tenant = TenantContainer.current();
            maskTenantShutdown();
            try {
                //Force to root tenant.
                tenant.detach0();
                try {
                    T t = supplier.get();
                    return t;
                } finally {
                    tenant.attach0();
                }
            } finally {
                unmaskTenantShutdown();
            }
        }
    }

    /**
     * Runs a block of code in the root tenant.
     * @param runnable the code to run
     */
    public static void primitiveRunInRoot(Runnable runnable) {
        // thread is already in root tenant.
        if(null == TenantContainer.current()) {
            runnable.run();
        } else{
            TenantContainer tenant = TenantContainer.current();
            maskTenantShutdown();
            try {
                //Force to root tenant.
                tenant.detach0();
                try {
                    runnable.run();
                } finally {
                    tenant.attach0();
                }
            } finally {
                unmaskTenantShutdown();
            }
        }
    }

    /**
     * Register a new tenant shutdown hook.
     * When the tenant begins its destroy it will
     * start all registered shutdown hooks in some unspecified order and let
     * them run concurrently.
     * @param   hook
     *          An initialized but unstarted <tt>{@link Thread}</tt> object
     */
    public void addShutdownHook(Thread hook) {
        addServiceThread(hook);
        tenantShutdownHooks.add(hook);
    }

    /**
     * De-registers a previously-registered tenant shutdown hook.
     * @param hook the hook to remove
     * @return true if the specified hook had previously been
     * registered and was successfully de-registered, false
     * otherwise.
     */
    public boolean removeShutdownHook(Thread hook) {
        removeServiceThread(hook);
        return tenantShutdownHooks.remove(hook);
    }

    // add a thread to the service thread list
    private void addServiceThread(Thread thread) {
        if (thread != null) {
            serviceThreads.put(thread, null);
        }
    }

    // remove a thread from the service thread list
    private void removeServiceThread(Thread thread) {
        serviceThreads.remove(thread);
    }

    // Dispose a tenant classloader and will never use it again
    private static void disposeTenantClassLoader(ClassLoader cl) {
        SharedSecrets.getJavaLangAccess().disposeTenantClassLoader(cl);
    }

    /**
     * Link a classloader object to this TenantContainer, the classloader will be marked
     * as 'dead' by calling {@code ClassLoader.dispose()} after this TenantContainer being destroyed
     * @param cl    the ClassLoader object to be associated
     */
    public void addTenantClassLoader(ClassLoader cl) {
        if (cl != null && tenantLoaders != null) {
            tenantLoaders.put(cl, null);
        }
    }

    /**
     * Try to modify resource limit of current tenant,
     * for resource whose limit cannot be changed after creation of {@code TenantContainer}, its limit will be ignored.
     * @param config  new TenantConfiguration to
     */
    public void update(TenantConfiguration config) {
        for (ResourceLimit rlimit : config.getAllLimits()) {
            // only save configurations of configurable
            if (rlimit.type().isJGroupResource() && jgroup != null) {
                rlimit.sync(jgroup);
                getConfiguration().setLimit(rlimit.type(), rlimit);
            }
        }
    }

    /**
     * Set the value of {@code Thread.tenantInheritance}
     * @param shouldInherit the new value
     */
    public static void setCurrentThreadInheritance(boolean shouldInherit) {
        if (TenantConfiguration.allowPerThreadInheritance()) {
            SharedSecrets.getJavaLangAccess()
                    .setChildShouldInheritTenant(Thread.currentThread(), shouldInherit);
        }
    }

    // Dump thread stacks of a group of threads
    private static native void dumpThreads(Thread[] threads);
}


