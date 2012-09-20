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

public class ShortLiteralTest
{

    @Test
    public void SuccessHexTests() {

        Assert.assertTrue(LiteralTools.parseShort("0x0") == 0x0);
        Assert.assertTrue(LiteralTools.parseShort("0x00") == 0x0);
        Assert.assertTrue(LiteralTools.parseShort("0x1") == 0x1);
        Assert.assertTrue(LiteralTools.parseShort("0x1234") == 0x1234);
        Assert.assertTrue(LiteralTools.parseShort("0x7fff") == 0x7fff);
        Assert.assertTrue(LiteralTools.parseShort("0x8000") == Short.MIN_VALUE);
        Assert.assertTrue(LiteralTools.parseShort("0xFFFF") == -1);

        Assert.assertTrue(LiteralTools.parseShort("-0x00") == 0);
        Assert.assertTrue(LiteralTools.parseShort("-0x01") == -1);
        Assert.assertTrue(LiteralTools.parseShort("-0x1234") == -0x1234);
        Assert.assertTrue(LiteralTools.parseShort("-0x8000") == Short.MIN_VALUE);
        Assert.assertTrue(LiteralTools.parseShort("-0x1fff") == -0x1fff);
    }

    @Test(expected=NumberFormatException.class)
    public void FaileHexTest1() {
        LiteralTools.parseShort("-0x8001");
    }

    @Test(expected=NumberFormatException.class)
    public void FailHexTest2() {
        LiteralTools.parseShort("-0xFFFF");
    }

    @Test(expected=NumberFormatException.class)
    public void FailHexTest3() {
        LiteralTools.parseShort("0x100000");
    }



  @Test
    public void SuccessDecTests() {
        Assert.assertTrue(LiteralTools.parseShort("0") == 0);
        Assert.assertTrue(LiteralTools.parseShort("1") == 1);
        Assert.assertTrue(LiteralTools.parseShort("12345") == 12345);
        Assert.assertTrue(LiteralTools.parseShort("32767") == 32767);
        Assert.assertTrue(LiteralTools.parseShort("32768") == Short.MIN_VALUE);
        Assert.assertTrue(LiteralTools.parseShort("65535") == -1);


        Assert.assertTrue(LiteralTools.parseShort("-0") == 0);
        Assert.assertTrue(LiteralTools.parseShort("-1") == -1);
        Assert.assertTrue(LiteralTools.parseShort("-12345") == -12345);
        Assert.assertTrue(LiteralTools.parseShort("-32767") == -32767);
        Assert.assertTrue(LiteralTools.parseShort("-32768") == Short.MIN_VALUE);
    }

    @Test(expected=NumberFormatException.class)
    public void FaileDecTest1() {
        LiteralTools.parseShort("-32769");
    }

    @Test(expected=NumberFormatException.class)
    public void FailDecTest2() {
        LiteralTools.parseShort("-65535");
    }

    @Test(expected=NumberFormatException.class)
    public void FailDecTest3() {
        LiteralTools.parseShort("65536");
    }

    @Test(expected=NumberFormatException.class)
    public void FailDecTest4() {
        LiteralTools.parseShort("65600");
    }


    @Test
    public void SuccessOctTests() {
        Assert.assertTrue(LiteralTools.parseShort("00") == 00);
        Assert.assertTrue(LiteralTools.parseShort("01") == 01);
        Assert.assertTrue(LiteralTools.parseShort("012345") == 012345);
        Assert.assertTrue(LiteralTools.parseShort("077777") == Short.MAX_VALUE);
        Assert.assertTrue(LiteralTools.parseShort("0100000") == Short.MIN_VALUE);
        Assert.assertTrue(LiteralTools.parseShort("0177777") == -1);


        Assert.assertTrue(LiteralTools.parseShort("-00") == 0);
        Assert.assertTrue(LiteralTools.parseShort("-01") == -1);
        Assert.assertTrue(LiteralTools.parseShort("-012345") == -012345);
        Assert.assertTrue(LiteralTools.parseShort("-077777") == -077777);
        Assert.assertTrue(LiteralTools.parseShort("-0100000") == Short.MIN_VALUE);
    }

    @Test(expected=NumberFormatException.class)
    public void FaileOctTest1() {
        LiteralTools.parseShort("-0100001");
    }

    @Test(expected=NumberFormatException.class)
    public void FailOctTest2() {
        LiteralTools.parseShort("-0177777");
    }

    @Test(expected=NumberFormatException.class)
    public void FailOctTest3() {
        LiteralTools.parseShort("0200000");
    }
}
