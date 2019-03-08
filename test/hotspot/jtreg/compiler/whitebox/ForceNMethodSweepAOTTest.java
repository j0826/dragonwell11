/*
 * Copyright (c) 2019 Alibaba Group Holding Limited. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Alibaba designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

/*
 * @test
 * @summary testing of unloading app aot library
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build sun.hotspot.WhiteBox
 * @run driver ClassFileInstaller sun.hotspot.WhiteBox
 *                                sun.hotspot.WhiteBox$WhiteBoxPermission
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI
 *                   -XX:+UseAppAOT -XX:+PrintAOT -Xlog:aot*=debug
 *                   -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockExperimentalVMOptions
 *                   compiler.whitebox.ForceNMethodSweepAOTTest METHOD_TEST
 */

package compiler.whitebox;

import jdk.test.lib.Asserts;
import sun.hotspot.code.BlobType;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import com.alibaba.aot.AppAOTController;
import jdk.test.lib.process.*;
import java.util.*;

import java.util.EnumSet;

public class ForceNMethodSweepAOTTest extends CompilerWhiteBoxTest {
    public static void main(String[] args) throws Exception {
        CompilerWhiteBoxTest.main(ForceNMethodSweepAOTTest::new, args);
    }
    private final EnumSet<BlobType> blobTypes;
    private ForceNMethodSweepAOTTest(TestCase testCase) {
        super(testCase);
        // to prevent inlining of #method
        WHITE_BOX.testSetDontInlineMethod(method, true);
        blobTypes = BlobType.getAvailable();
    }

    private void generateSharedLibrary(String workdir, String classPath) throws Exception{
        OutputAnalyzer output = null;
        String javaHome = System.getProperty("java.home");

        try {
            output = ProcessTools.executeProcess(javaHome + "/bin/jaotc",
                        "--output", workdir + "/test.so",
                        "-J-cp", "-J"+classPath, "compiler.whitebox.T1");
            System.out.println(output.getOutput());
        } catch (Throwable e) {
            e.printStackTrace();
            throw new Exception("jaotc fail");
        }
    }

    @Override
    protected void test() throws Exception {
        String classPath = System.getProperty("test.class.path");
        System.out.println("classPath = " + classPath);
        OutputAnalyzer output = null;
        String workdir = "";
        try {
            output = ProcessTools.executeProcess("pwd");
            workdir = output.getOutput().trim();
            System.out.println("workdir = " + workdir);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        generateSharedLibrary(workdir, classPath);

        File file=new File(workdir);
        URL url = file.toURI().toURL();
        URL [] urls={url};
        AotLoader ucl = new AotLoader(urls);
        int loaded = AppAOTController.loadAOTLibraryForLoader(ucl, workdir+"/test.so");
        if (loaded != 0){
            throw new Exception("failed to load aot library");
        }
        Class<?> c1 = ucl.loadClass("compiler.whitebox.T1");
        Constructor<?> conc = c1.getConstructor();
        Object obj = conc.newInstance();
        Method m = c1.getMethod("xadd", int.class, int.class);
        m.invoke(obj, 0, 0);
        AppAOTController.unloadAOTLibraryForLoader(ucl);
        guaranteedSweep();
        int usage = getTotalUsage();
        System.out.println(usage);
    }

    private int getTotalUsage() {
        int usage = 0;
        for (BlobType type : blobTypes) {
           usage += type.getMemoryPool().getUsage().getUsed();
        }
        return usage;
    }
    private void guaranteedSweep() {
        // not entrant -> ++stack_traversal_mark -> zombie -> flushed
        for (int i = 0; i < 5; ++i) {
            WHITE_BOX.fullGC();
            WHITE_BOX.forceNMethodSweep();
        }
    }
}

class AotLoader extends URLClassLoader {
    public AotLoader(URL [] urls) {
        super(urls);
    }
}

class T1 {
    public T1(){};
    public int xadd(int a,int b){
      int r=a+b;
      return r;
    }
}
