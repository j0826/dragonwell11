import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

/*
 * @test
 * @summary Test PreserveFramePointer is enable by default on x86
 * @library /test/lib
 * @build TestPreserveFramePointerEnabledByDefault
 * @run main TestPreserveFramePointerEnabledByDefault
 */
public class TestPreserveFramePointerEnabledByDefault {
    public static void main(String[] args) throws Exception {
        ProcessBuilder pb;
        OutputAnalyzer output;
        String srcPath = System.getProperty("test.class.path");

        pb = ProcessTools.createJavaProcessBuilder("-Xbootclasspath/a:.",
                "-cp", ".:" + srcPath,
                "-XX:+PrintFlagsFinal",
                "-version");
        output = new OutputAnalyzer(pb.start());
        output.shouldContain("bool PreserveFramePointer                     = true");
        output.shouldHaveExitValue(0);
        System.out.println(output.getOutput());
    }
}
