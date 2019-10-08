/*
*
* Copyright (c) 2019 Alibaba Group Holding Limited. All Rights Reserved.
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
*
* This code is free software; you can redistribute it and/or modify it
* under the terms of the GNU General Public License version 2 only, as
* published by the Free Software Foundation. Alibaba designates this
* particular file as subject to the "Classpath" exception as provided
* by Oracle in the LICENSE file that accompanied this code.
*
* This code is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
* FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
* version 2 for more details (a copy is included in the LICENSE file that
* accompanied this code).
*
* You should have received a copy of the GNU General Public License version
* 2 along with this work; if not, write to the Free Software Foundation,
* Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
*
*/

import java.util.Base64;
import java.util.Random;


/*
 *  @test
 *  @summary a test for vpandq encoding fix
 *  @run main/othervm TestVpandqEncoding
 *
 */

public class TestVpandqEncoding {
    private static boolean isURL = false;
    private static byte[] newline = null;
    private static int linemax = -1;
    private static boolean doPadding = true;
    public static Random random = new Random();

    public static int encode(byte[] src, byte[] dst) {
        return encode0(src, 0, src.length, dst);
    }
    private static final char[] TO_BASE_64 = {
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
        'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
        'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'
    };
    private static void encodeBlock(byte[] src, int sp, int sl, byte[] dst, int dp, boolean isURL) {
        char[] base64 = TO_BASE_64;
        for (int sp0 = sp, dp0 = dp ; sp0 < sl; ) {
            int bits = (src[sp0++] & 0xff) << 16 |
                       (src[sp0++] & 0xff) <<  8 |
                       (src[sp0++] & 0xff);
            dst[dp0++] = (byte)base64[(bits >>> 18) & 0x3f];
            dst[dp0++] = (byte)base64[(bits >>> 12) & 0x3f];
            dst[dp0++] = (byte)base64[(bits >>> 6)  & 0x3f];
            dst[dp0++] = (byte)base64[bits & 0x3f];
        }
    }
    private static int encode0(byte[] src, int off, int end, byte[] dst) {
        char[] base64 = TO_BASE_64;
        int sp = off;
        int slen = (end - off) / 3 * 3;
        int sl = off + slen;
        if (linemax > 0 && slen  > linemax / 4 * 3) {
            slen = linemax / 4 * 3;
        }
        int dp = 0;
        while (sp < sl) {
            int sl0 = Math.min(sp + slen, sl);
            encodeBlock(src, sp, sl0, dst, dp, isURL);
            int dlen = (sl0 - sp) / 3 * 4;
            dp += dlen;
            sp = sl0;
            if (dlen == linemax && sp < end) {
                for (byte b : newline){
                   dst[dp++] = b;
                }
            }
        }
        if (sp < end) {               // 1 or 2 leftover bytes
            int b0 = src[sp++] & 0xff;
            dst[dp++] = (byte)base64[b0 >> 2];
            if (sp == end) {
                dst[dp++] = (byte)base64[(b0 << 4) & 0x3f];
                if (doPadding) {
                    dst[dp++] = '=';
                    dst[dp++] = '=';
                }
            } else {
                int b1 = src[sp++] & 0xff;
                dst[dp++] = (byte)base64[(b0 << 4) & 0x3f | (b1 >> 4)];
                dst[dp++] = (byte)base64[(b1 << 2) & 0x3f];
                if (doPadding) {
                    dst[dp++] = '=';
                }
            }
        }
        return dp;
    }

    public static void main(String[] args) {
        byte[] a = new byte[1000];
        byte[] b = new byte[5000];
        long sum = 0;
        byte[] c = new byte[5000];
        long sum0 = 0;
        for (int i = 0; i < 100000; i++) {
            random.nextBytes(a);
            random.nextBytes(b);
            System.arraycopy(b, 0, c, 0, b.length);
            sum += Base64.getEncoder().encode(a, b);
            sum0 += encode(a, c);
        }
        if (sum != sum0) {
            throw new RuntimeException("wrong sum result!");
        }
        for (int i = 0; i < 5000; i++) {
            if (b[i] != c[i]) {
                throw new RuntimeException("wrong encode result!");
            }
        }
        System.out.println(sum);
    }
}


