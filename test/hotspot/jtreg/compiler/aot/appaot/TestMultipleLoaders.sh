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
# @summary test aot method can not access class from other loader
# @run shell TestMultipleLoaders.sh

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
SUB1_DIR=sub1
SUB2_DIR=sub2
TEST_SUB1_CLASS=T1
TEST_SUB2_CLASS=T2
TEST_SUB1_SOURCE=${TEST_SUB1_CLASS}.java
TEST_SUB2_SOURCE=${TEST_SUB2_CLASS}.java
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
        File file1=new File("./sub1");
        File file2=new File("./sub2");
        URL url1 = file1.toURI().toURL();
        URL url2 = file2.toURI().toURL();
        changeT2(url1);       // run with loader1
        run(url2);            // run with loader2
    }

    public static void changeT2(URL url) throws Exception{
        URL [] urls={url};
        System.out.println("work on "+url);
        AotLoader ucl = new AotLoader(urls);
        Class<?> c2 = ucl.loadClass("T2");
        Constructor<?> conc = c2.getConstructor();
        Object obj = conc.newInstance();
        Method m = c2.getMethod("change", int.class);
        m.invoke(obj, 0); // change T2.foo=0
    }

    public static void run(URL url) throws Exception{
        URL [] urls={url};
        System.out.println("work on "+url);
        AotLoader ucl = new AotLoader(urls);
        int loaded = AppAOTController.loadAOTLibraryForLoader(ucl, "./test.so");
        if (loaded != 0) throw new Exception("failed to load aot library");
        Class<?> c1 = ucl.loadClass("T1");
        Constructor<?> conc = c1.getConstructor();
        Object obj = conc.newInstance();
        Method m = c1.getMethod("xadd", int.class, int.class);
        Object ret = m.invoke(obj, 0, 0);
        System.out.println("invoke result is:"+ret); // ret should be 42
    }
}

class AotLoader extends URLClassLoader {
    public AotLoader(URL [] urls) {
        super(urls);
    }
}
EOF

# make sub classes which are aot compiled
rm -rf ${TESTCLASSES}${FS}${SUB1_DIR}
mkdir ${TESTCLASSES}${FS}${SUB1_DIR}
rm -rf ${SUB1_DIR}
mkdir ${SUB1_DIR}
rm -rf ${TESTCLASSES}${FS}${SUB2_DIR}
rm -rf ${SUB2_DIR}

cat > ${TESTCLASSES}${FS}${SUB1_DIR}${FS}$TEST_SUB1_SOURCE << EOF
public class T1{
  public int xadd(int a,int b){
    int r=a+b+T2.foo;
    return r;
  }
}
EOF

cat > ${TESTCLASSES}${FS}${SUB1_DIR}${FS}$TEST_SUB2_SOURCE << EOF
public class T2{
  public static int foo=42;
  public static void change(int newVal) {foo=newVal;}
}
EOF

cp -r ${TESTCLASSES}${FS}${SUB1_DIR} ${TESTCLASSES}${FS}${SUB2_DIR}

# Do compilation
${JAVAC} -cp ${TESTCLASSES} -d ${TESTCLASSES} ${TESTCLASSES}${FS}$TEST_SOURCE >> /dev/null 2>&1
if [ $? != '0' ]
then
	printf "Failed to compile ${TESTCLASSES}${FS}${TEST_SOURCE}"
	exit 1
fi
echo "compile sub classes"
#javac sub1/*.java
${JAVAC} -cp ${TESTCLASSES} -d ${SUB1_DIR} ${TESTCLASSES}${FS}${SUB1_DIR}${FS}*.java >> /dev/null 2>&1
if [ $? != '0' ]
then
	printf "Failed to compile ${TESTCLASSES}${FS}${SUB1_DIR}"
	exit 1
fi
cp -r ${SUB1_DIR} ${SUB2_DIR}

# Do jaotc compilation
echo "aot compilation (jaotc sub1/T1.class)"
${JAOTC} --output ${AOT_LIB} -J-Dgraal.PrintCompilation=true --verbose -J-cp -J${SUB1_DIR} ${TEST_SUB1_CLASS}.class > aotc.log
if [ $? != '0' ]
then
	printf "Failed to aot compile ${SUB1_DIR}${FS}${TEST_SUB1_SOURCE}.class"
	exit 1
fi

#run test
${JAVA} -XX:+UseAppAOT -XX:+PrintAOT -XX:+PrintCompilation -Xlog:aot*=debug ${TEST_CLASS} >> output.txt

function check_aotc_log() {
  # check xadd method is compiled
  xadd_mesg=`grep "added xadd(II)I" aotc.log|wc -l`
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
  aot_method_messages=`grep "T1.xadd(II)I" output.txt|grep aot|wc -l`
  if [[ $aot_method_messages -ne 1 ]]; then
    echo "aot method T1.xadd is not executed"
    exit -1
  fi

  # check invocation result
  result_message=`grep "invoke result is:42" output.txt|wc -l`
  if [[ $result_message -ne 1 ]]; then
    echo "not expected result" $result_message
    exit -1
  fi

  exit 0
}

echo "check jvm output"
check_output

