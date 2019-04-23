
// loaded by non-root loader, but called by compiled root methods
public class HolderWrapper implements Runnable {
    private Runnable rem;
    private Runnable task;

    // Below method will be compiled and kept in the middle of Java stack, and any oops compiled into its nmethod
    // will be used as GC root
    @Override
    public void run() {
        if (null != rem) {
            rem.run();
        }
        if (null != task) {
            task.run();
        }
    }
}
