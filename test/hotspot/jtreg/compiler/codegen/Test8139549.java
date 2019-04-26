import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

/*
 * @test
 * @summary Test for the bug fix of 8139549
 * @library /test/lib
 * @build Test8139549
 * @run main Test8139549
 */
public class Test8139549 {
    public static void main(String[] args) throws Exception {
        ProcessBuilder pb;
        OutputAnalyzer output;
        String srcPath = System.getProperty("test.class.path");

        pb = ProcessTools.createJavaProcessBuilder("-Xbootclasspath/a:.",
                "-cp", ".:" + srcPath,
                "-XX:+UnlockDiagnosticVMOptions",
                "-Xmn200M",
                "-Xmx1G",
                "-XX:CompileThreshold=100",
                "-XX:+PreserveFramePointer",
                "-XX:+PrintStubCode",
                TestRuntimeStubRunner.class.getName());
        output = new OutputAnalyzer(pb.start());
        output.shouldContain("Decoding RuntimeStub - _new_array_Java");
        output.shouldNotContain("add    $0x8,%rbp");
        output.shouldContain("done!");
        output.shouldHaveExitValue(0);
        System.out.println(output.getOutput());
    }

    public static class TestRuntimeStubRunner {
        private static int K = 1024;
        private static Object[] cache = new Object[K];
        public static Object newTypeArray() {
            return new int[K];
        }
        public static void newJavaArray(int size) {
            Object[] array = new Object[size];
            // avoid optimized out
            System.out.println(array);
        }
        public static void main(String[] args) throws Exception {
            for (int i = 0; i < 30000; i++) {
                cache[i % K] = newTypeArray();
            }
            System.out.println("done!");
        }
    }
}
