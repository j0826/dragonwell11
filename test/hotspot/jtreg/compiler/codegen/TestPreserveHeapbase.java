package compiler.codegen;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

/*
 * @test
 * @summary Test PreserveHeapbase
 * @library /test/lib
 * @build TestPreserveHeapbase
 * @run main compiler.codegen.TestPreserveHeapbase
 */
public class TestPreserveHeapbase {
    public static void main(String[] args) throws Exception {
        ProcessBuilder pb;
        OutputAnalyzer output;
        String srcPath = System.getProperty("test.class.path");
        String cname = RegPressure.class.getName();
        /* aarch64 only */
        if ( false == System.getProperty("os.arch").equals("aarch64")) {
            return;
        }

        // disable PreserveHeapBase
        pb = ProcessTools.createJavaProcessBuilder(
                "-cp", ".:" + srcPath,
                "-XX:+UseCompressedOops",
                "-XX:-UseSharedSpaces",
                "-XX:-PreserveHeapBase",
                "-Xlog:coops=debug",
                "-Xcomp",
                "-XX:-TieredCompilation",
                "-XX:+PrintCompilation",
                "-XX:CompileOnly="+cname+"::test",
                "-XX:CompileCommand=print,"+cname+"::test",
                cname);
        output = new OutputAnalyzer(pb.start());
        output.shouldContain("PreserveHeapBase is disabled");
        output.shouldContain("w27");  // heapbase w27 is used
        output.shouldHaveExitValue(0);
        System.out.println(output.getOutput());

        // enable PreserveHeapBase
        pb = ProcessTools.createJavaProcessBuilder(
                "-cp", ".:" + srcPath,
                "-XX:+UseCompressedOops",
                "-XX:-UseSharedSpaces",
                "-XX:+PreserveHeapBase",
                "-Xlog:coops=debug",
                "-Xcomp",
                "-XX:-TieredCompilation",
                "-XX:+PrintCompilation",
                "-XX:CompileOnly="+cname+"::test",
                "-XX:CompileCommand=print,"+cname+"::test",
                cname);
        output = new OutputAnalyzer(pb.start());
        output.shouldContain("PreserveHeapBase is enabled");
        output.shouldNotContain("w27");  // heapbase w27 is not used
        output.shouldHaveExitValue(0);
        System.out.println(output.getOutput());

        // PreserveHeapBase is enabled by klass point encode
        pb = ProcessTools.createJavaProcessBuilder(
                "-cp", ".:" + srcPath,
                "-Xms20m",
                "-Xmx20m",
                "-XX:+DumpSharedSpaces",
                "-XX:SharedBaseAddress=0x800000000",
                "-XX:-PreserveHeapBase",
                "-Xlog:coops=debug",
                "-Xlog:gc+metaspace=trace",
                cname);
        output = new OutputAnalyzer(pb.start());
        output.shouldHaveExitValue(0);
        output.shouldContain("PreserveHeapBase is disabled");
        pb = ProcessTools.createJavaProcessBuilder(
                "-cp", ".:" + srcPath,
                "-Xms20m",
                "-Xmx20m",
                "-XX:+DumpSharedSpaces",
                "-XX:SharedBaseAddress=0x800010000",  // change the base address
                "-XX:-PreserveHeapBase",
                "-Xlog:coops=debug",
                "-Xlog:gc+metaspace=trace",
                cname);
        output = new OutputAnalyzer(pb.start());
        output.shouldHaveExitValue(0);
        output.shouldContain("PreserveHeapBase is enabled");
        System.out.println(output.getOutput());
    }

    public static class RegPressure {
        int f0;
        int f1;
        int f2;
        int f3;
        int f4;
        int f5;
        int f6;
        int f7;
        int f8;
        int f9;
        int f10;
        int f11;
        int f12;
        int f13;
        int f14;
        int f15;
        int f16;
        int f17;
        int f18;
        int f19;
        int f20;
        int f21;
        int f22;
        int f23;
        int f24;
        int f25;
        int f26;
        int f27;
        int f28;
        int f29;
        int f30;
        int f31;

        static RegPressure tobj = new RegPressure();

        public static void main(String args[]){
            test(tobj);
        }

        public static int test(RegPressure p) {
            p.f0=p.f1*p.f2*p.f3&p.f4*p.f5|p.f6-p.f7^p.f8+p.f9&p.f10-p.f11&p.f12+p.f13+p.f14|p.f15+p.f16-p.f17|p.f18|p.f19&p.f20&p.f21-p.f22-p.f23+p.f24&p.f25+p.f26-p.f27&p.f28|p.f29+p.f30*p.f31;
            p.f1=p.f0^p.f2+p.f3&p.f4&p.f5*p.f6&p.f7*p.f8|p.f9|p.f10-p.f11|p.f12-p.f13|p.f14+p.f15*p.f16^p.f17-p.f18+p.f19*p.f20|p.f21|p.f22&p.f23^p.f24*p.f25*p.f26|p.f27^p.f28-p.f29-p.f30|p.f31;
            p.f2=p.f0*p.f1+p.f3&p.f4-p.f5-p.f6-p.f7|p.f8+p.f9^p.f10&p.f11+p.f12+p.f13|p.f14+p.f15*p.f16*p.f17-p.f18&p.f19-p.f20-p.f21*p.f22|p.f23-p.f24*p.f25*p.f26+p.f27|p.f28&p.f29+p.f30|p.f31;
            p.f3=p.f0|p.f1^p.f2|p.f4+p.f5^p.f6|p.f7|p.f8+p.f9&p.f10|p.f11-p.f12&p.f13|p.f14^p.f15+p.f16*p.f17&p.f18^p.f19+p.f20^p.f21*p.f22|p.f23^p.f24+p.f25^p.f26-p.f27^p.f28+p.f29&p.f30|p.f31;
            p.f4=p.f0+p.f1*p.f2^p.f3*p.f5-p.f6^p.f7*p.f8^p.f9&p.f10+p.f11+p.f12^p.f13+p.f14^p.f15+p.f16^p.f17*p.f18|p.f19^p.f20&p.f21&p.f22&p.f23-p.f24-p.f25*p.f26+p.f27^p.f28^p.f29-p.f30-p.f31;
            p.f5=p.f0^p.f1^p.f2-p.f3&p.f4^p.f6&p.f7&p.f8^p.f9&p.f10+p.f11^p.f12&p.f13|p.f14*p.f15*p.f16^p.f17&p.f18-p.f19|p.f20-p.f21-p.f22*p.f23|p.f24&p.f25-p.f26|p.f27&p.f28-p.f29-p.f30+p.f31;
            p.f6=p.f0|p.f1-p.f2&p.f3*p.f4+p.f5^p.f7-p.f8*p.f9+p.f10*p.f11*p.f12*p.f13^p.f14^p.f15&p.f16-p.f17|p.f18|p.f19+p.f20-p.f21|p.f22*p.f23^p.f24&p.f25|p.f26|p.f27|p.f28|p.f29+p.f30*p.f31;
            p.f7=p.f0-p.f1&p.f2|p.f3*p.f4&p.f5*p.f6^p.f8-p.f9^p.f10^p.f11^p.f12-p.f13+p.f14^p.f15-p.f16*p.f17-p.f18-p.f19*p.f20-p.f21|p.f22*p.f23-p.f24&p.f25-p.f26|p.f27+p.f28|p.f29*p.f30|p.f31;
            p.f8=p.f0-p.f1+p.f2*p.f3&p.f4-p.f5|p.f6|p.f7*p.f9*p.f10^p.f11^p.f12&p.f13^p.f14|p.f15&p.f16&p.f17|p.f18-p.f19^p.f20|p.f21*p.f22+p.f23&p.f24&p.f25|p.f26&p.f27&p.f28|p.f29*p.f30*p.f31;
            p.f9=p.f0&p.f1^p.f2-p.f3*p.f4^p.f5*p.f6^p.f7-p.f8*p.f10^p.f11+p.f12*p.f13|p.f14+p.f15|p.f16|p.f17&p.f18^p.f19+p.f20*p.f21^p.f22|p.f23+p.f24|p.f25*p.f26-p.f27&p.f28*p.f29*p.f30-p.f31;
            p.f10=p.f0|p.f1+p.f2^p.f3|p.f4-p.f5|p.f6-p.f7&p.f8&p.f9*p.f11+p.f12&p.f13^p.f14-p.f15|p.f16^p.f17-p.f18*p.f19|p.f20+p.f21^p.f22-p.f23&p.f24&p.f25*p.f26-p.f27^p.f28-p.f29|p.f30|p.f31;
            p.f11=p.f0|p.f1&p.f2*p.f3^p.f4&p.f5|p.f6|p.f7|p.f8&p.f9+p.f10-p.f12-p.f13^p.f14+p.f15-p.f16+p.f17+p.f18+p.f19&p.f20-p.f21*p.f22-p.f23|p.f24+p.f25|p.f26^p.f27^p.f28^p.f29*p.f30*p.f31;
            p.f12=p.f0+p.f1*p.f2|p.f3^p.f4^p.f5+p.f6*p.f7*p.f8^p.f9+p.f10^p.f11^p.f13^p.f14&p.f15&p.f16|p.f17&p.f18+p.f19*p.f20-p.f21|p.f22-p.f23-p.f24-p.f25*p.f26&p.f27*p.f28-p.f29*p.f30|p.f31;
            p.f13=p.f0-p.f1^p.f2|p.f3|p.f4^p.f5-p.f6|p.f7&p.f8-p.f9+p.f10+p.f11&p.f12^p.f14^p.f15-p.f16|p.f17&p.f18+p.f19+p.f20+p.f21^p.f22|p.f23*p.f24^p.f25+p.f26+p.f27*p.f28^p.f29*p.f30-p.f31;
            p.f14=p.f0+p.f1*p.f2&p.f3-p.f4|p.f5+p.f6&p.f7^p.f8-p.f9&p.f10|p.f11&p.f12+p.f13&p.f15&p.f16*p.f17-p.f18*p.f19-p.f20+p.f21*p.f22+p.f23&p.f24*p.f25+p.f26+p.f27&p.f28^p.f29-p.f30*p.f31;
            p.f15=p.f0^p.f1+p.f2&p.f3*p.f4*p.f5*p.f6&p.f7+p.f8*p.f9|p.f10|p.f11|p.f12+p.f13*p.f14+p.f16^p.f17|p.f18+p.f19+p.f20+p.f21+p.f22^p.f23*p.f24-p.f25&p.f26|p.f27+p.f28|p.f29+p.f30|p.f31;
            p.f16=p.f0|p.f1+p.f2*p.f3+p.f4-p.f5&p.f6|p.f7^p.f8&p.f9|p.f10-p.f11+p.f12*p.f13^p.f14&p.f15&p.f17*p.f18*p.f19^p.f20+p.f21*p.f22|p.f23+p.f24*p.f25|p.f26|p.f27|p.f28*p.f29^p.f30-p.f31;
            p.f17=p.f0+p.f1+p.f2-p.f3^p.f4&p.f5|p.f6^p.f7+p.f8-p.f9&p.f10^p.f11-p.f12|p.f13|p.f14*p.f15-p.f16*p.f18^p.f19^p.f20+p.f21+p.f22^p.f23-p.f24-p.f25*p.f26+p.f27|p.f28-p.f29+p.f30-p.f31;
            p.f18=p.f0^p.f1*p.f2^p.f3&p.f4|p.f5-p.f6*p.f7-p.f8+p.f9&p.f10^p.f11^p.f12|p.f13+p.f14*p.f15*p.f16&p.f17|p.f19|p.f20-p.f21^p.f22+p.f23*p.f24^p.f25|p.f26*p.f27+p.f28+p.f29*p.f30&p.f31;
            p.f19=p.f0-p.f1+p.f2*p.f3|p.f4^p.f5*p.f6&p.f7^p.f8-p.f9-p.f10-p.f11|p.f12-p.f13|p.f14&p.f15|p.f16*p.f17&p.f18&p.f20&p.f21*p.f22|p.f23+p.f24+p.f25&p.f26|p.f27|p.f28|p.f29-p.f30+p.f31;
            p.f20=p.f0|p.f1+p.f2&p.f3-p.f4^p.f5-p.f6&p.f7^p.f8|p.f9|p.f10|p.f11*p.f12&p.f13^p.f14*p.f15-p.f16^p.f17&p.f18&p.f19+p.f21|p.f22&p.f23&p.f24^p.f25*p.f26+p.f27&p.f28*p.f29+p.f30+p.f31;
            p.f21=p.f0*p.f1+p.f2&p.f3+p.f4+p.f5-p.f6-p.f7&p.f8-p.f9-p.f10&p.f11^p.f12-p.f13-p.f14^p.f15*p.f16|p.f17-p.f18^p.f19|p.f20|p.f22|p.f23+p.f24-p.f25*p.f26|p.f27*p.f28|p.f29*p.f30+p.f31;
            p.f22=p.f0-p.f1|p.f2*p.f3-p.f4&p.f5^p.f6+p.f7*p.f8+p.f9*p.f10&p.f11*p.f12-p.f13+p.f14&p.f15|p.f16|p.f17-p.f18^p.f19-p.f20*p.f21-p.f23^p.f24+p.f25+p.f26|p.f27^p.f28^p.f29-p.f30&p.f31;
            p.f23=p.f0|p.f1+p.f2^p.f3^p.f4|p.f5&p.f6*p.f7&p.f8&p.f9^p.f10|p.f11*p.f12*p.f13&p.f14^p.f15*p.f16^p.f17|p.f18&p.f19&p.f20|p.f21|p.f22*p.f24^p.f25*p.f26*p.f27+p.f28&p.f29&p.f30-p.f31;
            p.f24=p.f0|p.f1&p.f2|p.f3-p.f4|p.f5|p.f6-p.f7-p.f8+p.f9*p.f10|p.f11&p.f12+p.f13&p.f14+p.f15&p.f16*p.f17|p.f18|p.f19&p.f20^p.f21|p.f22^p.f23+p.f25&p.f26^p.f27|p.f28^p.f29|p.f30-p.f31;
            p.f25=p.f0|p.f1-p.f2&p.f3^p.f4-p.f5&p.f6*p.f7*p.f8&p.f9^p.f10+p.f11+p.f12|p.f13*p.f14*p.f15^p.f16&p.f17|p.f18+p.f19&p.f20^p.f21|p.f22|p.f23+p.f24-p.f26+p.f27-p.f28*p.f29&p.f30-p.f31;
            p.f26=p.f0-p.f1+p.f2^p.f3|p.f4+p.f5&p.f6-p.f7&p.f8&p.f9-p.f10^p.f11|p.f12+p.f13+p.f14&p.f15*p.f16*p.f17-p.f18+p.f19&p.f20*p.f21+p.f22*p.f23^p.f24*p.f25*p.f27&p.f28&p.f29*p.f30|p.f31;
            p.f27=p.f0|p.f1&p.f2+p.f3+p.f4&p.f5^p.f6*p.f7+p.f8&p.f9*p.f10-p.f11+p.f12&p.f13+p.f14*p.f15-p.f16*p.f17^p.f18+p.f19&p.f20-p.f21&p.f22*p.f23^p.f24*p.f25-p.f26^p.f28*p.f29+p.f30&p.f31;
            p.f28=p.f0|p.f1-p.f2|p.f3|p.f4^p.f5-p.f6^p.f7^p.f8&p.f9^p.f10+p.f11&p.f12-p.f13+p.f14-p.f15|p.f16*p.f17^p.f18|p.f19-p.f20^p.f21*p.f22^p.f23^p.f24^p.f25^p.f26&p.f27^p.f29|p.f30|p.f31;
            p.f29=p.f0-p.f1|p.f2|p.f3^p.f4*p.f5^p.f6+p.f7|p.f8*p.f9*p.f10+p.f11-p.f12|p.f13-p.f14*p.f15|p.f16|p.f17*p.f18*p.f19^p.f20|p.f21*p.f22-p.f23-p.f24|p.f25|p.f26&p.f27*p.f28^p.f30|p.f31;
            p.f30=p.f0-p.f1^p.f2-p.f3&p.f4*p.f5^p.f6+p.f7&p.f8&p.f9&p.f10-p.f11|p.f12+p.f13+p.f14+p.f15&p.f16^p.f17|p.f18|p.f19*p.f20-p.f21&p.f22|p.f23^p.f24&p.f25&p.f26-p.f27|p.f28^p.f29&p.f31;
            p.f31=p.f0^p.f1*p.f2+p.f3&p.f4-p.f5*p.f6-p.f7*p.f8-p.f9-p.f10^p.f11&p.f12+p.f13*p.f14-p.f15^p.f16*p.f17|p.f18^p.f19-p.f20&p.f21+p.f22^p.f23|p.f24+p.f25&p.f26|p.f27*p.f28-p.f29+p.f30;

            return p.f0+p.f1+p.f2+p.f3+p.f4+p.f5+p.f6+p.f7+p.f8+p.f9+p.f10+p.f11+p.f12+p.f13+p.f14+p.f15+p.f16+p.f17+p.f18+p.f19+p.f20+p.f21+p.f22+p.f23+p.f24+p.f25+p.f26+p.f27+p.f28+p.f29+p.f30+p.f31;

        }
    }
}
