/*
 * Copyright (c) 2020 Alibaba Group Holding Limited. All Rights Reserved.
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
 *
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.JDKToolLauncher;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.lang.reflect.Field;
import com.sun.tools.attach.VirtualMachine;
import java.lang.management.ManagementFactory;
import static jdk.test.lib.Asserts.assertTrue;

/*
 * @test TestClassRedefinitionPrintStackTrace
 * @library /test/lib
 * @build TestClassRedefinitionPrintStackTrace
 * @run main/othervm TestClassRedefinitionPrintStackTrace
 * @summary Print stack trace when code cache flushing occurred due to class re-definition
 */
public class TestClassRedefinitionPrintStackTrace {
    public static void main(String[] args) throws Exception {
        TargetClassGeneration.gen();
        AgentOperation.prepare();
        Running.run();
    }

    static class Main {
        public static void main(String[] args) throws Exception {
            System.out.println("In Main.main().");
            Proxy.init();
            Proxy.test();
            Thread.sleep(5000);
            Proxy.test();
            Proxy.clean();
        }
    }

    static class Running {
        public static void run() throws Exception {
            ProcessBuilder pb = ProcessBuilderFactory.create(JVMArg.Running, "");
            Process p = pb.start();
            TargetClassGeneration.genRedefined();
            AgentOperation.attach(Long.toString(p.pid()));
            OutputAnalyzer output = new OutputAnalyzer(p);
            output.shouldMatch("\\s+at Agent.agentmain\\(Agent.java:\\d+\\)");
            p.waitFor();
        }
    }

    // A proxy class, in which TargetClass is loaded by a custom class loader.
    static class Proxy {
        private static Object holder;
        private static CustomClassLoader classLoader;
        private static Method fooMethod;

        public static void init() throws Exception {
            classLoader = new CustomClassLoader();
            Class clz = loadClass("TargetClass");
            System.out.println("Class loader of TargetClass: " + clz.getClassLoader().getClass().getName());
            Constructor ctor = clz.getConstructor();
            ctor.setAccessible(true);
            holder = ctor.newInstance();
            fooMethod = clz.getMethod("test");
            fooMethod.setAccessible(true);
        }

        public static void test() throws Exception {
            fooMethod.invoke(holder);
        }

        public static void clean() {
            holder = null;
            fooMethod = null;
            classLoader = null;
        }

        public static Class loadClass(String className) throws Exception {
            return classLoader.findClass(className);
        }
    }

    // Generate TargetClass, which will be redefined by an agent.
    static class TargetClassGeneration {
        public static final String WRAPPER_CLASS_NAME = "Wrapper";
        public static final String TARGET_CLASS_FILE = "TargetClass.class";
        public static final String WRAPPER_CLASS_BODY =
                "import java.lang.reflect.Method;\n" +
                        "public class Wrapper {}\n" +
                        "\n" +
                        "class TargetClass {\n" +
                        "    public TargetClass() {}\n" +
                        "\n" +
                        "    public void test() throws Exception {\n" +
                        "        System.out.println(\"In %s TargetClass.test().\");\n" +
                        "        Thread.sleep(5000);\n" +
                        "        for (int i = 0; i < 50000; i++) {\n" +
                        "            foo();\n" +
                        "        }\n" +
                        "    }\n" +
                        "    public void foo() throws Exception {}\n" +
                        "}";

        public static final String REDEFINED_WRAPPER_CLASS_BODY =
                "import java.lang.reflect.Method;\n" +
                        "public class Wrapper {}\n" +
                        "\n" +
                        "class TargetClass {\n" +
                        "    public TargetClass() {}\n" +
                        "\n" +
                        "    public void test() throws Exception {\n" +
                        "        System.out.println(\"In %s TargetClass.test().\");\n" +
                        "        for (int i = 0; i < 50000; i++) {\n" +
                        "            foo();\n" +
                        "        }\n" +
                        "    }\n" +
                        "    public void foo() throws Exception { \n" +
                        "        bar();\n" +
                        "    }\n" +
                        "\n" +
                        "    private final void bar() throws Exception { \n" +
                        "    }\n" +
                        "}";

        public static void gen() throws Exception {
            JavaCompiler.compile(WRAPPER_CLASS_NAME, String.format(WRAPPER_CLASS_BODY, "original"));
            assertTrue(new File(TARGET_CLASS_FILE).exists());
        }

        public static void genRedefined() throws Exception {
            JavaCompiler.compile(WRAPPER_CLASS_NAME, String.format(REDEFINED_WRAPPER_CLASS_BODY, "redefined"));
            assertTrue(new File(TARGET_CLASS_FILE).exists());
        }
    }

    static class CustomClassLoader extends ClassLoader {
        @Override
        public Class findClass(String name) throws ClassNotFoundException {
            byte[] b = loadClassFromFile(name);
            return defineClass(name, b, 0, b.length);
        }

        private byte[] loadClassFromFile(String fileName) {
            try {
                InputStream inputStream = new FileInputStream(fileName.replace('.', File.separatorChar) + ".class");
                int size = inputStream.available();
                byte[] buffer = new byte[size];
                DataInputStream in = new DataInputStream(inputStream);
                in.readFully(buffer);
                in.close();
                return buffer;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    static class AgentOperation {
        private static final String MANIFEST_FILE_NAME = "MANIFEST.MF";
        private static final String AGENT_JAR_FILE_NAME = "agent.jar";
        private static final String AGENT_CLASS_NAME = "Agent";

        private static final String AGENT_CLASS_BODY =
                "import java.io.File;\n" +
                        "import java.lang.instrument.ClassDefinition;\n" +
                        "import java.lang.instrument.Instrumentation;\n" +
                        "import java.nio.file.Files;\n" +
                        "\n" +
                        "public class Agent {\n" +
                        "    public static void agentmain(String args, Instrumentation instrumentation) {\n" +
                        "        try {\n" +
                        "            System.out.println(\"Agent is loaded. \");\n" +
                        "            Class tgtClass = null;\n" +
                        "            for (Class clz : instrumentation.getAllLoadedClasses()) {\n" +
                        "                if (clz.getName().equals(\"TargetClass\")) {\n" +
                        "                    tgtClass = clz;\n" +
                        "                    break;\n" +
                        "                }\n" +
                        "            }\n" +
                        "            if (tgtClass == null) {\n" +
                        "                System.out.println(\"Unable to find TargetClass.\");\n" +
                        "            }\n" +
                        "            System.out.println(\"Old class: \" + tgtClass);\n" +
                        "            System.out.println(\"New TargetClass location: \" + new File(\"TargetClass.class\").getAbsolutePath());\n" +
                        "            instrumentation.redefineClasses(new ClassDefinition(tgtClass, Files.readAllBytes(new File(new File(\"TargetClass.class\").getAbsolutePath()).toPath())));\n" +
                        "            System.out.println(\"Finishing redefining the class TargetClass.\");\n" +
                        "        } catch (Exception e) {\n" +
                        "            e.printStackTrace();\n" +
                        "        }\n" +
                        "    }\n" +
                        "}";

        private static final String MANIFEST_FILE =
                "Manifest-Version: 1.0\n" +
                        "Agent-Class: Agent\n" +
                        "Can-Redefine-Classes: true\n" +
                        "Can-Retransform-Classes: true";

        public static void prepare() throws Exception {
            JavaCompiler.compile(AGENT_CLASS_NAME, AGENT_CLASS_BODY);
            genManifest();
            assertTrue(new File(MANIFEST_FILE_NAME).exists());
            genAgentJar();
            assertTrue(new File(AGENT_JAR_FILE_NAME).exists());
            System.out.println("Successfully generating the agent.");
        }

        public static void attach(String pid) {
            System.out.println(String.format("## Trying to attach the agent to the process %s ...", pid));
            try {
                VirtualMachine vm = VirtualMachine.attach(pid);
                vm.loadAgent("agent.jar", "");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private static void genManifest() {
            try (PrintWriter writer = new PrintWriter(new FileWriter(MANIFEST_FILE_NAME))) {
                writer.println(MANIFEST_FILE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private static void genAgentJar() throws Exception {
            JDKToolLauncher jar = JDKToolLauncher
                    .create("jar")
                    .addToolArg("cfm")
                    .addToolArg("agent.jar")
                    .addToolArg(MANIFEST_FILE_NAME)
                    .addToolArg("Agent.class");
            new ProcessBuilder(jar.getCommand()).start().waitFor();
        }
    }

    /**
     * JVM options
     */
    static class JVMArg {
        public static final List<String> Running = new ArrayList(Arrays.asList(
                "-XX:+PrintCodeCacheFlushStackTrace"
        ));
    }

    static class JavaCompiler {
        public static void compile(String className, String classBody) throws Exception {
            final String javaFileName = className + ".java";
            final String classFileName = className + ".class";
            // 1. Dump code to a Java file
            try (PrintWriter writer = new PrintWriter(new FileWriter(javaFileName))) {
                writer.println(classBody);
            } catch (Exception e) {
                e.printStackTrace();
            }
            assertTrue(new File(javaFileName).exists());
            // 2. Compile it with javac
            JDKToolLauncher javac = JDKToolLauncher.create("javac").addToolArg(javaFileName);
            new ProcessBuilder(javac.getCommand()).start().waitFor();
            assertTrue(new File(classFileName).exists());
        }
    }

    static class ProcessBuilderFactory {
        public static ProcessBuilder create(final List<String> jvmArgs, String programArg) throws Exception {
            List<String> _jvmArgs = new ArrayList<>(jvmArgs);
            _jvmArgs.addAll(Arrays.asList(
                    Main.class.getName(),
                    programArg
            ));
            return ProcessTools.createJavaProcessBuilder(_jvmArgs.stream().toArray(String[]::new));
        }
    }
}