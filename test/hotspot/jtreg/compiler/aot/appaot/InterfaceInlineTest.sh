#!/bin/sh
#
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
# @summary test inline super constructor
# @run shell InlineTest.sh

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
TEST_CLASS=TmpTestAppAOT
TEST_SOURCE=${TEST_CLASS}.java
SUB_DIR=sub
TEST_SUB_CLASS=Test
TEST_SUB_SOURCE=${TEST_SUB_CLASS}.java
AOT_LIB=test.so

###################################################################################
# test main
cat > ${TESTCLASSES}${FS}${TEST_SOURCE} << EOF
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import com.alibaba.aot.AppAOTController;

public class TmpTestAppAOT{
    public static void main(String[] args)throws Exception{
        File file1=new File("./sub");
        URL url1 = file1.toURI().toURL();
        run(url1);            // run with loader1
    }

    public static void run(URL url) throws Exception{
        URL [] urls={url};
        System.out.println("work on "+url);
        AotLoader ucl = new AotLoader(urls);
        int loaded = AppAOTController.loadAOTLibraryForLoader(ucl, "./test.so");
        if (loaded != 0) throw new Exception("failed to load aot library");
        Class<?> c1 = ucl.loadClass("Test");
        Constructor<?> conc = c1.getConstructor();
        Object obj = conc.newInstance();
    }
}

class AotLoader extends URLClassLoader {
    public AotLoader(URL [] urls) {
        super(urls);
    }
}
EOF

# make sub classes which are aot compiled
rm -rf ${TESTCLASSES}${FS}${SUB_DIR}
mkdir ${TESTCLASSES}${FS}${SUB_DIR}
rm -rf ${SUB_DIR}
mkdir ${SUB_DIR}

cat > ${TESTCLASSES}${FS}${SUB_DIR}${FS}$TEST_SUB_SOURCE << EOF
public class Test implements Itfc {
    public Test() {
        doSth();
    }
}
interface Itfc {
    static Dummy field = new Dummy();

    default public void doSth() {
        Dummy v = field;
    }
}
class Dummy {}
EOF

# Do compilation
${JAVAC} -cp ${TESTCLASSES} -d ${TESTCLASSES} ${TESTCLASSES}${FS}$TEST_SOURCE >> /dev/null 2>&1
if [ $? != '0' ]
then
	printf "Failed to compile ${TESTCLASSES}${FS}${TEST_SOURCE}"
	exit 1
fi
echo "compile sub classes"
${JAVAC} -cp ${TESTCLASSES} -d ${SUB_DIR} ${TESTCLASSES}${FS}${SUB_DIR}${FS}$TEST_SUB_SOURCE >> /dev/null 2>&1
if [ $? != '0' ]
then
	printf "Failed to compile ${TESTCLASSES}${FS}${SUB_DIR}${FS}${TEST_SUB_SOURCE}"
	exit 1
fi

# Do jaotc compilation
echo "aot compilation"
${JAOTC} --output ${AOT_LIB} -J-Dgraal.PrintCompilation=true --verbose -J-cp -J${SUB_DIR} -J-Dgraal.AOTInliningClassInitialization=true ${TEST_SUB_CLASS}.class > aotc.log
if [ $? != '0' ]
then
	printf "Failed to aot compile ${SUB_DIR}${FS}${TEST_SUB_SOURCE}.class"
	exit 1
fi

#run test
${JAVA} -XX:+UseAppAOT -XX:+PrintAOT -XX:+PrintCompilation -Xlog:aot*=debug ${TEST_CLASS} >> output.txt

function check_aotc_log() {
  # check xadd method is compiled
  xadd_mesg=`grep "added <init>()V" aotc.log|wc -l`
  if [[ $xadd_mesg -ne 1 ]]; then
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
  lib_load_mesg=`grep "test.so" output.txt|grep "loaded"|wc -l`

  if [[ $lib_load_mesg -ne 1 ]]; then
    echo "aot lib is not loaded"
    exit -1
  fi

  # check aot method is executed
  echo "check aot method"
  aot_method_messages=`grep "Test.<init>" output.txt|grep aot|wc -l`
  if [[ $aot_method_messages -ne 1 ]]; then
    echo "aot method Test.<init> is not executed"
    exit -1
  fi

  exit 0
}

echo "check jvm output"
check_output

