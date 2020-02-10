/*
 * @test
 * @library /test/hotspot/jtreg/testlibrary
 * @summary test for aarch64 ldr encoding
 * @build TestLdrEncoding
 * @run main/othervm -XX:CompileCommand=exclude,*::toBytesDup TestLdrEncoding
 * @run main/othervm -XX:CompileCommand=exclude,*::toBytesDup TestLdrEncoding
 * @run main/othervm -XX:CompileCommand=exclude,*::toBytesDup TestLdrEncoding
 * @run main/othervm -XX:CompileCommand=exclude,*::toBytesDup TestLdrEncoding
 * @run main/othervm -XX:CompileCommand=exclude,*::toBytesDup TestLdrEncoding
 */

import sun.misc.Unsafe;
import java.util.Random;
import java.math.BigDecimal;
import java.math.BigInteger;
import static java.lang.Math.abs;
import static java.lang.Math.pow;
import static java.lang.Math.round;
import static java.math.BigInteger.TEN;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.nio.ByteOrder;
import static java.lang.String.format;
import static java.lang.System.arraycopy;

public class TestLdrEncoding {
    private static final Unsafe UNSAFE;
    static Random random = new Random();

    static {
        Field f = null;
        try {
            f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (Unsafe) f.get(null);
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }
    private static final int MAX_LENGTH = 10000;

    private static byte[] input0 = new byte[MAX_LENGTH];
    private static byte[] input1 = new byte[MAX_LENGTH];
    private static final int MAX_VALUE_LENGTH = random.nextInt(200) + 20;
    private static final byte[] MAX_VALUE = new byte[MAX_VALUE_LENGTH];
    private static final byte[] MIN_VALUE = new byte[MAX_VALUE_LENGTH];
    private static final int DECIMAL_MAX_VALUE_LENGTH = MAX_VALUE_LENGTH - 15;
    private static byte[] byteArray;

    private static final    int VALUE_TYPE = random.nextInt(100);
    private static final    int NUM_NULLS = random.nextInt(100);
    private static final    int NUM_ROWS = random.nextInt(100);
    private static final    int COUNT_DISTINCT = random.nextInt(100);
    private static final    long RAW_DATA_SIZE = (long)random.nextInt(100);
    private static final    long SUM = (long)random.nextInt(1000);
    private static final    int VERSION = random.nextInt(100);


    private static final    int DICT_OFFSET = random.nextInt(100);
    private static final    int DICT_LENGTH = random.nextInt(100);
    private static final    int HIST_OFFSET = random.nextInt(100);
    private static final    int HIST_LENGTH = random.nextInt(100);
    private static final    int DPN_OFFSET = random.nextInt(100);
    private static final    int DPN_COUNT = random.nextInt(100);

    private static final    long MAX_ROW_COUNT = (long)random.nextInt(1000);
    private static final    long MIN_ROW_COUNT = (long)random.nextInt(1000);
    private static final    long TOTAL_ROW_COUNT = (long)random.nextInt(100);
    private static final    long MAX_MEM_SIZE = (long)random.nextInt(1000);
    private static final    long MIN_MEM_SIZE = (long)random.nextInt(100);
    private static final    long TOTAL_MEM_SIZE = (long)random.nextInt(1000);

    private static final    long TOAST_OFFSET = (long)random.nextInt(1000);
    private static final    boolean HAS_TOAST = random.nextInt(100) > 50;


    private static final int MAX_STRING_LENGTH = random.nextInt(300) + 20;
    private static final byte[] MAX_STRING = new byte[MAX_STRING_LENGTH];
    private static final byte[] MIN_STRING = new byte[MAX_STRING_LENGTH];
    private static int MAX_STRINGLength = random.nextInt(100);
    private static int MIN_STRINGLength = random.nextInt(100);
    private static final boolean MAX_STRING_IS_NULL = random.nextInt(100) > 20;
    private static final boolean MIN_STRING_IS_NULL = random.nextInt(100) > 60;

    private static final short PRECISION = (short)random.nextInt(100);
    private static final short SCALE = (short)random.nextInt(100);
    private static final boolean    USE_SHORT_COMPRESS_FLOAT = random.nextInt(100) > 60;
    private static final int SHORT_SIZE = 2;
    private static final int CHAR_SIZE = 2;
    private static final int INT_SIZE = 4;
    private static final int LONG_SIZE = 8;
    private static final int FLOAT_SIZE = 4;
    private static final int DOUBLE_SIZE = 8;
    private static final int    BOOLEAN_SIZE = 1;
    private static long num = 0;
    private static void premitiveAssert(boolean flag) {
        if (flag == false) {
            throw new RuntimeException("overflow!");
        }
    }

    private static long BYTE_ARRAY_OFFSET = UNSAFE.ARRAY_BYTE_BASE_OFFSET;


    public static final void toBytes(short obj, byte[] rawBytes, int start) {
        premitiveAssert(rawBytes.length >= (start +         SHORT_SIZE));
        UNSAFE.putShort(rawBytes, (long) BYTE_ARRAY_OFFSET + start, obj);
        num += UNSAFE.getShort(rawBytes, (long) BYTE_ARRAY_OFFSET + start);
    }

    public static final void toBytes(int obj, byte[] rawBytes, int start) {
        premitiveAssert(rawBytes.length >= (start +         INT_SIZE));
        UNSAFE.putInt(rawBytes, (long) BYTE_ARRAY_OFFSET + start, obj);
        num += UNSAFE.getInt(rawBytes, (long) BYTE_ARRAY_OFFSET + start);
    }

    public static final void toBytes(long obj, byte[] rawBytes, int start) {
        premitiveAssert(rawBytes.length >= (start +         LONG_SIZE));
        UNSAFE.putLong(rawBytes, (long) BYTE_ARRAY_OFFSET + start, obj);
        num += UNSAFE.getLong(rawBytes, (long) BYTE_ARRAY_OFFSET + start);
    }

    public static final void toBytes(float obj, byte[] rawBytes, int start) {
        premitiveAssert(rawBytes.length >= (start +         FLOAT_SIZE));
        UNSAFE.putFloat(rawBytes, (long) BYTE_ARRAY_OFFSET + start, obj);
    }

    public static final void toBytes(double obj, byte[] rawBytes, int start) {
        premitiveAssert(rawBytes.length >= (start +         DOUBLE_SIZE));
        UNSAFE.putDouble(rawBytes, (long) BYTE_ARRAY_OFFSET + start, obj);
    }

    public static final void toBytes(char obj, byte[] rawBytes, int start) {
        premitiveAssert(rawBytes.length >= (start +         CHAR_SIZE));
        UNSAFE.putChar(rawBytes, (long) BYTE_ARRAY_OFFSET + start, obj);
        num += UNSAFE.getChar(rawBytes, (long) BYTE_ARRAY_OFFSET + start);
    }

    public static final void toBytes(boolean obj, byte[] rawBytes, int start) {
        premitiveAssert(rawBytes.length >= (start +         1));
        UNSAFE.putBoolean(rawBytes, (long) BYTE_ARRAY_OFFSET + start, obj);
    }

    private static int bytesSize() {
        return MAX_LENGTH;
    }
    static byte[] toBytesDup() {
        byte[] rawBytes = input1;

        int offset = 0;

        toBytes(VALUE_TYPE, rawBytes, offset);
        offset +=         INT_SIZE;

        toBytes(NUM_NULLS, rawBytes, offset);
        offset +=         INT_SIZE;

        toBytes(NUM_ROWS, rawBytes, offset);
        offset +=         INT_SIZE;

        toBytes(COUNT_DISTINCT, rawBytes, offset);
        offset +=         INT_SIZE;

        toBytes(RAW_DATA_SIZE, rawBytes, offset);
        offset +=         LONG_SIZE;

        toBytes(SUM, rawBytes, offset);
        offset +=         LONG_SIZE;
        if (VERSION > 50) {
            System.arraycopy(MAX_VALUE, 0, rawBytes, offset, MAX_VALUE_LENGTH);
            offset += MAX_VALUE_LENGTH;

            System.arraycopy(MIN_VALUE, 0, rawBytes, offset, MAX_VALUE_LENGTH);
            offset += MAX_VALUE_LENGTH;
        } else {
            System.arraycopy(MAX_VALUE, 0, rawBytes, offset, DECIMAL_MAX_VALUE_LENGTH);
            offset += DECIMAL_MAX_VALUE_LENGTH;

            System.arraycopy(MIN_VALUE, 0, rawBytes, offset, DECIMAL_MAX_VALUE_LENGTH);
            offset += DECIMAL_MAX_VALUE_LENGTH;
        }

        toBytes(DICT_OFFSET, rawBytes, offset);
        offset +=         INT_SIZE;

        toBytes(DICT_LENGTH, rawBytes, offset);
        offset +=         INT_SIZE;

        toBytes(HIST_OFFSET, rawBytes, offset);
        offset +=         INT_SIZE;

        toBytes(HIST_LENGTH, rawBytes, offset);
        offset +=         INT_SIZE;

        toBytes(DPN_OFFSET, rawBytes, offset);
        offset +=         INT_SIZE;

        toBytes(DPN_COUNT, rawBytes, offset);
        offset +=         INT_SIZE;
        if (VERSION >= 60) {
            toBytes(MAX_ROW_COUNT, rawBytes, offset);
            offset +=         LONG_SIZE;

            toBytes(MIN_ROW_COUNT, rawBytes, offset);
            offset +=         LONG_SIZE;

            toBytes(TOTAL_ROW_COUNT, rawBytes, offset);
            offset +=         LONG_SIZE;

            toBytes(MAX_MEM_SIZE, rawBytes, offset);
            offset +=         LONG_SIZE;

            toBytes(MIN_MEM_SIZE, rawBytes, offset);
            offset +=         LONG_SIZE;

            toBytes(TOTAL_MEM_SIZE, rawBytes, offset);
            offset +=         LONG_SIZE;
        }

       if (VERSION >= 65) {
           toBytes(TOAST_OFFSET, rawBytes, offset);
           offset +=         LONG_SIZE;
           toBytes(HAS_TOAST, rawBytes, offset);
           offset +=         BOOLEAN_SIZE;
       }
       if (VERSION >= 70) {
          System.arraycopy(MAX_STRING, 0, rawBytes, offset, MAX_STRING_LENGTH);
          offset += MAX_STRING_LENGTH;

          System.arraycopy(MIN_STRING, 0, rawBytes, offset, MAX_STRING_LENGTH);
          offset += MAX_STRING_LENGTH;

          toBytes(MAX_STRINGLength, rawBytes, offset);
          offset +=         INT_SIZE;

          toBytes(MIN_STRINGLength, rawBytes, offset);
          offset +=         INT_SIZE;

          toBytes(MAX_STRING_IS_NULL, rawBytes, offset);
          offset +=         BOOLEAN_SIZE;

          toBytes(MIN_STRING_IS_NULL, rawBytes, offset);
          offset +=         BOOLEAN_SIZE;
       }

       if (VERSION >= 75) {
           toBytes(PRECISION, rawBytes, offset);
           offset +=         SHORT_SIZE;

           toBytes(SCALE, rawBytes, offset);
           offset +=         SHORT_SIZE;
       }

       if (VERSION >= 80) {
           toBytes(USE_SHORT_COMPRESS_FLOAT, rawBytes, offset);
           offset +=         BOOLEAN_SIZE;
       }
        return rawBytes;
    }


    static byte[] toBytes() {
        byte[] rawBytes = input0;

        int offset = 0;

        toBytes(VALUE_TYPE, rawBytes, offset);
        offset +=         INT_SIZE;

        toBytes(NUM_NULLS, rawBytes, offset);
        offset +=         INT_SIZE;

        toBytes(NUM_ROWS, rawBytes, offset);
        offset +=         INT_SIZE;

        toBytes(COUNT_DISTINCT, rawBytes, offset);
        offset +=         INT_SIZE;

        toBytes(RAW_DATA_SIZE, rawBytes, offset);
        offset +=         LONG_SIZE;

        toBytes(SUM, rawBytes, offset);
        offset +=         LONG_SIZE;

        //if (version.getVersion() < DetailColumn.VERSION_EIGHT) {
        if (VERSION > 50) {
            System.arraycopy(MAX_VALUE, 0, rawBytes, offset, MAX_VALUE_LENGTH);
            offset += MAX_VALUE_LENGTH;

            System.arraycopy(MIN_VALUE, 0, rawBytes, offset, MAX_VALUE_LENGTH);
            offset += MAX_VALUE_LENGTH;
        } else {
            System.arraycopy(MAX_VALUE, 0, rawBytes, offset, DECIMAL_MAX_VALUE_LENGTH);
            offset += DECIMAL_MAX_VALUE_LENGTH;

            System.arraycopy(MIN_VALUE, 0, rawBytes, offset, DECIMAL_MAX_VALUE_LENGTH);
            offset += DECIMAL_MAX_VALUE_LENGTH;
        }

        toBytes(DICT_OFFSET, rawBytes, offset);
        offset +=         INT_SIZE;

        toBytes(DICT_LENGTH, rawBytes, offset);
        offset +=         INT_SIZE;

        toBytes(HIST_OFFSET, rawBytes, offset);
        offset +=         INT_SIZE;

        toBytes(HIST_LENGTH, rawBytes, offset);
        offset +=         INT_SIZE;

        toBytes(DPN_OFFSET, rawBytes, offset);
        offset +=         INT_SIZE;

        toBytes(DPN_COUNT, rawBytes, offset);
        offset +=         INT_SIZE;

        if (VERSION >= 60) {
            toBytes(MAX_ROW_COUNT, rawBytes, offset);
            offset +=         LONG_SIZE;

            toBytes(MIN_ROW_COUNT, rawBytes, offset);
            offset +=         LONG_SIZE;

            toBytes(TOTAL_ROW_COUNT, rawBytes, offset);
            offset +=         LONG_SIZE;

            toBytes(MAX_MEM_SIZE, rawBytes, offset);
            offset +=         LONG_SIZE;

            toBytes(MIN_MEM_SIZE, rawBytes, offset);
            offset +=         LONG_SIZE;

            toBytes(TOTAL_MEM_SIZE, rawBytes, offset);
            offset +=         LONG_SIZE;
        }

       if (VERSION >= 65) {
           toBytes(TOAST_OFFSET, rawBytes, offset);
           offset +=         LONG_SIZE;
           toBytes(HAS_TOAST, rawBytes, offset);
           offset +=         BOOLEAN_SIZE;
       }

       if (VERSION >= 70) {
          System.arraycopy(MAX_STRING, 0, rawBytes, offset, MAX_STRING_LENGTH);
          offset += MAX_STRING_LENGTH;

          System.arraycopy(MIN_STRING, 0, rawBytes, offset, MAX_STRING_LENGTH);
          offset += MAX_STRING_LENGTH;

          toBytes(MAX_STRINGLength, rawBytes, offset);
          offset +=         INT_SIZE;

          toBytes(MIN_STRINGLength, rawBytes, offset);
          offset +=         INT_SIZE;

          toBytes(MAX_STRING_IS_NULL, rawBytes, offset);
          offset +=         BOOLEAN_SIZE;

          toBytes(MIN_STRING_IS_NULL, rawBytes, offset);
          offset +=         BOOLEAN_SIZE;
       }

       if (VERSION >= 75) {
           toBytes(PRECISION, rawBytes, offset);
           offset +=         SHORT_SIZE;

           toBytes(SCALE, rawBytes, offset);
           offset +=         SHORT_SIZE;
       }

       if (VERSION >= 80) {
           toBytes(USE_SHORT_COMPRESS_FLOAT, rawBytes, offset);
           offset +=         BOOLEAN_SIZE;
       }
        return rawBytes;
    }


    public static void main(String[] args) throws Throwable {
        long s = 0, s1 = 0;
        for (int i = 0; i < input0.length; i++) {
            input0[i] = 0;
            input1[i] = 0;
        }
        for (int i = 0; i < 100000; i++) {
            s += toBytes()[0];
            s1 += toBytesDup()[0];
            for (int j = 0; j < input0.length; j++) {
                if (input0[j] != input1[j]) {
                    throw new RuntimeException("not match!");
                }
            }
        }
        System.out.println("ddd " + s + " " + s1);
        System.out.println("nnn " + num);
    }
}

