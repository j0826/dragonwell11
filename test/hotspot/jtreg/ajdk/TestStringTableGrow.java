/*
 * @test StringTable grow up with invalid Node
 * @modules java.base/java.lang:+open
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @summary Test Stringtable expansion with invalid node
 * @run driver ClassFileInstaller sun.hotspot.WhiteBox
 *                                sun.hotspot.WhiteBox$WhiteBoxPermission
 * @run main/othervm -Xbootclasspath/a:. -XX:+IgnoreInvalidEntryAtStringTableExpansion -Xlog:stringtable=trace -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI TestStringTableGrow
 */

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import sun.hotspot.WhiteBox;

public class TestStringTableGrow {

    public static final String FEEDBACK_ADDR = "http://ifs.k2.et2.vipserver/feedback";
    public static final String FEEDBACK_ADDR2 = "http://k2.vipserver.tbsite.net/feedback";

    public static void main(String[] args) {

        String feedback2Internal = "http://k2.vipserver.tbsite.net/feedback".intern();
        String feedbackInternal = "http://ifs.k2.et2.vipserver/feedback".intern();

        System.out.println("before convert");
        System.out.println("http://ifs.k2.et2.vipserver/feedback = " + FEEDBACK_ADDR);
        System.out.println("http://k2.vipserver.tbsite.net/feedback = " + FEEDBACK_ADDR2);
        System.out.println("http://ifs.k2.et2.vipserver/feedback = " + feedbackInternal);
        System.out.println("http://k2.vipserver.tbsite.net/feedback = " + feedback2Internal);

        // use reflect to change the result value of String.intern()
        try {
            Field[] array = String.class.getDeclaredFields();
            int length = array.length;

            for(int i = 0; i < length; ++i) {
                Field f = array[i];
                f.setAccessible(true);
                if (!Modifier.isStatic(f.getModifiers())) {
                    f.set("http://k2.vipserver.tbsite.net/feedback", f.get("http://ifs.k2.et2.vipserver/feedback"));
                }
            }
        } catch(Exception e) {
        }

        // After the above code, the content of oop in Node with String "http://k2.vipserver.tbsite.net/feedback" is changed.
        // the node in StringTable is invalid.

        System.out.println("after convert");
        System.out.println("http://ifs.k2.et2.vipserver/feedback = " + FEEDBACK_ADDR);
        System.out.println("http://k2.vipserver.tbsite.net/feedback = " + FEEDBACK_ADDR2);
        System.out.println("http://ifs.k2.et2.vipserver/feedback = " + feedbackInternal);
        System.out.println("http://k2.vipserver.tbsite.net/feedback = " + feedback2Internal);

        System.out.println("new intern");
        String combineString1 = "http://k2.vipserver.";
        String combineString2 = "tbsite.net/feedback";
        System.out.println("combineString1 = " + combineString1);
        System.out.println("combineString2 = " + combineString2);
        String internString = (combineString1 + combineString2).intern();
        System.out.println("http://k2.vipserver.tbsite.net/feedback = " + internString);

        // use reflect to change the result value of String.intern()
        try {
            Field[] array = String.class.getDeclaredFields();
            int length = array.length;

            for(int i = 0; i < length; ++i) {
                Field f = array[i];
                f.setAccessible(true);
                if (!Modifier.isStatic(f.getModifiers())) {
                    f.set(internString, f.get("http://ifs.k2.et2.vipserver/feedback"));
                }
            }
        } catch(Exception e) {
        }
        // After the above code, the content of oop in Node with String "http://k2.vipserver.tbsite.net/feedback" is changed.
        // the node in StringTable is invalid.

        System.out.println("second convert");
        System.out.println("http://k2.vipserver.tbsite.net/feedback = " + internString);

        // directly call StringTable::grow, then the invalid node is founded.
        WhiteBox wb = WhiteBox.getWhiteBox();
        wb.growStringTable();

        System.out.println("http://ifs.k2.et2.vipserver/feedback = " + FEEDBACK_ADDR);
        System.out.println("http://k2.vipserver.tbsite.net/feedback = " + FEEDBACK_ADDR2);
        System.out.println("http://ifs.k2.et2.vipserver/feedback = " + feedbackInternal);
        System.out.println("http://k2.vipserver.tbsite.net/feedback = " + feedback2Internal);
        System.out.println("http://k2.vipserver.tbsite.net/feedback = " + internString);
    }
}
