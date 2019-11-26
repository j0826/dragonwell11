#!/bin/sh
# Copyright (c) 2019 Alibaba Group Holding Limited. All Rights Reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation. Alibaba designates this
# particular file as subject to the "Classpath" exception as provided
# by Oracle in the LICENSE file that accompanied this code.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#

# @test
# @summary A recursively inlined callee (inline level >= 2 if root is level 0) is loaded by its direct caller which may have a different defining class loader as the root method.
# @run shell TestIncorrectCallerForInlinedCallee.sh

if [ "${TESTSRC}" = "" ]
then
    TESTSRC=${PWD}
    echo "TESTSRC not set.  Using "${TESTSRC}" as default"
fi
echo "TESTSRC=${TESTSRC}"
## Adding common setup Variables for running shell tests.
. ${TESTSRC}/../../../test_env.sh

JAVA=${TESTJAVA}${FS}bin${FS}java
JAVAC=${TESTJAVA}${FS}bin${FS}javac
JAOTC=${TESTJAVA}${FS}bin${FS}jaotc


# Layout:
# We can test AppAOT by using TmpTestAppAOT as the main class or
# test the normal AOT by using Main as the main class.
# TESTCLASSES
#      │   TmpTestAppAOT
#      │   TmpTestAppAOT.class
#      ├── main
#          ├── Main.class
#          ├── Main.java
#          ├── childFirst
#          │   ├── A.class
#          │   ├── A.java
#          │   ├── C.class
#          │   └── C.java
#          ├── ChildFirstLoader.class
#          ├── parentFirst
#          │   ├── B.class
#          │   ├── B.java
#          │   ├── C.class
#          │   └── C.java
#          ├── ParentFirstLoader.class


TEST_MAIN_CLASS=Main
TEST_MAIN_SOURCE=${TEST_MAIN_CLASS}.java

MAIN_DIR=${TESTCLASSES}
CHILD_FIRST_DIR=${MAIN_DIR}${FS}childFirst
PARENT_FIRST_DIR=${MAIN_DIR}${FS}parentFirst

rm -rf $CHILD_FIRST_DIR && mkdir -p $CHILD_FIRST_DIR
rm -rf $PARENT_FIRST_DIR && mkdir -p $PARENT_FIRST_DIR

MAIN_AOT_LIB=${TESTCLASSES}${FS}libMain.so
AOTC_LOG_PATH=${TESTCLASSES}${FS}aotc.log
TEST_LOG_PATH=${TESTCLASSES}${FS}output.txt


###################################################################################

cat > ${MAIN_DIR}${FS}${TEST_MAIN_SOURCE} << EOF
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import com.alibaba.aot.AppAOTController;

public class Main {
    public static void main(String[] args)throws Exception {
        ParentFirstLoader pfcl = new ParentFirstLoader(new URL[] { Main.class.getResource("parentFirst/") });
        ChildFirstLoader cfcl = new ChildFirstLoader(new URL[] { Main.class.getResource("childFirst/") }, pfcl);
        {
            int loaded = AppAOTController.loadAOTLibraryForLoader(cfcl, "${MAIN_AOT_LIB}");
            if (loaded != 0) throw new Exception("failed to load aot library");
        }
        Class<?> A = cfcl.loadClass("A");
        Method m = A.getDeclaredMethod("testA");
        System.out.println(m.invoke(null));
    }
}

class ParentFirstLoader extends URLClassLoader {
    public ParentFirstLoader(URL [] urls) {
        super(urls);
    }
}

// Widely used by tomcat and other containers
class ChildFirstLoader extends URLClassLoader {

    ClassLoader parent;

    public ChildFirstLoader(URL [] urls, ClassLoader parent) {
        super(urls, parent);
        this.parent = parent;
    }


    protected Class<?> loadClass(String name, boolean resolve)
        throws ClassNotFoundException
    {
        synchronized (getClassLoadingLock(name)) {
            // First, check if the class has already been loaded
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                try {
                    c = findClass(name);
                } catch (ClassNotFoundException e) {
                    // ClassNotFoundException thrown if class not found
                    // from the child class loader
                }

                if (c == null) {
                    if (parent != null) {
                        c = Class.forName(name, false, parent);
                    }
                }
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }
}
EOF


cat > ${CHILD_FIRST_DIR}${FS}A.java << EOF
public final class A {
    public static String testA() {
        return "A:" + B.testB();
    }
}
EOF

cat > ${CHILD_FIRST_DIR}${FS}C.java << EOF
public final class C {
    static { System.out.println("Should not load me!"); }
    public static String testC() {
        return "Invalid C";
    }
}
EOF

cat > ${PARENT_FIRST_DIR}${FS}B.java << EOF
public final class B {
    public static final String testB() {
        return C.testC();
    }
}
EOF

cat > ${PARENT_FIRST_DIR}${FS}C.java << EOF
public final class C {
    public static final String testC() {
        return "Valid C";
    }
}
EOF

# Do compilation
echo "compile main"
${JAVAC} -d ${MAIN_DIR} ${MAIN_DIR}${FS}${TEST_MAIN_SOURCE} >> /dev/null 2>&1
if [ $? != '0' ]
then
    echo "Failed to compile ${MAIN_DIR}${FS}${TEST_MAIN_SOURCE}"
    exit 1
fi

echo "compile parentFirst"
${JAVAC} -d ${PARENT_FIRST_DIR} ${PARENT_FIRST_DIR}${FS}B.java ${PARENT_FIRST_DIR}${FS}C.java >> /dev/null 2>&1
if [ $? != '0' ]
then
    echo "Failed to compile ${PARENT_FIRST_DIR}${FS}B.java"
    exit 1
fi

echo "compile childFirst"
${JAVAC} -d ${CHILD_FIRST_DIR} -cp ${PARENT_FIRST_DIR}${PS}${CHILD_FIRST_DIR} ${CHILD_FIRST_DIR}${FS}A.java ${CHILD_FIRST_DIR}${FS}C.java >> /dev/null 2>&1
if [ $? != '0' ]
then
    echo "Failed to compile ${CHILD_FIRST_DIR}"
    exit 1
fi

echo "Building libMain.so (put parentFirst in front of childFirst)"
$JAOTC --output ${MAIN_AOT_LIB} -J-cp -J${PARENT_FIRST_DIR}${PS}${CHILD_FIRST_DIR} -J-Dgraal.PrintCompilation=true --verbose A B C > ${AOTC_LOG_PATH}
if [ $? != '0' ]
then
    echo "Failed to aot compile ${TEST_MAIN_CLASS}.class"
    exit 1
fi


#run test
VM_OPTS="-XX:+UseAppAOT -XX:+PrintAOT -XX:+PrintCompilation -Xlog:aot*=trace"
VM_OPTS="$VM_OPTS -cp ${MAIN_DIR}"
${JAVA} ${VM_OPTS} ${TEST_MAIN_CLASS} > ${TEST_LOG_PATH}

function check_aotc_log() {
  # check xadd method is compiled
  comp_mesg=`grep "added testA()Ljava/lang/String;" ${AOTC_LOG_PATH}|wc -l`
  if [[ $comp_mesg -ne 1 ]]; then
    echo "expected java method is not compiled by jaotc"
    exit -1
  fi
}

echo "check aotc output"
check_aotc_log

function check_output()
{
  # check aot lib is loaded
  echo "check aot lib"
  lib_load_mesg=`grep -E "loaded.*libMain.so.*aot library" ${TEST_LOG_PATH} |  wc -l`

  if [[ $lib_load_mesg -ne 1 ]]; then
    echo "aot lib libMain.so is not loaded"
    exit -1
  fi

  # check aot method is executed
  echo "check aot method"
  aot_method_messages=`grep -E "aot\\[.*A.testA\\(\\)Ljava/lang/String;" ${TEST_LOG_PATH} | grep aot | wc -l`
  if [[ $aot_method_messages == 0 ]]; then
    echo "aot method  A.testA  is **not executed**"
    exit -1
  fi

  # check invocation result
  result_message=`grep "Should not load me!" ${TEST_LOG_PATH} | wc -l`
  if [[ $result_message -ne 0 ]]; then
    echo "not expected result" $result_message
    exit -1
  fi

  # check invocation result
  result_message=`grep "Invalid C" ${TEST_LOG_PATH} | wc -l`
  if [[ $result_message -ne 0 ]]; then
    echo "not expected result" $result_message
    exit -1
  fi

  # check invocation result
  result_message=`grep "Valid C" ${TEST_LOG_PATH} | wc -l`
  if [[ $result_message -ne 1 ]]; then
    echo "not expected result" $result_message
    exit -1
  fi

  exit 0
}

echo "check jvm output"
check_output

