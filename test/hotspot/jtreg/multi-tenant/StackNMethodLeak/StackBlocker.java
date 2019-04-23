

// loaded by non-root loader, and called by HolderWrapper,
// its 'run' will not return even after deoptimization.
public class StackBlocker implements Runnable {

    private boolean unblocked = true;

    @Override
    public void run() {
        if (unblocked) {
            // do not block the stack
        } else {
            // block the stack, all callers will be kept on stack
            while (true) {
                try {
                    // try to retrieve stack trace
                    new Exception("Just to retrieve stacktrace").getStackTrace();
                    // sleep short time to avoid too many outputs
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }
    }
}
