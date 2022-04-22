package jdk.jfr.events;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.internal.Type;

@Name(Type.EVENT_NAME_PREFIX + "WispThreadPark")
@Label("Wisp ThreadPark")
@Category("Java Application")
@Description("Thread park with wisp enabled")
public class WispThreadParkEvent extends AbstractJDKEvent {
    public static final ThreadLocal<WispThreadParkEvent> EVENT =
            new ThreadLocal<>() {
                @Override protected WispThreadParkEvent initialValue() {
                    return new WispThreadParkEvent();
                }
            };

    @Label("Parked class")
    @Description("Parked class")
    public Class<?> parkBlockerClass;

    @Label("Timeout")
    @Description("Park timeout in nano")
    public long timeout;

    @Label("FromJvm")
    @Description("Park from jvm internal")
    public boolean fromJvm;

    @Label("JdkPark")
    @Description("Jdk park status")
    public int currentJdkParkStatus;

    @Label("JvmPark")
    @Description("Jvm park status")
    public int currentJvmParkStatus;

    public void reset() {
        parkBlockerClass = null;
        fromJvm = false;
        timeout = Long.MIN_VALUE;
        currentJvmParkStatus = 0;
        currentJdkParkStatus = 0;
    }
}
