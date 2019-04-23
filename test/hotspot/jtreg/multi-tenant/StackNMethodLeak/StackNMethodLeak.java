
/*
 * @test
 * @summary Test memory leak caused by on-stack nmethod
 * @library /test/lib
 * @build sun.hotspot.WhiteBox HolderWrapper StackBlocker
 * @run driver ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm/timeout=60 -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+IgnoreUnrecognizedVMOptions
 *                              -XX:+WhiteBoxAPI -XX:-Inline -Xlog:gc*,class+unload=info -XX:+TraceEagerlyPurgeDeadOops
 *                              -Dcom.alibaba.tenant.DebugTenantShutdown=true -XX:+WizardMode -XX:+UseConcMarkSweepGC
 *                              -XX:+MultiTenant -XX:+TenantThreadStop StackNMethodLeak
 */

import static jdk.test.lib.Asserts.*;
import com.alibaba.tenant.TenantConfiguration;
import com.alibaba.tenant.TenantContainer;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import com.alibaba.tenant.TenantException;
import com.alibaba.tenant.TenantState;
import sun.hotspot.WhiteBox;


public class StackNMethodLeak {

    private static WhiteBox wb = WhiteBox.getWhiteBox();

    private static void warmUp(Runnable task) {
        for (int i = 0; i < 100000; ++i) {
            hold(task);
        }
    }

    // entry method to call into holerwrapper loaded by another classloader
    private static void hold(Runnable r) {
        r.run();
    }



    public static void main(String[] args) {
        // all init to null
        Class holderClass = null;
        Runnable holder = null;
        Class blockerClass = null;
        Runnable blocker = null;
        Field unblockedField = null;
        Field taskField = null;
        Field remField = null;
        Runnable rem = null;
        Thread blockerThread = null;

        // create a new tenantCotnainer, only used to dispose its associated classloader by calling destroy().
        TenantContainer tenant = TenantContainer.create(new TenantConfiguration());

        try {
            ClassLoader loader = getTestClassLoader();
            ClassLoader[] receiver_loaders = new ClassLoader[1];
            try {
                tenant.run(()->{
                    receiver_loaders[0] = getTestClassLoader();
                });
            } catch (TenantException e) {
                e.printStackTrace();
                fail();
            }
            ClassLoader dropLoader = receiver_loaders[0];
            receiver_loaders[0] = null;
            assertNotNull(dropLoader);

            try {
                blockerClass = loader.loadClass("StackBlocker");
                System.out.println("Successfully loaded class " + blockerClass + ", classloader = " + loader);
                blocker = (Runnable) blockerClass.newInstance();
                unblockedField = blockerClass.getDeclaredField("unblocked");
                unblockedField.setAccessible(true);
                unblockedField.setBoolean(blocker, true);

                holderClass = loader.loadClass("HolderWrapper");
                System.out.println("Successfully loaded class " + holderClass + ", classloader = " + loader);
                holder = (Runnable) holderClass.newInstance();
                taskField = holderClass.getDeclaredField("task");
                taskField.setAccessible(true);
                taskField.set(holder, blocker);

                remField = holderClass.getDeclaredField("rem");
                remField.setAccessible(true);

                Class c = dropLoader.loadClass("StackBlocker");
                System.out.println("Successfully loaded class " + c + ", classloader = " + dropLoader);
                rem = (Runnable) c.newInstance();

                remField.set(holder, rem);

            } catch (ClassNotFoundException | NoSuchFieldException
                    | InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
                fail();
            }

            assertTrue(holderClass != null && holder != null);
            assertTrue(blockerClass != null && blocker != null);
            try {
                assertFalse(wb.isMethodCompiled(blockerClass.getDeclaredMethod("run")));
                assertFalse(wb.isMethodCompiled(holderClass.getDeclaredMethod("run")));
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                fail();
            }

            warmUp(holder);

            CountDownLatch cdl = new CountDownLatch(1);
            AtomicBoolean stopped = new AtomicBoolean(false);

            // sleep a while to wait for runTask to be compiled
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // ensure required method are compiled
            try {
                assertTrue(wb.isMethodCompiled(blockerClass.getDeclaredMethod("run")));
                assertTrue(wb.isMethodCompiled(holderClass.getDeclaredMethod("run")));
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                fail();
            }

            // close the 'blocker', keep compiled HolderWrapper.run() on stack
            try {
                unblockedField.setBoolean(blocker, false);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                fail();
            }

            // spawn a new thread to keep runTask() on the stack, but this thread is not a TenantThread, thus cannot
            // be killed by TenantContainer.destroy();
            blockerThread = new BlockerThread(holder, cdl);
            blockerThread.start();

            try {
                cdl.await();
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                fail();
            }

            // two weakReferences to detect if target objects have been reclaimed by GC
            WeakReference<Object> objRef = new WeakReference<>(rem);
            WeakReference<ClassLoader> clRef = new WeakReference<>(dropLoader);

            // remove
            try {
                remField.set(holder, null);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                fail();
            }

            // Release all strong references
            dropLoader = null;
            holderClass = null;
            holder = null;
            blockerClass = null;
            blocker = null;
            unblockedField = null;
            rem = null;
            remField = null;

            // clear reference from on-stack NMethod
            tenant.destroy();
            assertEquals(tenant.getState(), TenantState.DEAD);

            // try to do GC
            for (int i = 0; i < 8; ++i) {
                System.gc();
                // sleep a while to interleave with StackBlocker.walkStack()
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    // ignore
                }
            }

            // ensure the testing thread is still running
            assertTrue(blockerThread.isAlive());

            /************************************************************************/
            assertTrue(objRef.get() == null); // <------- Will be reclaimed in JDK8
            assertTrue(clRef.get() == null);  // <------- 'dropLoader' Will be compiled into nmethod by C2 compiler,
                                              //          without AJDK fix D5884, this assertion will fail.
            /************************************************************************/
        } finally {
            restoreClasses();
        }
    }

    static class BlockerThread extends Thread {
        Runnable task;
        CountDownLatch cdl;

        BlockerThread(Runnable r, CountDownLatch c) {
            task = r;
            cdl = c;
            setDaemon(true);
        }

        @Override
        public void run() {
            cdl.countDown();
            task.run();
            fail();
        }
    }

    private static void fail() {
        assertTrue(false, "test failed!");
    }

    private static final String[] classNames = {"StackBlocker", "HolderWrapper"};

    private static final Map<String, ByteBuffer> classData;

    private static void initializeClassData() {
        ClassLoader curLoader = StackNMethodLeak.class.getClassLoader();
        String dir = new File(curLoader.getResource("StackNMethodLeak.class").getPath()).getParent();

        // Load all test class data to memory, and delete corresponding class files from disk to prevent AppClassLoader
        // from loading them.
        Arrays.stream(classNames).forEach(clsName -> {
            String curClassFilePath = dir + File.separator + clsName + ".class";
            File clsFile = new File(curClassFilePath);
            if (!clsFile.exists()) {
                throw new RuntimeException("source class file of " + curClassFilePath + " does not exist!");
            }

            ByteBuffer buf = ByteBuffer.allocate((int) clsFile.length());
            try (FileChannel fc = FileChannel.open(clsFile.toPath(), StandardOpenOption.READ)) {
                int read = 0;
                while (read < buf.capacity()) {
                    read += fc.read(buf);
                }
            } catch (IOException e) {
                e.printStackTrace();
                fail();
            }

            classData.put(clsName, buf);
            clsFile.delete(); // delete class file temporarily, will restore at exit
        });
    }

    private static ClassLoader getTestClassLoader() {
        try {
            Path dirPath = Files.createTempDirectory("classLoader");

            classData.keySet().forEach(clsName -> {
                String clsFilePath = dirPath.toString() + File.separator + clsName + ".class";
                ByteBuffer buf = classData.get(clsName);
                try (FileChannel fc = FileChannel.open(new File(clsFilePath).toPath(),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE)) {
                    int writen = 0;
                    buf.clear();
                    while (writen < buf.capacity()) {
                        writen += fc.write(buf);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    fail();
                }
            });

            URL[] urls = {dirPath.toUri().toURL()};
            System.out.println("url path = " + urls[0].toString());
            return URLClassLoader.newInstance(urls);
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }

        return null;
    }

    private static void restoreClasses() {
        ClassLoader curLoader = StackNMethodLeak.class.getClassLoader();
        String dir = new File(curLoader.getResource("StackNMethodLeak.class").getPath()).getParent();

        classData.keySet().forEach(clsName -> {
            String clsFilePath = dir + File.separator + clsName + ".class";
            ByteBuffer buf = classData.get(clsName);
            try (FileChannel fc = FileChannel.open(new File(clsFilePath).toPath(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE)) {
                int writen = 0;
                buf.clear();
                while (writen < buf.capacity()) {
                    writen += fc.write(buf);
                }
            } catch (IOException e) {
                e.printStackTrace();
                fail();
            }
        });
        System.out.println("Classes restored!");
    }

    static {
        classData = new HashMap<>();
        initializeClassData();
    }
}

