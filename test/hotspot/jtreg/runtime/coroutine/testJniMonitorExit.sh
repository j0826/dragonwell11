#!/bin/sh

## @test
##
## @requires os.family == "linux"
## @summary test jni MonitorExit
## @run shell testJniMonitorExit.sh
##


export LD_LIBRARY_PATH=.:${TEST_IMAGE_DIR}/lib/server:/usr/lib:$LD_LIBRARY_PATH
echo ${TEST_IMAGE_DIR}
echo $LD_LIBRARY_PATH
g++ -DLINUX -o testJniMonitorExit \
    -I${TEST_IMAGE_DIR}/include -I${TEST_IMAGE_DIR}/include/linux \
    -L${TEST_IMAGE_DIR}/lib/server \
    -ljvm -lpthread ${TESTSRC}/testJniMonitorExit.c

./testJniMonitorExit
exit $?
