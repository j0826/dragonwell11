/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.

 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test
 * @bug 8235385
 * @summary Crash on aarch64 JDK due to long offset
 *
 * @modules jdk.unsupported/sun.misc
 * @run testng/othervm -Diters=20000 -XX:-TieredCompilation  compiler.unsafe.MisalignedAccesses
 */

package compiler.unsafe;

import org.testng.annotations.Test;

import java.lang.reflect.Field;

import static org.testng.Assert.*;

public class MisalignedAccesses {
    static final int ITERS = Integer.getInteger("iters", 1);

    static final sun.misc.Unsafe UNSAFE;

    static final long BYTE_ARRAY_OFFSET;

    static final byte[] BYTE_ARRAY = new byte[4096];

    static {
        try {
            Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (sun.misc.Unsafe) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Unable to get Unsafe instance.", e);
        }
        BYTE_ARRAY_OFFSET = UNSAFE.arrayBaseOffset(byte[].class);
    }

    @Test
    static long testBytes() {
        long sum = 0;
        sum += UNSAFE.getByte(BYTE_ARRAY, 1 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 38 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 75 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 112 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 149 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 186 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 223 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 260 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 297 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 334 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 371 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 408 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 445 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 482 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 519 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 556 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 593 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 630 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 667 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 704 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 741 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 778 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 815 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 852 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 889 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 926 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 963 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 1000 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 1037 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 1074 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 1111 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 1148 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 1185 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 1222 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 1259 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 1296 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 1333 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 1370 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 1407 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 1444 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 1481 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 1518 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 1555 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 1592 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 1629 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 1666 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 1703 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 1740 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 1777 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 1814 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 1851 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 1888 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 1925 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 1962 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 1999 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 2036 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 2073 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 2110 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 2147 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 2184 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 2221 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 2258 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 2295 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 2332 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 2369 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 2406 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 2443 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 2480 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 2517 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 2554 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 2591 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 2628 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 2665 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 2702 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 2739 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 2776 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 2813 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 2850 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 2887 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 2924 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 2961 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 2998 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 3035 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 3072 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 3109 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 3146 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 3183 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 3220 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 3257 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 3294 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 3331 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 3368 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 3405 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 3442 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 3479 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 3516 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 3553 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 3590 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 3627 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 3664 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 3701 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 3738 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 3775 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 3812 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 3849 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 3886 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 3923 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 3960 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(BYTE_ARRAY, 3997 + BYTE_ARRAY_OFFSET);
        return sum;
    }

    @Test
    static long testShorts() {
        long sum = 0;
        sum += UNSAFE.getShort(BYTE_ARRAY, 1 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 38 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 75 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 112 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 149 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 186 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 223 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 260 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 297 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 334 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 371 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 408 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 445 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 482 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 519 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 556 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 593 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 630 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 667 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 704 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 741 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 778 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 815 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 852 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 889 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 926 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 963 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 1000 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 1037 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 1074 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 1111 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 1148 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 1185 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 1222 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 1259 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 1296 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 1333 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 1370 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 1407 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 1444 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 1481 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 1518 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 1555 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 1592 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 1629 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 1666 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 1703 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 1740 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 1777 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 1814 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 1851 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 1888 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 1925 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 1962 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 1999 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 2036 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 2073 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 2110 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 2147 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 2184 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 2221 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 2258 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 2295 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 2332 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 2369 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 2406 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 2443 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 2480 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 2517 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 2554 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 2591 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 2628 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 2665 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 2702 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 2739 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 2776 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 2813 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 2850 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 2887 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 2924 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 2961 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 2998 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 3035 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 3072 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 3109 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 3146 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 3183 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 3220 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 3257 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 3294 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 3331 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 3368 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 3405 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 3442 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 3479 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 3516 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 3553 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 3590 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 3627 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 3664 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 3701 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 3738 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 3775 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 3812 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 3849 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 3886 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 3923 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 3960 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(BYTE_ARRAY, 3997 + BYTE_ARRAY_OFFSET);
        return sum;
    }

    @Test
    static long testInts() {
        long sum = 0;
        sum += UNSAFE.getInt(BYTE_ARRAY, 1 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 38 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 75 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 112 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 149 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 186 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 223 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 260 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 297 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 334 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 371 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 408 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 445 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 482 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 519 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 556 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 593 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 630 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 667 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 704 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 741 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 778 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 815 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 852 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 889 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 926 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 963 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 1000 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 1037 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 1074 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 1111 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 1148 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 1185 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 1222 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 1259 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 1296 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 1333 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 1370 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 1407 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 1444 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 1481 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 1518 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 1555 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 1592 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 1629 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 1666 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 1703 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 1740 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 1777 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 1814 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 1851 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 1888 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 1925 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 1962 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 1999 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 2036 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 2073 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 2110 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 2147 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 2184 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 2221 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 2258 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 2295 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 2332 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 2369 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 2406 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 2443 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 2480 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 2517 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 2554 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 2591 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 2628 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 2665 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 2702 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 2739 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 2776 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 2813 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 2850 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 2887 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 2924 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 2961 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 2998 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 3035 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 3072 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 3109 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 3146 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 3183 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 3220 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 3257 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 3294 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 3331 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 3368 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 3405 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 3442 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 3479 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 3516 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 3553 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 3590 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 3627 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 3664 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 3701 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 3738 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 3775 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 3812 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 3849 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 3886 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 3923 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 3960 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(BYTE_ARRAY, 3997 + BYTE_ARRAY_OFFSET);
        return sum;
    }

    @Test
    static long testLongs() {
        long sum = 0;
        sum += UNSAFE.getLong(BYTE_ARRAY, 1 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 38 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 75 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 112 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 149 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 186 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 223 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 260 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 297 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 334 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 371 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 408 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 445 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 482 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 519 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 556 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 593 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 630 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 667 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 704 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 741 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 778 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 815 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 852 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 889 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 926 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 963 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 1000 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 1037 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 1074 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 1111 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 1148 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 1185 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 1222 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 1259 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 1296 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 1333 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 1370 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 1407 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 1444 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 1481 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 1518 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 1555 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 1592 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 1629 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 1666 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 1703 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 1740 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 1777 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 1814 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 1851 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 1888 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 1925 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 1962 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 1999 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 2036 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 2073 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 2110 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 2147 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 2184 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 2221 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 2258 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 2295 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 2332 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 2369 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 2406 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 2443 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 2480 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 2517 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 2554 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 2591 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 2628 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 2665 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 2702 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 2739 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 2776 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 2813 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 2850 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 2887 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 2924 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 2961 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 2998 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 3035 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 3072 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 3109 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 3146 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 3183 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 3220 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 3257 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 3294 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 3331 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 3368 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 3405 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 3442 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 3479 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 3516 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 3553 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 3590 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 3627 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 3664 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 3701 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 3738 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 3775 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 3812 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 3849 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 3886 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 3923 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 3960 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(BYTE_ARRAY, 3997 + BYTE_ARRAY_OFFSET);
        return sum;
    }

    @Test
    static long testFloats() {
        long sum = 0;
        sum += UNSAFE.getFloat(BYTE_ARRAY, 1 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 38 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 75 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 112 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 149 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 186 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 223 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 260 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 297 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 334 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 371 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 408 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 445 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 482 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 519 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 556 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 593 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 630 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 667 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 704 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 741 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 778 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 815 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 852 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 889 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 926 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 963 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 1000 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 1037 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 1074 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 1111 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 1148 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 1185 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 1222 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 1259 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 1296 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 1333 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 1370 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 1407 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 1444 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 1481 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 1518 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 1555 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 1592 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 1629 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 1666 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 1703 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 1740 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 1777 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 1814 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 1851 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 1888 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 1925 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 1962 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 1999 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 2036 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 2073 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 2110 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 2147 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 2184 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 2221 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 2258 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 2295 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 2332 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 2369 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 2406 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 2443 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 2480 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 2517 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 2554 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 2591 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 2628 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 2665 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 2702 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 2739 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 2776 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 2813 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 2850 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 2887 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 2924 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 2961 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 2998 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 3035 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 3072 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 3109 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 3146 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 3183 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 3220 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 3257 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 3294 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 3331 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 3368 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 3405 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 3442 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 3479 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 3516 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 3553 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 3590 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 3627 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 3664 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 3701 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 3738 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 3775 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 3812 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 3849 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 3886 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 3923 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 3960 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(BYTE_ARRAY, 3997 + BYTE_ARRAY_OFFSET);
        return sum;
    }

    @Test
    static long testDoubles() {
        long sum = 0;
        sum += UNSAFE.getDouble(BYTE_ARRAY, 1 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 38 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 75 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 112 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 149 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 186 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 223 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 260 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 297 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 334 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 371 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 408 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 445 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 482 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 519 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 556 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 593 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 630 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 667 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 704 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 741 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 778 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 815 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 852 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 889 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 926 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 963 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 1000 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 1037 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 1074 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 1111 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 1148 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 1185 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 1222 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 1259 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 1296 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 1333 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 1370 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 1407 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 1444 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 1481 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 1518 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 1555 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 1592 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 1629 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 1666 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 1703 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 1740 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 1777 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 1814 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 1851 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 1888 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 1925 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 1962 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 1999 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 2036 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 2073 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 2110 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 2147 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 2184 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 2221 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 2258 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 2295 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 2332 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 2369 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 2406 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 2443 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 2480 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 2517 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 2554 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 2591 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 2628 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 2665 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 2702 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 2739 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 2776 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 2813 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 2850 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 2887 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 2924 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 2961 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 2998 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 3035 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 3072 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 3109 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 3146 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 3183 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 3220 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 3257 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 3294 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 3331 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 3368 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 3405 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 3442 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 3479 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 3516 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 3553 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 3590 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 3627 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 3664 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 3701 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 3738 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 3775 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 3812 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 3849 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 3886 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 3923 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 3960 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(BYTE_ARRAY, 3997 + BYTE_ARRAY_OFFSET);
        return sum;
    }


    static volatile long result;

    public static void main(String[] args) {
        for (int i = 0; i < ITERS; i++) {
            result += testBytes();
            result += testShorts();
            result += testInts();
            result += testLongs();
            result += testFloats();
            result += testDoubles();
        }
    }
}

