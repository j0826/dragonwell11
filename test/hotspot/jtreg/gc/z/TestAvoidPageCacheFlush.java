
package gc.z;

/*
 * @test TestHighUsageLargeHeap
 * @requires vm.gc.Z & !vm.graal.enabled
 * @summary Test ZGC Avoid "Page Cache Flush" by Enabling -XX:+ZBalancePageCache
 * @library /test/lib
 * @run main/othervm/timeout=600 gc.z.TestAvoidPageCacheFlush
 */

import java.util.Random;
import java.util.concurrent.locks.LockSupport;

import jdk.test.lib.Platform;
import jdk.test.lib.process.ProcessTools;

public class TestAvoidPageCacheFlush {

    public static void main(String[] args) throws Exception {
        if (Platform.isSlowDebugBuild()) {
            return; // ignore slow debug build because allocation is too fast for garbage collection
        }

        ProcessTools.executeTestJvm(new String[]{"-XX:+UnlockExperimentalVMOptions",
                                                 "-XX:+UseZGC",
                                                 "-XX:+UnlockDiagnosticVMOptions",
                                                 "-Xms4g",
                                                 "-Xmx4g",
                                                 "-XX:ParallelGCThreads=2",
                                                 "-XX:ConcGCThreads=4",
                                                 "-XX:+ZBalancePageCache", // page cache flush if this line is removed
                                                 "-Xlog:gc,gc+heap",
                                                 AvoidPageCacheFlush.class.getName()})
                                                 .shouldNotContain("Allocation Stall")
                                                 .shouldNotContain("Page Cache Flushed")
                                                 .shouldHaveExitValue(0);
    }

    static class AvoidPageCacheFlush {
        private static final int THREADS_NUM = 12;

        public static void main(String[] args) {
            Thread[] w = new Thread[THREADS_NUM];
            for (int i = 0; i < THREADS_NUM; i++) {
                w[i] = new AllocationThread();
                w[i].start();
            }
            for (int i = 0; i < THREADS_NUM; i++) {
                try {
                    w[i].join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static class AllocationThread extends Thread {
        SmallContainer smallContainer;
        MediumContainer mediumContainer;

        public AllocationThread() {
            this.smallContainer = new SmallContainer();
            this.mediumContainer = new MediumContainer();
        }

        /*
         * small object: fast allocation
         * medium object: slow allocation, fast accessing, abrupt deletion
         */
        public void run() {
            int i = 0, j = 0, addedMediumObjectCount = 0;
            while (true) {
                smallContainer.createAndSaveObject();
                mediumContainer.accessObject(10); // frequently accessing medium object
                i++;
                if (i == 150) {
                    LockSupport.parkNanos(10); // make allocation slower
                    i = 0;
                }
                j++;
                if (j == 500000) {
                    mediumContainer.createAndAppendObject();
                    j = 0;
                    addedMediumObjectCount++;
                    if (addedMediumObjectCount > 1.5 * MediumContainer.MEDIUM_OBJ_LIMIT) {
                        break;
                    }
                }
            }
        }
    }

    static class SmallContainer {
        private final static int SMALL_OBJ_LIMIT = 200000;
        private final Random RANDOM = new Random();

        private byte[][] smallObjectArray = new byte[SMALL_OBJ_LIMIT][];

        // random insertion (with random deletion)
        void createAndSaveObject() {
            smallObjectArray[RANDOM.nextInt(SMALL_OBJ_LIMIT)] = new byte[200];
        }
    }

    static class MediumContainer {
        public final static int MEDIUM_OBJ_LIMIT = 30;
        private final static Random RANDOM = new Random();

        private byte[][] mediumObjectArray = new byte[MEDIUM_OBJ_LIMIT][];
        private int mediumObjectArrayCurrentIndex = 0;

        void createAndAppendObject() {
            if (mediumObjectArrayCurrentIndex == MEDIUM_OBJ_LIMIT) {
                dropAnArray(); // delete a lot of medium objects in an operations
                mediumObjectArrayCurrentIndex = 0;
            } else {
                mediumObjectArray[mediumObjectArrayCurrentIndex] = new byte[2 << 20]; // 2 MB
                mediumObjectArrayCurrentIndex ++;
            }
        }

        byte[] accessObject(long x) {
            if (mediumObjectArrayCurrentIndex == 0) {
                return null;
            }
            return mediumObjectArray[RANDOM.nextInt(mediumObjectArrayCurrentIndex)];
        }

        private void dropAnArray() {
            // in the real environment, the old mediumObjectArray may be dumped to the disk
            mediumObjectArray = new byte[MEDIUM_OBJ_LIMIT][]; // also delete all medium objects in the old array
            mediumObjectArrayCurrentIndex = 0;
        }
    }
}
