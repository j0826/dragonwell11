#!/usr/bin/env bash

#
# @test TestJGroupInitializer.sh
# @summary test tenant initializer scripts, encapsulated inside JDK/bin/jgroup
# @run shell/timeout=300 TestJGroupInitializer.sh
#

# Please NOTE: AJDK-11 will only support AliOS version >= 7

#
# this testcase only works with specific docker images
# so for continuous-integration system, it should be scheduled to
# a docker-compatible machine
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
JGROUP=${TESTJAVA}${FS}bin${FS}jgroup

# check if docker command and needed docker images exist
which docker > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "Cannot find command 'docker'"
    exit 1
fi

# please add more to-be-checked images here
DOCKER_IMAGES=("reg.docker.alibaba-inc.com/alibase/alios7u2")
for IMG in ${DOCKER_IMAGES[*]}; do
    if [ -z "$(docker images $IMG | grep -v REPOSITORY)" ]; then
        echo "Cannot find docker image: $IMG"
        exit 1
    fi
done

#
################ Test the normal path ###################
#
echo "Testing normal path..."

######### Test with AliOS7u image
TEST_SH=alios7u.sh
IMAGE='reg.docker.alibaba-inc.com/alibase/alios7u2'
cat > ${TEST_SH} << EOF
#!/bin/bash
echo "Inside docker..."
yum install libcgroup-tools libcgroup -y
${JGROUP} -u root -g root
[ \$? -eq 0 ] || exit 1
${JAVA} -XX:+MultiTenant -XX:+TenantCpuThrottling -XX:+TenantCpuAccounting -XX:+UseG1GC -version
[ \$? -eq 0 ] || exit 1
# embedded groups in another group
${JGROUP} -u root -g root -r /ajdk_multi_tenant
[ \$? -eq 0 ] || exit 1
${JAVA} -Dcom.alibaba.tenant.jgroup.rootGroup=/ajdk_multi_tenant -XX:+MultiTenant -XX:+TenantCpuThrottling -XX:+TenantCpuAccounting -XX:+UseG1GC -version
[ \$? -eq 0 ] || exit 1
${JAVA} -Dcom.alibaba.tenant.jgroup.rootGroup=/non_exists_path -XX:+MultiTenant -XX:+TenantCpuThrottling -XX:+TenantCpuAccounting -XX:+UseG1GC -version
[ \$? -ne 0 ] || exit 1
EOF

docker run --rm --privileged -v ${PWD}:${PWD} -v ${TESTJAVA}:${TESTJAVA} -w ${PWD} --entrypoint=/bin/bash ${IMAGE} -x ${PWD}${FS}${TEST_SH} 2>&1
if [ $? -ne 0 ]; then
    echo "Failed to initialize jgroup in alios5u docker container"
    exit 1
fi

echo "Done!"

#
################ Test the exceptional path ###################
# jgroup owner does not exist
echo "Testing exceptional path..."

######### Test with AliOS7u image
TEST_SH=alios7u_e1.sh
IMAGE='reg.docker.alibaba-inc.com/alibase/alios7u2'
cat >> ${TEST_SH} << EOF
#!/bin/bash
echo "Inside docker..."
yum install libcgroup-tools libcgroup -y
${JGROUP} -u NonExistUser -g NonExistGroup
exit \$?
EOF

docker run --rm --privileged -v ${PWD}:${PWD} -v ${TESTJAVA}:${TESTJAVA} -w ${PWD} --entrypoint=/bin/bash ${IMAGE} -x ${PWD}${FS}${TEST_SH} 2>&1
if [ $? -eq 0 ]; then
    echo "Failed to initialize jgroup in alios5u docker container"
    exit 1
fi

echo "Done!"

exit 0
