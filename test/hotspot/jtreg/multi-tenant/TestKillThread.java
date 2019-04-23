
/*
 * @test
 * @summary Test function of killing tenants' threads
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm/timeout=100 -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+IgnoreUnrecognizedVMOptions
 *                               -XX:+TraceTenantKillThreads -XX:+MultiTenant -XX:+TenantThreadStop
 *                               -XX:+WhiteBoxAPI TestKillThread
 *
 */

import static jdk.test.lib.Asserts.*;
import com.alibaba.tenant.TenantConfiguration;
import com.alibaba.tenant.TenantContainer;
import com.alibaba.tenant.TenantException;
import com.alibaba.tenant.TenantState;
import sun.hotspot.WhiteBox;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Below testcase tries to test thread killing feature of MultiTenant JDK for following scenarios
 * <ul>A busy loop</ul>
 * <ul><code>Thread.getState() == WAITING </code></ul>
 * <ul>
 *   <li>{@link Object#wait() Object.wait} with no timeout</li>
 *   <li>{@link Thread#join} with no timeout</li>
 *   <li>{@link LockSupport#park() LockSupport.park}</li>
 * </ul>
 * <ul><code>Thread.getState() == TIMED_WAITING </code></ul>
 * <ul>
 *   <li>{@link Thread#sleep}</li>
 *   <li>{@link Object#wait(long) Object.wait} with timeout</li>
 *   <li>{@link Thread#join} with timeout</li>
 *   <li>{@link LockSupport#parkNanos LockSupport.parkNanos}</li>
 *   <li>{@link LockSupport#parkUntil LockSupport.parkUntil}</li>
 * </ul>
 */
public class TestKillThread {

    private static final WhiteBox wb;

    private static final int HEAP_REGION_SIZE;

    // used by testKillNewTenantThread to idicate that child thread has started and is ready to be killed
    private static volatile CountDownLatch cdl = null;

    // to hold information about runnable tasks
    private static class TaskInfo {
        boolean willWaiting;
        String name;
        Runnable task;
        TaskInfo(boolean wait, String nm, Runnable t) {
            willWaiting = wait;
            name        = nm;
            task        = t;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof TaskInfo) {
                return task.equals(((TaskInfo) obj).task);
            } else if (obj instanceof Runnable) {
                return task.equals((Runnable)obj);
            }
            return super.equals(obj);
        }
    }
    /*
     * Below Runnable objects are extracted code snippet to create special threading status, like waiting, parking,
     * deadlocking, etc, and they will be used in single-thread testing or threadpool/forkjoinpool.
     *
     * key points for adding new tasks
     * 1, private static member 'cdl' is used to signal that the testing threads are ready, and 'tenant.destroy()' may
     *      be called now.
     * 2, 'cdl' should be initialized by testing method, with the number of testing threads usually. and must be set to
     *      'null' when leaving testing method.
     * 3, extra assertions can be added via 'addExtraAddsertion()' method, to enable the runnables to register some
     *      checking statement after execution of Runnable.run().
     */

    // available runnable tasks.
    private static final List<TaskInfo> tasks = Collections.synchronizedList(new LinkedList<>());

    //------------------------ Non-blocking scenarios ----------------------------
    // code snippet which will cause runner thread to do busy loop
    private static final Runnable RUN_BUSY_LOOP = () -> {
        int i = 0;
        final int bounceLimit = 0xFFFF;

        if (cdl != null) cdl.countDown();

        while (true) {
            if (i < bounceLimit) {
                ++i;
            } else {
                i -= bounceLimit;
            }
        }
    };

    // task running in compiled code
    private static void decIt(long num) {
        while (0 != num--);
    }

    private static final Runnable RUN_COMPILED_BUSY_LOOP = () -> {
        // warmup
        for (int i = 0; i < 100000; ++i) {
            decIt(i);
        }
        msg("Warmup finished, executing in compiled loop");
        if (cdl != null) cdl.countDown();
        while (true) {
            decIt(0xFFFFFFFFl);
        }
    };

    //------------------------ blocked on WAITING state --------------------------
    // code snippet which causes runner thread to block on object.wait()
    private static final Runnable RUN_BLOCK_ON_WAIT = () -> {
        Object obj = new Object();
        synchronized (obj) {
            try {
                signalChildThreadReady();
                obj.wait();
                fail();
            } catch (InterruptedException e) {
                e.printStackTrace();
                fail();
            } catch (Throwable t) {
                t.printStackTrace();
                fail();
            } finally {
                System.out.println("finally!");
            }
        }
    };

    // code snippet which causes current thread to block on Thread.join()
    private static final Runnable RUN_BLOCK_ON_THREAD_JOIN = () -> {
        CountDownLatch childReady = new CountDownLatch(1);
        Thread child = new Thread(()->{
            while (true) {
                if (childReady.getCount() > 0) childReady.countDown();
                long l = 0;
                while (l++ < 8096);
            }
        });
        child.setName("THREAD_JOIN_child");
        child.start();
        try {
            childReady.await();
            signalChildThreadReady();
            child.join();
            fail();
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail();
        } catch (Throwable t) {
            t.printStackTrace();
            fail();
        } finally {
            System.out.println("finally!");
        }

        addExtraAssertTask(() -> {
            assertTrue(!child.isAlive() && child.getState() == Thread.State.TERMINATED);
        });
    };

    // code snippet which causes runner thread to block on object.wait()
    private static final Runnable RUN_BLOCK_ON_LOCKSUPPORT_PARK = () -> {
        Object obj = new Object();
        synchronized (obj) {
            try {
                signalChildThreadReady();
                LockSupport.park();
                // NOTE: Thread.interrupt() from TenantContainer.destroy() may wake up LockSupport.park() according to
                // Java spec, thus we do not put a 'fail()' here
            } catch (Throwable t) {
                t.printStackTrace();
                fail();
            } finally {
                System.out.println("finally!");
            }
        }
    };

    // threads blocked on CountDownLatch.await()
    private static final Runnable RUN_BLOCK_ON_COUNTDOWNLATCH_AWAIT = () -> {
        CountDownLatch c = new CountDownLatch(1);
        try {
            signalChildThreadReady();
            c.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail();
        }
    };

    private static final Runnable RUN_BLOCK_ON_COUNTDOWNLATCH_TIMED_AWAIT = () -> {
        CountDownLatch c = new CountDownLatch(1);
        try {
            signalChildThreadReady();
            c.await(30_000, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail();
        }
    };

    private static final Runnable RUN_BLOCK_ON_PROCESS_WAIT_FOR = () -> {
        final String[] commands = {"/usr/bin/id", "/bin/id", "id"};
        for (String c : commands) {
            try {
                Process p = Runtime.getRuntime().exec(c + " -u");
                signalChildThreadReady();
                p.waitFor();
            } catch (IOException e) {
                // ignore, commands may not be available on all machines
            } catch (InterruptedException e) {
                e.printStackTrace();
                fail();
            }
        }
    };

    //------------------------ blocked on TIMED_WAITING state --------------------------
    // code snippet which will cause runner thread to block on object.wait()
    private static final Runnable RUN_BLOCK_ON_TIMED_WAIT = () -> {
        Object obj = new Object();
        synchronized (obj) {
            while (true) {
                try {
                    signalChildThreadReady();
                    obj.wait(30_000);
                    // fail();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    fail();
                } catch (Throwable t) {
                    t.printStackTrace();
                    fail();
                } finally {
                    System.out.println("finally!");
                }
            }
        }
    };

    // Code snippet which causes current thread to block on Thread.sleep()
    private static final Runnable RUN_BLOCK_ON_SLEEP = () -> {
        while (true) {
            try {
                signalChildThreadReady();
                Thread.sleep(30_000);
                fail(); // it is unlikely to reach here after 30 seconds, the parent should kill it immediately
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                System.out.println("finally!");
            }
        }
    };

    // code snippet which causes current thread to block on Thread.join(time)
    private static final Runnable RUN_BLOCK_ON_THREAD_TIMED_JOIN = () -> {
        CountDownLatch childReady = new CountDownLatch(1);
        Thread child = new Thread(()->{
            while (true) {
                if (childReady.getCount() > 0) childReady.countDown();
                long l = 0;
                while (l++ < 512);
            }
        });
        child.setName("THREAD_TIMED_JOIN_child");
        child.start();
        try {
            try {
                childReady.await();
                signalChildThreadReady();
                child.join(30_000);
                // fail();
            } catch (InterruptedException e) {
                e.printStackTrace();
                fail();
            } finally {
                System.out.println("finally!");
            }
        } catch (Throwable t) {
            t.printStackTrace();
            fail();
        } finally {
            System.out.println("finally!");
        }

        addExtraAssertTask(() -> {
            assertTrue(!child.isAlive() && child.getState() == Thread.State.TERMINATED);
        });
    };

    // code snippet which causes runner thread to block on LockSupport.parkNanos()
    private static final Runnable RUN_BLOCK_ON_LOCKSUPPORT_PARK_NANOS = () -> {
        Object obj = new Object();
        try {
            signalChildThreadReady();
            LockSupport.parkNanos(30_000_000_000L);
            // NOTE: Thread.interrupt() from TenantContainer.destroy() may wake up LockSupport.park() according to
            // Java spec, thus we do not put a 'fail()' here
        } catch (Throwable t) {
            t.printStackTrace();
            fail();
        } finally {
            while (true);
        }
    };

    // code snippet which causes runner thread to block on LockSupport.parkUntil()
    private static final Runnable RUN_BLOCK_ON_LOCKSUPPORT_PARK_UNTIL = () -> {
        Object obj = new Object();
        long untilTime = System.currentTimeMillis() + 30_000;
        try {
            signalChildThreadReady();
            LockSupport.parkUntil(untilTime);
            // NOTE: Thread.interrupt() from TenantContainer.destroy() may wake up LockSupport.park() according to
            // Java spec, thus we do not put a 'fail()' here
        } catch (Throwable t) {
            t.printStackTrace();
            fail();
        } finally {
            System.out.println("finally!");
        }
    };

    //------------------------ blocked on I/O --------------------------
    private static final Runnable RUN_BLOCK_ON_STDIN = () -> {
        try {
            signalChildThreadReady();
            System.in.read();
        } catch (IOException e) {
            fail();
            e.printStackTrace();
        } finally {
            System.out.println("finally!");
        }
    };

    private static final Runnable RUN_BLOCK_ON_ACCEPT = () -> {
        try {
            ServerSocketChannel ssc = ServerSocketChannel.open();
            ssc.bind(new InetSocketAddress(0));
            ssc.configureBlocking(true);
            signalChildThreadReady();
            ssc.accept();
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        } finally {
            System.out.println("finally!");
        }
    };

    private static final Runnable RUN_BLOCK_ON_CONNECT = () -> {
        try {
            SocketChannel ch = SocketChannel.open();
            ch.configureBlocking(true);
            signalChildThreadReady();
            ch.connect(new InetSocketAddress("8.8.8.8", 80));
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        } finally {
            System.out.println("finally!");
        }
    };

    private static final Runnable RUN_BLOCK_ON_SELECT = () -> {
        try {
            Selector selector = Selector.open();

            ServerSocketChannel ssc = ServerSocketChannel.open();
            ssc.bind(new InetSocketAddress(0));
            ssc.configureBlocking(false);
            ssc.accept();
            ssc.register(selector, SelectionKey.OP_ACCEPT);

            SocketChannel ch = SocketChannel.open();
            ch.configureBlocking(false);
            ch.connect(new InetSocketAddress("8.8.8.8", 80));
            ch.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

            signalChildThreadReady();
            int num = selector.select();
            selector.selectedKeys().stream()
                    .map(k->"ops:" + k.interestOps())
                    .forEach(System.out::println);
            assertEquals(0, num);
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        } finally {
            System.out.println("finally!");
        }
    };

    private static final Runnable RUN_BLOCK_ON_RECV = () -> {
        try {
            DatagramChannel ch = DatagramChannel.open();
            ch.bind(new InetSocketAddress("127.0.0.1", 0));
            ch.configureBlocking(true);
            signalChildThreadReady();
            ch.receive(ByteBuffer.allocate(1024));
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    };

    private static final Runnable RUN_BLOCK_ON_UDP_READ = () -> {
        UDPSocketPair pair = new UDPSocketPair();
        DatagramChannel client = pair.clientEnd;
        try {
            signalChildThreadReady();
            client.read(ByteBuffer.allocate(1024));
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    };

    private static final Runnable RUN_BLOCK_ON_TCP_READ = () -> {
        TCPSocketPair pair = new TCPSocketPair();
        SocketChannel client = pair.clientEnd;
        try {
            signalChildThreadReady();
            // try to read from a connected socket pair
            client.configureBlocking(true);
            client.read(ByteBuffer.allocate(1024));
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
        addExtraAssertTask(()->{
            pair.cleanup();
        });
    };

    //----------------------- composed scenarios -------------------------
    private static final Runnable RUN_BASIC_EXCLUSIVE_LOCKING = () -> {
        final Lock lock = new ReentrantLock();
        CountDownLatch childrenReady = new CountDownLatch(2);
        Runnable exclusiveTask = ()-> {
            System.err.println("exclusive task" + Thread.currentThread());
            childrenReady.countDown();
            lock.lock();
            try {
                while (true);
            } finally {
                lock.unlock();
            }
        };

        Thread t1 = new Thread(exclusiveTask);
        t1.setName("Exclusive_lock_t1");
        t1.start();
        Thread t2 = new Thread(exclusiveTask);
        t2.setName("Exclusive_lock_t2");
        t2.start();
        try {
            childrenReady.await();
            signalChildThreadReady();
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail();
        }
    };

    // one lock holder and many acquirer
    private static final Runnable RUN_LOOP_AND_TRY_LOCK = () -> {
        Object lock = new Object();
        CountDownLatch holderReady = new CountDownLatch(1);
        Thread holder = new Thread(()-> {
            synchronized (lock) {
                holderReady.countDown();
                while (true);
                // never return
            }
        });
        holder.setName("LOCK_AND_TRY_holder");

        // many threads waiting for the lock
        int waiterCount = 64;
        Thread[] threads = new Thread[waiterCount];
        CountDownLatch testBegin = new CountDownLatch(waiterCount);
        for (int i = 0; i < waiterCount; ++i) {
            System.err.println("waiter "+ i +" started");
            threads[i] = new Thread(() -> {
                try {
                    System.err.println("Started thread:" + Thread.currentThread());
                    holderReady.await();
                    testBegin.countDown();
                    synchronized (lock) { // will be blocked here
                        fail();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    fail();
                }
            });
            threads[i].setName("LOCK_AND_TRY_getter_" + i);
            threads[i].start();
        }
        holder.start();

        System.err.println("testBegin="+testBegin.getCount());
        // wait until all testing threads started, then trigger tenant destroy
        try {
            testBegin.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail();
        }

        signalChildThreadReady();

        // extra assertions after 'tenant.destroy()'
        addExtraAssertTask(()-> {
            Arrays.stream(threads).sequential()
                    .forEach(thrd->assertFalse(thrd.isAlive()));

            Arrays.stream(threads).sequential().forEach(thrd->{
                try {
                    thrd.join();
                    holder.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    fail();
                }
            });

            assertTrue(holder.getState() == Thread.State.TERMINATED);
            Arrays.stream(threads).sequential()
                    .forEach(thrd->assertTrue(thrd.getState() == Thread.State.TERMINATED));
        });
    };

    // To test destroy of ForkJoinPool
    private static class BusyLoopAction extends RecursiveAction {
        private int cnt;
        private List<Thread> threads;
        private CountDownLatch countDown;
        BusyLoopAction(int cnt, List<Thread> thrds, CountDownLatch count) {
            this.cnt = cnt;
            this.threads = thrds;
            countDown = count;
        }

        @Override
        protected void compute() {
            if (cnt > 0) {
                BusyLoopAction task = new BusyLoopAction(cnt - 1, threads, countDown);
                task.fork();
                assertNotNull(threads);
                Thread curThread = Thread.currentThread();
                if (!threads.contains(curThread)) {
                    threads.add(curThread);
                }
                // below statement is against JavaSE's convention, just for testing purpose,
                // in realworld application, one may never want to put a infinite loop into ForkJoinPool a task like this
                Runnable t = ()->{
                    while (true);
                };
                countDown.countDown();
                t.run();
                task.join();
            }
        }
    };

    private static final Runnable RUN_BLOCK_THREADS_OF_FORKJOIN_POOL = () -> {
        List<Thread> threads = Collections.synchronizedList(new ArrayList<>(tasks.size()));
        int limit = tasks.size();
        ForkJoinPool myPool = new ForkJoinPool(limit);
        CountDownLatch childrenReady = new CountDownLatch(limit);
        myPool.submit(new BusyLoopAction(limit, threads, childrenReady));
        try {
            childrenReady.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail();
        } finally {
            signalChildThreadReady();
        }

        addExtraAssertTask(() -> {
            threads.stream().sequential().distinct()
                    .forEach(t -> assertFalse(t.isAlive()));
            threads.stream().sequential().distinct().forEach(t -> {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    fail();
                }
            });
            threads.stream().sequential().distinct()
                    .forEach(t -> assertEquals(t.getState(), Thread.State.TERMINATED));
        });
    };

    //------------------------- Testing entry ----------------------------------------------
    public static void main(String[] args) {
        TestKillThread test = new TestKillThread();
        test.testWaitingThreads();
        test.testNotKillRootThreads();
        test.testKillNewSingleThreadExecutorService();
        test.testKillNewTenantThreadPool();

    }

    private void testNotKillRootThreads() {
        msg(">> testNotKillRootThreads()");
        TenantConfiguration config = new TenantConfiguration(1024, 64 * 1024 * 1024);
        TenantContainer tenant = TenantContainer.create(config);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean endTest = new AtomicBoolean(false);
        AtomicBoolean safeEnd = new AtomicBoolean(false);

        // below thread is created in Root tenant, but attached to non-root tenant.
        // should not be impacted by TenantContainer.destroy();
        Thread threadInRoot = new Thread(() -> {
            try {
                tenant.run(() -> {
                    latch.countDown();
                    long l = 0;
                    while(!endTest.get()) { l++; }
                });
            } catch (Throwable t) {
                t.printStackTrace();
                fail();
            }
            safeEnd.set(true);
        });
        threadInRoot.start();

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail();
        }

        tenant.destroy();

        // thread should not be killed by 'tenant.destroy()'
        assertTrue(threadInRoot.isAlive());
        assertTrue(threadInRoot.getState() != Thread.State.TERMINATED);

        endTest.set(true);
        try {
            threadInRoot.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail();
        }

        assertTrue(safeEnd.get());

        msg("<< testNotKillRootThreads()");
    }

    private void testWaitingThreads() {
        tasks.stream().sequential()
                .forEach(this::testKillNewTenantThread);
    }

    // To test killing of independent tenant threads (non-threadpool)
    private void testKillNewTenantThread(TaskInfo ti) {
        msg(">> testKillNewTenantThread: task=" + ti.name +", time=" + LocalDateTime.now());

        setUp();

        // kill thread created from a tenant container;
        TenantConfiguration config = new TenantConfiguration(10, HEAP_REGION_SIZE << 3);
        TenantContainer tenant = TenantContainer.create(config);
        final Thread[] threads = new Thread[1];
        try {
            tenant.run(() -> {
                Thread thread = new Thread(()->{
                    System.err.println("new thread-->" + Thread.currentThread());
                    ti.task.run();
                });
                threads[0] = thread;
                thread.setName("test-thread");
                thread.start();
            });
        } catch (TenantException e) {
            e.printStackTrace();
            fail();
        } catch (Throwable t) {
            t.printStackTrace();
            fail();
        }

        // wait for ready signal from child-tenant thread
        waitChildrenThreadsToStart();

        // destroy tenant container
        System.err.println("BEfore destroy!");
        tenant.destroy();

        assertTrue(tenant.getState() == TenantState.DEAD);

        // check thread status
        assertFalse(threads[0].isAlive(), "thread not killed, task = " + ti.name);
        try {
            threads[0].join();
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail();
        }
        assertTrue(threads[0].getState() == Thread.State.TERMINATED, "Should be terminated after join()");
        assertNull(threads[0].getThreadGroup());
        assertNull(threads[0].getContextClassLoader());
        assertNull(getInheritedTenantContainer(threads[0]));

        executeExtraAssertTasks();

        msg("<< testKillNewTenantThread: task=" + ti.name +", time=" + LocalDateTime.now());
    }

    //------- thread pool related testing --------
    private void testKillNewSingleThreadExecutorService() {
        msg(">>testKillNewSingleThreadExecutorService" +", time=" + LocalDateTime.now());

        setUp();

        // kill thread created from a tenant container;
        TenantConfiguration config = new TenantConfiguration(10, HEAP_REGION_SIZE << 3);
        TenantContainer tenant = TenantContainer.create(config);

        List<Thread> poolThreads = new ArrayList<>(1);
        ExecutorService[] executors = new ExecutorService[1];

        try {
            tenant.run(()->{
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(()->{
                    System.out.println("in submitted task");
                    poolThreads.add(Thread.currentThread());
                    cdl.countDown();
                    while (true) ;
                });
                executor.shutdown();
                executors[0] = executor;
            });
        } catch (TenantException e) {
            e.printStackTrace();
            fail();
        }

        waitChildrenThreadsToStart();

        // thread should have been submitted successfully
        assertEquals(poolThreads.size(), 1);
        assertTrue(poolThreads.get(0).isAlive());

        tenant.destroy();

        // ThreadPoolExecutor.shutdownNow() is just too weak to kill its member threads, thus the size() info was not
        // updated after tenant.destroy(), ignore this assert for now.
        //assertEquals(poolThreads.size(), 0);
        assertTrue(!poolThreads.get(0).isAlive());

        poolThreads.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
                fail();
            }
        });
        assertTrue(poolThreads.get(0).getState() == Thread.State.TERMINATED);
        assertNull(poolThreads.get(0).getThreadGroup());
        assertNull(poolThreads.get(0).getContextClassLoader());
        assertNull(getInheritedTenantContainer(poolThreads.get(0)));
        executeExtraAssertTasks();

        msg("<<testKillNewSingleThreadExecutorService" +", time=" + LocalDateTime.now());
    }

    // new thread pool executor with growable number of worker threads
    private void testKillNewTenantThreadPool() {
        msg(">> testKillNewTenantThreadPool" +", time=" + LocalDateTime.now());

        setUp(tasks.size());

        TenantConfiguration config = new TenantConfiguration(10, HEAP_REGION_SIZE << 3);
        TenantContainer tenant = TenantContainer.create(config);
        final List<Thread> threads = new ArrayList<>();

        ExecutorService[] executors = new ExecutorService[1];
        try {
            tenant.run(() -> {
                // create a thread pool which will create new threads as needed
                ExecutorService executor = Executors.newCachedThreadPool();
                assertNotNull(executor);
                tasks.stream().sequential()
                        /* wrap the task with more code */
                        .map(t->(Runnable)()->{
                            threads.add(Thread.currentThread());
                            msg("submitted task started: task=" + t.name + "tenant="+TenantContainer.current());
                            t.task.run();
                        })
                        .forEach(executor::submit);
                executor.shutdown(); // submit all tasks,
                                     //  but the orderly shutdown operation will never end without tenant.destroy()
                executors[0] = executor;
            });
        } catch (TenantException e) {
            e.printStackTrace();
            fail();
        } catch (Throwable t) {
            t.printStackTrace();
            fail();
        }

        // wait for all task threads to count down
        waitChildrenThreadsToStart();

        // forcefully destroy tenant, all new threads created in new thread pool should be terminated
        msg("Trying to destroy tenant");
        tenant.destroy();

        assertTrue(tenant.getState() == TenantState.DEAD);

        // verify to confirm all threads terminated
        threads.stream().sequential()
                .distinct().filter(t -> t != null)
                .forEach(thread ->
                        assertFalse(thread.isAlive(), "pooled thread not killed"));

        // wait all child tasks to join
        threads.stream().sequential()
                .distinct().filter(t -> t != null)
                .forEach(thread -> {
                    try {
                        thread.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        fail();
                    }
                });

        threads.stream().sequential()
                .distinct().filter(t -> t != null)
                .forEach(thread ->
                        assertTrue(thread.getState() == Thread.State.TERMINATED, "Should be terminated after join"));
        threads.stream().sequential()
                .distinct().filter(t -> t != null)
                .forEach(thread -> {
                    assertNull(thread.getThreadGroup());
                    assertNull(thread.getContextClassLoader());
                    assertNull(getInheritedTenantContainer(thread));
                });

        executeExtraAssertTasks();

        msg("<< testKillNewTenantThreadPool" +", time=" + LocalDateTime.now());
    }

    // facilities to execute some extra assertions after child threads ended, NOTE: not thread safe;
    private static List<Runnable> extraAssertTasks = null;
    private static void initExtraAssertTasks() {
        extraAssertTasks = new ArrayList<>();
    }
    private static void addExtraAssertTask(Runnable task) {
        assertTrue(extraAssertTasks != null, "Should call initExtraAssertTask before add one");
        extraAssertTasks.add(task);
    }
    private static void executeExtraAssertTasks() {
        extraAssertTasks.stream().sequential().forEach(Runnable::run);
        cdl = null;
    }

    // facilities to coordinate main testing thread and test threads
    private static void setUp() {
        cdl = new CountDownLatch(1);
        initExtraAssertTasks();
    }
    private static void setUp(int count) {
        cdl = new CountDownLatch(count);
        initExtraAssertTasks();
    }
    // should be call by each child, testing threads, before entering blocking status
    private static void signalChildThreadReady() {
        if (cdl != null && cdl.getCount() > 0) {
            cdl.countDown();
        } else {
            msg("WARNING: main thread and testing threads are not synchronized, may leads to inaccuracy.");
        }
    }

    private static void waitChildrenThreadsToStart() {
        if (cdl != null) {
            try {
                cdl.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
                fail();
            }
        } else {
            msg("WARNING: main thread and testing threads are not synchronized, may leads to inaccuracy.");
        }
    }

    // extra testing utilities
    private static void fail() {
        msg("Failed thread is :" + Thread.currentThread());
        assertTrue(false, "Failed!");
    }
    private static void dumpAllThreadStacks() {
        Thread[] threads = new Thread[Thread.activeCount()];
        int cnt = Thread.enumerate(threads);
        msg("Dump threads:");
        for (int i = 0; i < cnt; ++i) {
            msg("Thread:"+ threads[i]);
            Arrays.stream(threads[i].getStackTrace()).sequential().map(s->"\t"+s)
                    .forEach(TestKillThread::msg);
        }
    }

    private static Field inheritedTenantContainerField;
    private static TenantContainer getInheritedTenantContainer(Thread t) {
        if (t != null && inheritedTenantContainerField != null) {
            try {
                return (TenantContainer) inheritedTenantContainerField.get(t);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                fail();
            }
        }
        return null;
    }

    // utilities class to create a pair of TCP sockets connected to each other
    static class TCPSocketPair {
        ServerSocketChannel server;
        SocketChannel       clientEnd;
        SocketChannel       serverEnd;
        TCPSocketPair() {
            try {
                server = ServerSocketChannel.open();
                server.bind(new InetSocketAddress(InetAddress.getLocalHost(), 0));
                server.configureBlocking(true);
                SocketChannel ch[] = new SocketChannel[1];
                Thread t = new Thread(()->{
                    try {
                        msg("Starting accept: tenant=" + TenantContainer.current());
                        ch[0] = server.accept();
                    } catch (IOException e) {
                        e.printStackTrace();
                        fail();
                    }
                });
                t.setName("TCP accepter");
                t.start();

                clientEnd = SocketChannel.open(server.getLocalAddress());
                clientEnd.configureBlocking(true);
                t.join();
                serverEnd = ch[0];

                assertNotNull(serverEnd);
                assertNotNull(clientEnd);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                fail();
            }
        }

        //  normally socket should have been closed by JVM, below code might not be needed
        void cleanup() {
            try {
                if (clientEnd != null) clientEnd.close();
                if (serverEnd != null) serverEnd.close();
                if (server != null) server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    static class UDPSocketPair {
        DatagramChannel serverEnd;
        DatagramChannel clientEnd;
        UDPSocketPair() {
            try {
                serverEnd = DatagramChannel.open();
                serverEnd.configureBlocking(true);
                serverEnd.bind(new InetSocketAddress(InetAddress.getLocalHost(), 0));
                clientEnd = DatagramChannel.open();
                clientEnd.configureBlocking(true);
                clientEnd.connect(serverEnd.getLocalAddress());
                assertNotNull(serverEnd);
                assertNotNull(clientEnd);
            } catch (IOException e) {
                e.printStackTrace();
                fail();
            }
        }

        void cleanup() {
            try {
                if (clientEnd != null) clientEnd.close();
                if (serverEnd != null) serverEnd.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static synchronized void msg(String s) {
        System.err.println("[" + LocalDateTime.now() + "] " + s);
    }

    static {
        wb = WhiteBox.getWhiteBox();
        HEAP_REGION_SIZE = wb.g1RegionSize();

        // initialize known runnables
        tasks.add(new TaskInfo(false, "BUSY_LOOP", RUN_BUSY_LOOP));
        tasks.add(new TaskInfo(false, "COMPILED_BUSY_LOOP", RUN_COMPILED_BUSY_LOOP));
        tasks.add(new TaskInfo(true, "BLOCK_ON_WAIT", RUN_BLOCK_ON_WAIT));
        tasks.add(new TaskInfo(true, "BLOCK_ON_TIMED_WAIT", RUN_BLOCK_ON_TIMED_WAIT));
        tasks.add(new TaskInfo(true, "BLOCK_ON_PROCESS_WAIT_FOR", RUN_BLOCK_ON_PROCESS_WAIT_FOR));
        tasks.add(new TaskInfo(true, "BLOCK_ON_SLEEP", RUN_BLOCK_ON_SLEEP));
        tasks.add(new TaskInfo(true, "BLOCK_ON_COUNTDOWNLATCH_AWAIT", RUN_BLOCK_ON_COUNTDOWNLATCH_AWAIT));
        tasks.add(new TaskInfo(true, "BLOCK_ON_COUNTDOWNLATCH_TIMED_AWAIT", RUN_BLOCK_ON_COUNTDOWNLATCH_TIMED_AWAIT));
        tasks.add(new TaskInfo(true, "BLOCK_ON_THREAD_JOIN", RUN_BLOCK_ON_THREAD_JOIN));
        tasks.add(new TaskInfo(true, "BLOCK_ON_THREAD_TIMED_JOIN", RUN_BLOCK_ON_THREAD_TIMED_JOIN));
        tasks.add(new TaskInfo(true, "BLOCK_ON_LOCKSUPPORT_PARK", RUN_BLOCK_ON_LOCKSUPPORT_PARK));
        tasks.add(new TaskInfo(true, "BLOCK_ON_LOCKSUPPORT_PARK_NANOS", RUN_BLOCK_ON_LOCKSUPPORT_PARK_NANOS));
        tasks.add(new TaskInfo(true, "BLOCK_ON_LOCKSUPPORT_PARK_UNTIL", RUN_BLOCK_ON_LOCKSUPPORT_PARK_UNTIL));
        //tasks.add(new TaskInfo(true, "BLOCK_ON_IO", RUN_BLOCK_ON_STDIN)); // unsupported for now
        tasks.add(new TaskInfo(true, "BLOCK_ON_ACCEPT", RUN_BLOCK_ON_ACCEPT));
        tasks.add(new TaskInfo(true, "BLOCK_ON_CONNECT", RUN_BLOCK_ON_CONNECT));
        tasks.add(new TaskInfo(true, "BLOCK_ON_SELECT", RUN_BLOCK_ON_SELECT));
        tasks.add(new TaskInfo(true, "BLOCK_ON_RECV", RUN_BLOCK_ON_RECV));
        tasks.add(new TaskInfo(true, "BLOCK_ON_UDP_READ", RUN_BLOCK_ON_UDP_READ));
        tasks.add(new TaskInfo(true, "BLOCK_ON_TCP_READ", RUN_BLOCK_ON_TCP_READ));
        tasks.add(new TaskInfo(true, "BASIC_EXCLUSIVE_LOCKING", RUN_BASIC_EXCLUSIVE_LOCKING));
        tasks.add(new TaskInfo(true, "LOOP_AND_TRY_LOCK", RUN_LOOP_AND_TRY_LOCK));
        tasks.add(new TaskInfo(true, "BLOCK_THREADS_OF_FORKJOIN_POOL", RUN_BLOCK_THREADS_OF_FORKJOIN_POOL));

        try {
            inheritedTenantContainerField = Thread.class.getDeclaredField("inheritedTenantContainer");
            inheritedTenantContainerField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            fail();
        }
    }
}
