# @test
# @summary test disable Interface method ref check
# @run shell TestDisableInterfaceMethodrefCheck.sh

JAVA=${TESTJAVA}/bin/java
JAVAC=${TESTJAVA}/bin/javac
#wget http://central.maven.org/maven2/org/clojure/clojure/1.9.0/clojure-1.9.0.jar
#wget http://central.maven.org/maven2/org/clojure/spec.alpha/0.1.143/spec.alpha-0.1.143.jar
cp -r /vmfarm/www/clojure/*.jar .
echo "
import java.lang.String;
public interface Foo {
  public static String bar() {
    return \"test\";
  }
}" > Foo.java


$JAVAC Foo.java
$JAVA -XX:+UnlockDiagnosticVMOptions  -XX:+DisableInterfaceMethodrefCheck -cp ./clojure-1.9.0.jar:spec.alpha-0.1.143.jar:spec.alpha-0.1.143.jar:. clojure.main -e \(Foo/bar\)

if [ $? != '0' ]
then
        printf "InterfaceMethodrefCheck failed!"
        exit 1
fi

rm -rf clojure-1.9.0.jar
rm -rf spec.alpha-0.1.143.jar
rm -rf Foo.java
rm -rf Foo.class

