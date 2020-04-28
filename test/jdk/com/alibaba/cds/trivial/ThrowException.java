package trivial;

import java.lang.StackWalker.StackFrame;

public class ThrowException {
    public static void throwError() {
        System.out.println("throwError");
        throw new Error("testing");
    }
}
