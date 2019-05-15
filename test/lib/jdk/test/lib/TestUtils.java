package jdk.test.lib;

import java.lang.reflect.Method;
import java.util.Arrays;
import static jdk.test.lib.Asserts.fail;

public class TestUtils {

    /**
     * Run all methods of {@code clazz} whose names start with {@code prefix}
     * @param prefix
     * @param clazz
     * @param object
     */
    public static void runWithPrefix(String prefix, Class clazz, Object object) {
        if (prefix == null || clazz == null || object == null) {
            throw new IllegalArgumentException("Bad arguments");
        }

        long totalTests = Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> m.getName().startsWith(prefix))
                .count();
        long passed = 0;
        long failed = 0;

        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().startsWith(prefix)) {
                method.setAccessible(true);
                String name = clazz.getName() + "." + method.getName();
                println("=== Begin test " + name + " ===");
                try {
                    method.invoke(object);
                    ++passed;
                    println("=== PASSED (" + passed + " passed, " + failed +" failed, "
                            + totalTests + " total) ===");
                } catch (Throwable e) {
                    e.printStackTrace();
                    ++failed;
                    println("=== FAILED ( " + passed + " passed, " + failed +" failed"
                            + totalTests + " total) ===");
                }
            }
        }

        if (failed != 0) {
            fail("Total " + failed + "/" + totalTests + " testcases failed, class " + clazz.getName());
        } else {
            println("All " + totalTests + " testcases passed, class " + clazz.getName());
        }
    }

    private static void println(String msg) {
        System.err.println(msg);
        System.out.println(msg);
    }
}
