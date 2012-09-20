/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver (JesusFreke)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import org.jf.smali.LiteralTools;
import org.junit.Assert;
import org.junit.Test;

public class LongLiteralTest
{
    @Test
    public void SuccessHexTests() {
        Assert.assertTrue(LiteralTools.parseLong("0x0L") == 0x0);
        Assert.assertTrue(LiteralTools.parseLong("0x00L") == 0x0);
        Assert.assertTrue(LiteralTools.parseLong("0x1L") == 0x1);
        Assert.assertTrue(LiteralTools.parseLong("0x1234567890123456L") == 0x1234567890123456L);
        Assert.assertTrue(LiteralTools.parseLong("0x7fffffffffffffffL") == 0x7fffffffffffffffL);
        Assert.assertTrue(LiteralTools.parseLong("0x8000000000000000L") == Long.MIN_VALUE);
        Assert.assertTrue(LiteralTools.parseLong("0xFFFFFFFFFFFFFFFFL") == -1);

        Assert.assertTrue(LiteralTools.parseLong("-0x00L") == 0);
        Assert.assertTrue(LiteralTools.parseLong("-0x01L") == -1);
        Assert.assertTrue(LiteralTools.parseLong("-0x1234567890123456L") == -0x1234567890123456L);
        Assert.assertTrue(LiteralTools.parseLong("-0x8000000000000000L") == Long.MIN_VALUE);
        Assert.assertTrue(LiteralTools.parseLong("-0x1fffffffffffffffL") == -0x1fffffffffffffffL);
    }

    @Test(expected=NumberFormatException.class)
    public void FaileHexTest1() {
        LiteralTools.parseLong("-0x8000000000000001");
    }

    @Test(expected=NumberFormatException.class)
    public void FailHexTest2() {
        LiteralTools.parseLong("-0xFFFFFFFFFFFFFFFF");
    }

    @Test(expected=NumberFormatException.class)
    public void FailHexTest3() {
        LiteralTools.parseLong("0x10000000000000000");
    }

    @Test
    public void SuccessDecTests() {
        Assert.assertTrue(LiteralTools.parseLong("0L") == 0);
        Assert.assertTrue(LiteralTools.parseLong("1") == 1);
        Assert.assertTrue(LiteralTools.parseLong("1234567890123456789") == 1234567890123456789L);
        Assert.assertTrue(LiteralTools.parseLong("9223372036854775807") == 9223372036854775807L);
        Assert.assertTrue(LiteralTools.parseLong("9223372036854775808") == Long.MIN_VALUE);
        Assert.assertTrue(LiteralTools.parseLong("18446744073709551615L") == -1);

        Assert.assertTrue(LiteralTools.parseLong("-0") == 0);
        Assert.assertTrue(LiteralTools.parseLong("-1") == -1);
        Assert.assertTrue(LiteralTools.parseLong("-1234567890123456789") == -1234567890123456789L);
        Assert.assertTrue(LiteralTools.parseLong("-9223372036854775807") == -9223372036854775807L);
        Assert.assertTrue(LiteralTools.parseLong("-9223372036854775808") == Long.MIN_VALUE);
    }

    @Test(expected=NumberFormatException.class)
    public void FaileDecTest1() {
        LiteralTools.parseLong("-9223372036854775809");
    }

    @Test(expected=NumberFormatException.class)
    public void FailDecTest2() {
        LiteralTools.parseLong("-18446744073709551616");
    }

    @Test(expected=NumberFormatException.class)
    public void FailDecTest3() {
        LiteralTools.parseLong("18446744073709551617");
    }

    @Test(expected=NumberFormatException.class)
    public void FailDecTest4() {
        LiteralTools.parseLong("18446744073709551700");
    }

    @Test
    public void SuccessOctTests() {
        Assert.assertTrue(LiteralTools.parseLong("00") == 00);
        Assert.assertTrue(LiteralTools.parseLong("01") == 01);
        Assert.assertTrue(LiteralTools.parseLong("0123456701234567012345") == 0123456701234567012345L);
        Assert.assertTrue(LiteralTools.parseLong("0777777777777777777777") == Long.MAX_VALUE);
        Assert.assertTrue(LiteralTools.parseLong("01000000000000000000000") == Long.MIN_VALUE);
        Assert.assertTrue(LiteralTools.parseLong("01777777777777777777777") == -1);

        Assert.assertTrue(LiteralTools.parseLong("-00") == 0);
        Assert.assertTrue(LiteralTools.parseLong("-01") == -1);
        Assert.assertTrue(LiteralTools.parseLong("-0123456701234567012345") == -0123456701234567012345L);
        Assert.assertTrue(LiteralTools.parseLong("-0777777777777777777777") == -0777777777777777777777L);
        Assert.assertTrue(LiteralTools.parseLong("-01000000000000000000000") == Long.MIN_VALUE);
    }

    @Test(expected=NumberFormatException.class)
    public void FaileOctTest1() {
        LiteralTools.parseLong("-01000000000000000000001");
    }

    @Test(expected=NumberFormatException.class)
    public void FailOctTest2() {
        LiteralTools.parseLong("-01777777777777777777777");
    }

    @Test(expected=NumberFormatException.class)
    public void FailOctTest3() {
        LiteralTools.parseLong("02000000000000000000000");
    }
}
