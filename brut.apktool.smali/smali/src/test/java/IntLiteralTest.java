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

public class IntLiteralTest
{
    @Test
    public void SuccessHexTests() {

        Assert.assertTrue(LiteralTools.parseInt("0x0") == 0x0);
        Assert.assertTrue(LiteralTools.parseInt("0x00") == 0x0);
        Assert.assertTrue(LiteralTools.parseInt("0x1") == 0x1);
        Assert.assertTrue(LiteralTools.parseInt("0x12345678") == 0x12345678);
        Assert.assertTrue(LiteralTools.parseInt("0x7fffffff") == 0x7fffffff);
        Assert.assertTrue(LiteralTools.parseInt("0x80000000") == Integer.MIN_VALUE);
        Assert.assertTrue(LiteralTools.parseInt("0xFFFFFFFF") == -1);

        Assert.assertTrue(LiteralTools.parseInt("-0x00") == 0);
        Assert.assertTrue(LiteralTools.parseInt("-0x01") == -1);
        Assert.assertTrue(LiteralTools.parseInt("-0x12345678") == -0x12345678);
        Assert.assertTrue(LiteralTools.parseInt("-0x80000000") == Integer.MIN_VALUE);
        Assert.assertTrue(LiteralTools.parseInt("-0x1FFFFFFF") == -0x1FFFFFFF);
    }

    @Test(expected=NumberFormatException.class)
    public void FaileHexTest1() {
        LiteralTools.parseInt("-0x80000001");
    }

    @Test(expected=NumberFormatException.class)
    public void FailHexTest2() {
        LiteralTools.parseInt("-0xFFFFFFFF");
    }

    @Test(expected=NumberFormatException.class)
    public void FailHexTest3() {
        LiteralTools.parseInt("0x100000000");
    }



    @Test
    public void SuccessDecTests() {
        Assert.assertTrue(LiteralTools.parseInt("0") == 0);
        Assert.assertTrue(LiteralTools.parseInt("1") == 1);
        Assert.assertTrue(LiteralTools.parseInt("1234567890") == 1234567890);
        Assert.assertTrue(LiteralTools.parseInt("2147483647") == 2147483647);
        Assert.assertTrue(LiteralTools.parseInt("2147483648") == Integer.MIN_VALUE);
        Assert.assertTrue(LiteralTools.parseInt("4294967295") == -1);


        Assert.assertTrue(LiteralTools.parseInt("-0") == 0);
        Assert.assertTrue(LiteralTools.parseInt("-1") == -1);
        Assert.assertTrue(LiteralTools.parseInt("-1234567890") == -1234567890);
        Assert.assertTrue(LiteralTools.parseInt("-2147483647") == -2147483647);
        Assert.assertTrue(LiteralTools.parseInt("-2147483648") == Integer.MIN_VALUE);
    }

    @Test(expected=NumberFormatException.class)
    public void FaileDecTest1() {
        LiteralTools.parseInt("-2147483649");
    }

    @Test(expected=NumberFormatException.class)
    public void FailDecTest2() {
        LiteralTools.parseInt("-4294967295");
    }

    @Test(expected=NumberFormatException.class)
    public void FailDecTest3() {
        LiteralTools.parseInt("4294967296");
    }

    @Test(expected=NumberFormatException.class)
    public void FailDecTest4() {
        LiteralTools.parseInt("4294967300");
    }

    @Test(expected=NumberFormatException.class)
    public void FailDecTest5() {
        LiteralTools.parseInt("8589934592");
    }


    @Test
    public void SuccessOctTests() {
        Assert.assertTrue(LiteralTools.parseInt("00") == 00);
        Assert.assertTrue(LiteralTools.parseInt("01") == 01);
        Assert.assertTrue(LiteralTools.parseInt("012345670123") == 012345670123);
        Assert.assertTrue(LiteralTools.parseInt("017777777777") == Integer.MAX_VALUE);
        Assert.assertTrue(LiteralTools.parseInt("020000000000") == Integer.MIN_VALUE);
        Assert.assertTrue(LiteralTools.parseInt("037777777777") == -1);


        Assert.assertTrue(LiteralTools.parseInt("-00") == 0);
        Assert.assertTrue(LiteralTools.parseInt("-01") == -1);
        Assert.assertTrue(LiteralTools.parseInt("-012345670123") == -012345670123);
        Assert.assertTrue(LiteralTools.parseInt("-017777777777") == -017777777777);
        Assert.assertTrue(LiteralTools.parseInt("-020000000000") == Integer.MIN_VALUE);
    }

    @Test(expected=NumberFormatException.class)
    public void FaileOctTest1() {
        LiteralTools.parseInt("-020000000001");
    }

    @Test(expected=NumberFormatException.class)
    public void FailOctTest2() {
        LiteralTools.parseInt("-037777777777");
    }

    @Test(expected=NumberFormatException.class)
    public void FailOctTest3() {
        LiteralTools.parseInt("040000000000");
    }
}
