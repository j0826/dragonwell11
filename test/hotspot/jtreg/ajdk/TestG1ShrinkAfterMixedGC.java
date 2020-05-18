/*
 * @test TestG1ShrinkAfterMixedGC.java
 * @summary test G1ShrinkAfterMixedGC
 * @key gc
 * @requires vm.gc.G1
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @modules java.management
 * @build sun.hotspot.WhiteBox
 * @run driver ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+G1ShrinkAfterMixedGC -XX:+UnlockExperimentalVMOptions -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -verbose:gc -XX:SurvivorRatio=1 -Xmx100m -Xms20m -XX:MaxTenuringThreshold=1 -XX:InitiatingHeapOccupancyPercent=100 -XX:MaxGCPauseMillis=30000 -XX:G1HeapRegionSize=1m -XX:G1HeapWastePercent=0 -XX:G1MixedGCLiveThresholdPercent=100 -XX:GCTimeRatio=4 TestG1ShrinkAfterMixedGC
 */

import jdk.test.lib.Asserts;
import sun.hotspot.WhiteBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import static jdk.test.lib.Asserts.*;

import java.lang.management.*;

public class TestG1ShrinkAfterMixedGC {

    public static void main(String [] args) throws Exception {
        TestG1ShrinkAfterMixedGC t = new TestG1ShrinkAfterMixedGC();
        t.run();
    }

    public void run() throws Exception {
        GCTrigger gcTrigger = new GCTrigger();
        gcTrigger.allocateObjects();

        gcTrigger.triggerCM();

        MemoryUsage muAfterCM = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();

        gcTrigger.triggerMixedGC();
        MemoryUsage muAfterMixedGC = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        assertLessThan(muAfterMixedGC.getCommitted(), muAfterCM.getCommitted(), "Must be");
    }

    public class GCTrigger {
        private final WhiteBox WB = WhiteBox.getWhiteBox();
        private final List<byte[]> liveOldObjects = new ArrayList<>();

        public static final int ALLOCATION_SIZE = 10000;
        public static final int ALLOCATION_COUNT = 1000;

        public void allocateObjects() throws Exception {
            List<byte[]> deadOldObjects = new ArrayList<>();
            // Allocates buffer and promotes it to the old gen. Mix live and dead old
            // objects
            // Promote 10k * 6 * 1000 = 60m
            for (int i = 0; i < ALLOCATION_COUNT; i++) {
                liveOldObjects.add(new byte[ALLOCATION_SIZE]);
                deadOldObjects.add(new byte[ALLOCATION_SIZE]);
                deadOldObjects.add(new byte[ALLOCATION_SIZE]);
                deadOldObjects.add(new byte[ALLOCATION_SIZE]);
                deadOldObjects.add(new byte[ALLOCATION_SIZE]);
                deadOldObjects.add(new byte[ALLOCATION_SIZE]);
            }

            // Do two young collections, MaxTenuringThreshold=1 will force promotion.
            // G1HeapRegionSize=1m guarantees that old gen regions will be filled.
            WB.youngGC();
            Thread.sleep(10);
            WB.youngGC();
            Thread.sleep(10);
            // Check it is promoted & keep alive
            Asserts.assertTrue(WB.isObjectInOldGen(liveOldObjects),
                               "List of the objects is suppose to be in OldGen");
            Asserts.assertTrue(WB.isObjectInOldGen(deadOldObjects),
                               "List of the objects is suppose to be in OldGen");
        }

        /**
         * Waits until Concurent Mark Cycle finishes
         * @param wb  Whitebox instance
         * @param sleepTime sleep time
         */
        private void waitTillCMCFinished() throws Exception {
            while (WB.g1InConcurrentMark()) {
                Thread.sleep(10);
            }
        }

        public void triggerCM() throws Exception {
            waitTillCMCFinished();
            WB.g1StartConcMarkCycle();
            waitTillCMCFinished();
        }

        public void triggerMixedGC() throws Exception {
            WB.youngGC();
            Thread.sleep(100);
            WB.youngGC();
            Thread.sleep(100);
            WB.youngGC();
            Thread.sleep(200);
            WB.youngGC();
            Thread.sleep(200);
            WB.youngGC();
            Thread.sleep(200);
            WB.youngGC();
            Thread.sleep(200);
            WB.youngGC();
            Thread.sleep(100);
            WB.youngGC();
            // check that liveOldObjects still alive
            Asserts.assertTrue(WB.isObjectInOldGen(liveOldObjects),
                               "List of the objects is suppose to be in OldGen");
        }
    }
}
