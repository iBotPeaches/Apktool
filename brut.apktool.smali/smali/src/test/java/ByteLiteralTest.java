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

public class ByteLiteralTest
{

    @Test
    public void SuccessHexTests() {

        Assert.assertTrue(LiteralTools.parseByte("0x0T") == 0x0);
        Assert.assertTrue(LiteralTools.parseByte("0x00") == 0x0);
        Assert.assertTrue(LiteralTools.parseByte("0x1T") == 0x1);
        Assert.assertTrue(LiteralTools.parseByte("0x12") == 0x12);
        Assert.assertTrue(LiteralTools.parseByte("0x7fT") == 0x7f);
        Assert.assertTrue(LiteralTools.parseByte("0x80t") == Byte.MIN_VALUE);
        Assert.assertTrue(LiteralTools.parseByte("0xFFt") == -1);

        Assert.assertTrue(LiteralTools.parseByte("-0x00") == 0);
        Assert.assertTrue(LiteralTools.parseByte("-0x01") == -1);
        Assert.assertTrue(LiteralTools.parseByte("-0x12") == -0x12);
        Assert.assertTrue(LiteralTools.parseByte("-0x80") == Byte.MIN_VALUE);
        Assert.assertTrue(LiteralTools.parseByte("-0x1f") == -0x1f);
    }

    @Test(expected=NumberFormatException.class)
    public void FaileHexTest1() {
        LiteralTools.parseByte("-0x81");
    }

    @Test(expected=NumberFormatException.class)
    public void FailHexTest2() {
        LiteralTools.parseByte("-0xFF");
    }

    @Test(expected=NumberFormatException.class)
    public void FailHexTest3() {
        LiteralTools.parseByte("0x100");
    }



    @Test
    public void SuccessDecTests() {
        Assert.assertTrue(LiteralTools.parseByte("0") == 0);
        Assert.assertTrue(LiteralTools.parseByte("1t") == 1);
        Assert.assertTrue(LiteralTools.parseByte("123") == 123);
        Assert.assertTrue(LiteralTools.parseByte("127T") == 127);
        Assert.assertTrue(LiteralTools.parseByte("128") == Byte.MIN_VALUE);
        Assert.assertTrue(LiteralTools.parseByte("255") == -1);


        Assert.assertTrue(LiteralTools.parseByte("-0") == 0);
        Assert.assertTrue(LiteralTools.parseByte("-1") == -1);
        Assert.assertTrue(LiteralTools.parseByte("-123") == -123);
        Assert.assertTrue(LiteralTools.parseByte("-127") == -127);
        Assert.assertTrue(LiteralTools.parseByte("-128") == Byte.MIN_VALUE);
    }

    @Test(expected=NumberFormatException.class)
    public void FaileDecTest1() {
        LiteralTools.parseByte("-129");
    }

    @Test(expected=NumberFormatException.class)
    public void FailDecTest2() {
        LiteralTools.parseByte("-255");
    }

    @Test(expected=NumberFormatException.class)
    public void FailDecTest3() {
        LiteralTools.parseByte("256");
    }

    @Test(expected=NumberFormatException.class)
    public void FailDecTest4() {
        LiteralTools.parseByte("260");
    }


    @Test
    public void SuccessOctTests() {
        Assert.assertTrue(LiteralTools.parseByte("00") == 00);
        Assert.assertTrue(LiteralTools.parseByte("01") == 01);
        Assert.assertTrue(LiteralTools.parseByte("0123t") == 0123);
        Assert.assertTrue(LiteralTools.parseByte("0177") == Byte.MAX_VALUE);
        Assert.assertTrue(LiteralTools.parseByte("0200T") == Byte.MIN_VALUE);
        Assert.assertTrue(LiteralTools.parseByte("0377") == -1);


        Assert.assertTrue(LiteralTools.parseByte("-00") == 0);
        Assert.assertTrue(LiteralTools.parseByte("-01") == -1);
        Assert.assertTrue(LiteralTools.parseByte("-0123") == -0123);
        Assert.assertTrue(LiteralTools.parseByte("-0177") == -0177);
        Assert.assertTrue(LiteralTools.parseByte("-0200") == Byte.MIN_VALUE);
    }

    @Test(expected=NumberFormatException.class)
    public void FaileOctTest1() {
        LiteralTools.parseByte("-0201");
    }

    @Test(expected=NumberFormatException.class)
    public void FailOctTest2() {
        LiteralTools.parseByte("-0377");
    }

    @Test(expected=NumberFormatException.class)
    public void FailOctTest3() {
        LiteralTools.parseByte("0400");
    }
}
