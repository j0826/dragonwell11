#!/usr/bin/env bash

#
# @test TestJGroupDebugMode.sh
# @summary test debugging mode of JGroup native implementation
# @run shell/timeout=300 TestJGroupDebugMode.sh
#

set -x

if [ "${TESTSRC}" = "" ]
then
    TESTSRC=${PWD}
    echo "TESTSRC not set.  Using "${TESTSRC}" as default"
fi
echo "TESTSRC=${TESTSRC}"
FS=/
JAVA=${TESTJAVA}${FS}bin${FS}java
JAVAC=${TESTJAVA}${FS}bin${FS}javac
TEST_CLASS="Test"
TEST_OPTS="-XX:+MultiTenant -XX:+TenantCpuThrottling"

# generate and compile Java snippet for testing
cat >> ${TEST_CLASS}.java << EOF
import com.alibaba.tenant.*;
public class ${TEST_CLASS} {
    public static void main(String[] args) throws TenantException {
        TenantConfiguration config = new TenantConfiguration()
            //.limitHeap(64 * 1024 * 1024)
            .limitCpuShares(1024);
        TenantContainer tenant = TenantContainer.create(config);
        tenant.run(()-> {
            // empty!
        });
    }
}
EOF

${JAVAC} -cp . ${TEST_CLASS}.java
if [ $? != 0 ]; then
    echo "Failed to compile ${TEST_CLASS}.java"
    exit 1
fi

# Test envrionment variable debugging options

unset JGROUP_DEBUG

# disable debugging
export JGROUP_DEBUG=""
if [ ! -z "$(${JAVA} ${TEST_OPTS} -cp ${PWD} ${TEST_CLASS} | grep 'cgroup initialized successfully')" ]; then
    echo "Failed in non-debug mode"
    exit 1
fi

# enable debugging
export JGROUP_DEBUG="TRue"
if [ -z "$(${JAVA} ${TEST_OPTS} -cp ${PWD} ${TEST_CLASS} | grep 'cgroup initialized successfully')" ]; then
    echo "Failed in debug mode"
    exit 1
fi

export JGROUP_DEBUG="trUe"
if [ -z "$(${JAVA} ${TEST_OPTS} -cp ${PWD} ${TEST_CLASS} | grep 'cgroup initialized successfully')" ]; then
    echo "Failed in debug mode"
    exit 1
fi

# Test Java property debugging options
if [ ! -z "$(${JAVA} ${TEST_OPTS} -cp ${PWD} ${TEST_CLASS} | grep 'Created group with standard controllers:')" ]; then
    echo "Failed in non-debug mode"
    exit 1
fi

if [ ! -z "$(${JAVA} ${TEST_OPTS} -Dcom.alibaba.tenant.debugJGroup=false -cp ${PWD} ${TEST_CLASS} | grep 'Created group with standard controllers')" ]; then
    echo "Failed in debug mode"
    exit 1
fi

# enable debugging
if [ -z "$(${JAVA} ${TEST_OPTS} -Dcom.alibaba.tenant.debugJGroup=tRue -cp ${PWD} ${TEST_CLASS} | grep 'Created group with standard controllers')" ]; then
    echo "Failed in debug mode"
    exit 1
fi

if [ -z "$(${JAVA} ${TEST_OPTS} -Dcom.alibaba.tenant.debugJGroup=trUE -cp ${PWD} ${TEST_CLASS} | grep 'Created group with standard controllers')" ]; then
    echo "Failed in debug mode"
    exit 1
fi
