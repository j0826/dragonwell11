#!/bin/bash
ARGNUM=$#
if [ $ARGNUM != 1 -a $ARGNUM != 3 ]; then 
  echo "USAGE: $0 release/debug or $0 release/debug username uid (docker mode)"
  exit
elif [ $# -eq 1 ]; then
  PREBUILD=0
else
  PREBUILD=1
fi

DOCKER_IMAGE=reg.docker.alibaba-inc.com/ajdk/11.alios7
SCRIPT_NAME=`basename $0`
VERSION_UPDATE=3
MX=`pwd`/mx/mx
BUILD_UID=`id -u ${USER}`

ps -e | grep docker
if [ $? -eq 0 ]; then
    echo "We will build AJDK in Docker!"
    sudo docker pull $DOCKER_IMAGE
    sudo docker run -i --rm -e BUILD_NUMBER=$BUILD_NUMBER -v `pwd`:`pwd` -w `pwd` \
               --entrypoint=bash $DOCKER_IMAGE `pwd`/$SCRIPT_NAME $1 $USER $BUILD_UID
    exit $?
fi

LC_ALL=C
BUILD_MODE=$1

if [ $PREBUILD -eq 1 ]; then
    # create user in docker image
    BUILD_USER=$2
    BUILD_UID=$3
    if [ $BUILD_UID != "500" ]; then
        BUILD_USER='buildadmin'   # fake build user
        useradd -u $BUILD_UID -G users -d /tmp/${BUILD_USER}_home $BUILD_USER
    else
        BUILD_USER='admin' # 'admin' user exists in docker image
    fi
    sudo su $BUILD_USER `pwd`/$SCRIPT_NAME $BUILD_MODE
    exit $?
fi

# update graal to openjdk
WORKDIR=`pwd`
cd graalvm/compiler
$MX --java-home /vmfarm/ajdk11 updategraalinopenjdk $WORKDIR 11
cd ../..

# jaotc.test requires junit, now we can not run junit in build process
rm -rf src/jdk.aot/share/classes/jdk.tools.jaotc.test

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
                 --with-zlib=system \
                 --with-jvm-features=zgc
make CONF=$BUILD_MODE LOG=cmdlines JOBS=8 images

# recover modified components
rm -rf src/jdk.internal.vm.compiler
rm -rf src/jdk.aot
rm -rf src/jdk.internal.vm.compiler.management
rm -f make/CompileJavaModules.gmk
git checkout src/jdk.internal.vm.compiler
git checkout src/jdk.aot
git checkout src/jdk.internal.vm.compiler.management
git checkout make/CompileJavaModules.gmk

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

# sanity check for jvmci compiler
$NEW_JAVA_HOME/bin/java -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI -XX:+UseJVMCICompiler -XX:+BootstrapJVMCI -version
if [ 0 != $? ]; then RET=1; fi
echo "================= Sanity test end ======================"

exit $RET
