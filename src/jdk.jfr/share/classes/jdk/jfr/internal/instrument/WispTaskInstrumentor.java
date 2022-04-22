package jdk.jfr.internal.instrument;

import jdk.jfr.events.WispThreadParkEvent;

import java.util.concurrent.locks.LockSupport;

@JIInstrumentationTarget("com.alibaba.wisp.engine.WispTask")
public class WispTaskInstrumentor {
    private WispTaskInstrumentor() {}

    private volatile int jdkParkStatus;
    private volatile int jvmParkStatus;

    private Thread threadWrapper;

    @SuppressWarnings("deprecation")
    @JIInstrumentationMethod
    private void parkInternal(long timeoutNano, boolean fromJvm) {
        WispThreadParkEvent event = WispThreadParkEvent.EVENT.get();
        if (!event.isEnabled()) {
            parkInternal(timeoutNano, fromJvm);
            return;
        } else {
            try {
                event.begin();
                parkInternal(timeoutNano, fromJvm);
            } finally {
                if (timeoutNano != 0) {
                    event.timeout = timeoutNano;
                }
                event.fromJvm = fromJvm;
                event.currentJdkParkStatus = jdkParkStatus;
                event.currentJvmParkStatus = jvmParkStatus;
                if (threadWrapper != null) {
                    Object parkBlocker = LockSupport.getBlocker(threadWrapper);
                    if (parkBlocker != null) {
                        event.parkBlockerClass = parkBlocker.getClass();
                    }
                }
                event.commit();
                event.reset();
            }
        }
    }
}
