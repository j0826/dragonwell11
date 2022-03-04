#!/bin/sh

## @test
##
## @requires os.family == "linux"
## @summary test DetachCurrentThread unpark
## @run shell testJniDetachThreadHoldingMonitor.sh
##


export LD_LIBRARY_PATH=.:${TEST_IMAGE_DIR}/lib/server:/usr/lib:$LD_LIBRARY_PATH

g++ -DLINUX -o testJniDetachThreadHoldingMonitor \
    -I${TEST_IMAGE_DIR}/include -I${TEST_IMAGE_DIR}/include/linux \
    -L${TEST_IMAGE_DIR}/lib/server \
    -ljvm -lpthread ${TESTSRC}/testJniDetachThreadHoldingMonitor.c

./testJniDetachThreadHoldingMonitor
exit $?
