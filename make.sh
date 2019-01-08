#!/bin/bash
if [ $# != 1 ]; then 
  echo "USAGE: $0 release/debug"
fi

DOCKER_IMAGE=reg.docker.alibaba-inc.com/ajdk/11.alios7
SCRIPT_NAME=`basename $0`
VERSION_UPDATE=3

ps -e | grep docker
if [ $? -eq 0 ]; then
    echo "We will build AJDK in Docker!"
    sudo docker pull $DOCKER_IMAGE
    sudo docker run -u admin -i --rm -e BUILD_NUMBER=$BUILD_NUMBER -v `pwd`:`pwd` -w `pwd` \
               --entrypoint=bash $DOCKER_IMAGE `pwd`/$SCRIPT_NAME $1
    exit $?
fi


LC_ALL=C
BUILD_MODE=$1

case "$BUILD_MODE" in
    release)
        DEBUG_LEVEL="release"
        JDK_IMAGES_DIR=`pwd`/build/linux-x86_64-normal-server-release/images
    ;;
    debug)
        DEBUG_LEVEL="slowdebug"
        JDK_IMAGES_DIR=`pwd`/build/linux-x86_64-normal-server-slowdebug/images
    ;;
    *)
        echo "Argument must be release or debug!"
        exit 1
    ;;
esac

NEW_JAVA_HOME=$JDK_IMAGES_DIR/jdk

if [ "x${BUILD_NUMBER}" = "x" ]; then
  BUILD_NUMBER=0
fi

export LDFLAGS_JDK="-L/vmfarm/tools/jemalloc/lib -ljemalloc"
bash ./configure --with-freetype=system \
                 --enable-unlimited-crypto \
                 --with-cacerts-file=/vmfarm/security/cacerts \
                 --with-jtreg=/vmfarm/tools/jtreg4.2 \
                 --with-jvm-variants=server \
                 --with-debug-level=$DEBUG_LEVEL \
                 --with-vendor-name="Alibaba" \
                 --with-vendor-url="http://www.alibabagroup.com" \
                 --with-vendor-bug-url="mailto:jvm@list.alibaba-inc.com" \
                 --with-version-pre="AJDK" \
                 --with-version-opt="Alibaba" \
                 --with-version-build="${BUILD_NUMBER}" \
                 --with-version-feature="11" \
                 --with-version-interim="0" \
                 --with-version-update="$VERSION_UPDATE" \
                 --with-version-date="$(date +%Y-%m-%d)" \
                 --with-zlib=system

make CONF=$BUILD_MODE LOG=cmdlines JOBS=8 images

\cp -f /vmfarm/tools/hsdis/8/amd64/hsdis-amd64.so  $NEW_JAVA_HOME/lib/
\cp -f /vmfarm/tools/jemalloc/lib/libjemalloc.so.2 $NEW_JAVA_HOME/lib/

# Sanity tests
echo "================= Start sanity test ======================"
JAVA_EXES=("$NEW_JAVA_HOME/bin/java")
VERSION_OPTS=("-version" "-Xinternalversion" "-fullversion")
for exe in "${JAVA_EXES[@]}"; do
  for opt in "${VERSION_OPTS[@]}"; do
    $exe $opt > /dev/null 2>&1
    if [ 0 -ne $? ]; then
      echo "Failed: $exe $opt"
      exit 128
    fi
  done
done

# Keep old output
$NEW_JAVA_HOME/bin/java -version

cat > /tmp/systemProperty.java << EOF
public class systemProperty {
    public static void main(String[] args) {
        System.getProperties().list(System.out);
    }
}
EOF

$NEW_JAVA_HOME/bin/javac /tmp/systemProperty.java
$NEW_JAVA_HOME/bin/java -cp /tmp/ systemProperty > /tmp/systemProperty.out

EXPECTED_PATTERN=('^java\.vm\.vendor\=.*Alibaba.*$'
                '^java\.vendor\.url\=http\:\/\/www\.alibabagroup\.com$'
                '^java\.vendor\=Alibaba$'
                '^java\.vendor\.url\.bug\=mailto\:jvm@list\.alibaba-inc\.com$')
RET=0
for p in ${EXPECTED_PATTERN[*]}
do
    cat /tmp/systemProperty.out | grep "$p"
    if [ 0 != $? ]; then RET=1; fi
done

\rm -f /tmp/systemProperty*

ldd $NEW_JAVA_HOME/lib/libzip.so|grep libz
if [ 0 != $? ]; then RET=1; fi
echo "================= Sanity test end ======================"

exit $RET
