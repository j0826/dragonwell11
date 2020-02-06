#!/usr/bin/env bash

#
# @test TestJVMOptionDeps
# @summary Test the dependencies of JVM options
# @run shell TestJVMOptionDeps.sh
#

if [ "${TESTSRC}" = "" ]
then
    TESTSRC=${PWD}
    echo "TESTSRC not set.  Using "${TESTSRC}" as default"
fi
echo "TESTSRC=${TESTSRC}"
## Adding common setup Variables for running shell tests.
. ${TESTSRC}/../test_env.sh

JAVA=${TESTJAVA}${FS}bin${FS}java

set -x

# if $FROM is enabled, $TO should be enabled automatically
function check_dependency_bool_bool() {
  FROM=$1
  TO="$(echo $2 | sed 's/-XX:+//g')"
  if [ -z "$(${JAVA} ${FROM} -XX:+PrintFlagsFinal -version 2>&1 | grep ${TO} | grep '= true')" ]; then
    echo "check_dependency_bool_bool failed: $1 --> $2"
    exit 1
  fi
}

function check_dependency_bool_bool_false() {
  FROM=$1
  TO="$(echo $2 | sed 's/-XX:+//g')"
  if [ -z "$(${JAVA} ${FROM} -XX:+PrintFlagsFinal -version 2>&1 | grep ${TO} | grep '= false')" ]; then
    echo "check_dependency_bool_bool failed: $1 --> $2"
    exit 1
  fi
}

check_dependency_bool_bool '-XX:+UseG1GC -XX:+TenantDataIsolation' '-XX:+MultiTenant'
check_dependency_bool_bool '-XX:+UseG1GC -XX:+TenantCpuThrottling' '-XX:+MultiTenant'
check_dependency_bool_bool '-XX:+UseG1GC -XX:+TenantCpuAccounting' '-XX:+MultiTenant'
check_dependency_bool_bool '-XX:+UseG1GC -XX:+TenantThreadStop' '-XX:+MultiTenant'

# check if provided jvm arguments is invalid
function assert_invalid_jvm_options() {
  JVM_ARGS=$1
  CMD="${JAVA} ${JVM_ARGS} -version"
  OUT=$(${CMD} 2>&1)
  if [ 0 -eq $? ]; then
    echo "Expected invalid JVM arguments: ${JVM_ARGS}"
    exit 1
  fi
}

assert_invalid_jvm_options '-XX:+TenantCpuThrottling -XX:-MultiTenant'
assert_invalid_jvm_options '-XX:+TenantCpuAccounting -XX:-MultiTenant'
assert_invalid_jvm_options '-XX:+TenantDataIsolation -XX:-MultiTenant'
assert_invalid_jvm_options '-XX:+TenantThreadStop -XX:-MultiTenant'