package jdk.jfr.startupargs;

import java.util.ArrayList;
import java.util.List;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedClassLoader;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.Asserts;
import jdk.test.lib.Platform;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;
import jdk.test.lib.management.DynamicVMOption;

/**
 * @test
 * @requires vm.hasJFR
 * @modules jdk.jfr/jdk.jfr.internal.test
 * @library /test/lib
 * @key jfr
 *
 * @run main/othervm -XX:+JFREnableEarlyNativeEventSupport jdk.jfr.startupargs.TestEarlyNativeEventSupport
 * @run main/othervm -XX:-JFREnableEarlyNativeEventSupport jdk.jfr.startupargs.TestEarlyNativeEventSupport
 */

public class TestEarlyNativeEventSupport {

    public static void main(String[] args) throws Exception {
        boolean enabled = Boolean.valueOf(DynamicVMOption.getString("JFREnableEarlyNativeEventSupport"));

        try (Recording recording = new Recording()) {
            recording.start();
            recording.stop();
            List<RecordedEvent> events = Events.fromRecording(recording);
            if (enabled && Platform.isDebugBuild()) {
                boolean found = false;
                for (RecordedEvent event : events) {
                    Asserts.assertTrue(event.getEventType().getName().equals(EventNames.ClassLoad));
                    System.out.println(event.getClass("loadedClass").getName());
                    found |= event.getClass("loadedClass").getName().equals("java.lang.Object")
                             && event.getThread().getId() == 1
                             && ((RecordedClassLoader)event.getValue("definingClassLoader")).getName().equals("bootstrap")
                             && ((RecordedClassLoader)event.getValue("initiatingClassLoader")).getName().equals("bootstrap");
                }
                Asserts.assertTrue(found);
            } else {
                Asserts.assertTrue(events.size() == 0);
            }
        }

        try (Recording recording = new Recording()) {
            recording.start();
            Dummy dummy = new Dummy();
            recording.stop();
            List<RecordedEvent> events = Events.fromRecording(recording);
            Asserts.assertTrue(events.size() == 0);
        }
    }

    static class Dummy {

    }
}

