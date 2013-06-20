/*
 * Copyright 2013, Google Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.util;

import junit.framework.Assert;
import org.junit.Test;

public class NumberUtilsTest {
    @Test
    public void isLikelyFloatTest() {
        Assert.assertTrue(NumberUtils.isLikelyFloat(Float.floatToRawIntBits(1.23f)));
        Assert.assertTrue(NumberUtils.isLikelyFloat(Float.floatToRawIntBits(1.0f)));
        Assert.assertTrue(NumberUtils.isLikelyFloat(Float.floatToRawIntBits(Float.NaN)));
        Assert.assertTrue(NumberUtils.isLikelyFloat(Float.floatToRawIntBits(Float.NEGATIVE_INFINITY)));
        Assert.assertTrue(NumberUtils.isLikelyFloat(Float.floatToRawIntBits(Float.POSITIVE_INFINITY)));
        Assert.assertTrue(NumberUtils.isLikelyFloat(Float.floatToRawIntBits(1e-30f)));
        Assert.assertTrue(NumberUtils.isLikelyFloat(Float.floatToRawIntBits(1000f)));
        Assert.assertTrue(NumberUtils.isLikelyFloat(Float.floatToRawIntBits(1f)));
        Assert.assertTrue(NumberUtils.isLikelyFloat(Float.floatToRawIntBits(-1f)));
        Assert.assertTrue(NumberUtils.isLikelyFloat(Float.floatToRawIntBits(-5f)));
        Assert.assertTrue(NumberUtils.isLikelyFloat(Float.floatToRawIntBits(1.3333f)));
        Assert.assertTrue(NumberUtils.isLikelyFloat(Float.floatToRawIntBits(4.5f)));
        Assert.assertTrue(NumberUtils.isLikelyFloat(Float.floatToRawIntBits(.1f)));
        Assert.assertTrue(NumberUtils.isLikelyFloat(Float.floatToRawIntBits(50000f)));
        Assert.assertTrue(NumberUtils.isLikelyFloat(Float.floatToRawIntBits(Float.MAX_VALUE)));
        Assert.assertTrue(NumberUtils.isLikelyFloat(Float.floatToRawIntBits((float)Math.PI)));
        Assert.assertTrue(NumberUtils.isLikelyFloat(Float.floatToRawIntBits((float)Math.E)));

        Assert.assertTrue(NumberUtils.isLikelyFloat(2139095039));


        // Float.MIN_VALUE is equivalent to integer value 1 - this should be detected as an integer
        //Assert.assertTrue(NumberUtils.isLikelyFloat(Float.floatToRawIntBits(Float.MIN_VALUE)));

        // This one doesn't quite work. It has a series of 2 0's, but that is probably not enough to strip off normally
        //Assert.assertTrue(NumberUtils.isLikelyFloat(Float.floatToRawIntBits(1.33333f)));

        Assert.assertFalse(NumberUtils.isLikelyFloat(0));
        Assert.assertFalse(NumberUtils.isLikelyFloat(1));
        Assert.assertFalse(NumberUtils.isLikelyFloat(10));
        Assert.assertFalse(NumberUtils.isLikelyFloat(100));
        Assert.assertFalse(NumberUtils.isLikelyFloat(1000));
        Assert.assertFalse(NumberUtils.isLikelyFloat(1024));
        Assert.assertFalse(NumberUtils.isLikelyFloat(1234));
        Assert.assertFalse(NumberUtils.isLikelyFloat(-5));
        Assert.assertFalse(NumberUtils.isLikelyFloat(-13));
        Assert.assertFalse(NumberUtils.isLikelyFloat(-123));
        Assert.assertFalse(NumberUtils.isLikelyFloat(20000000));
        Assert.assertFalse(NumberUtils.isLikelyFloat(2000000000));
        Assert.assertFalse(NumberUtils.isLikelyFloat(-2000000000));
        Assert.assertFalse(NumberUtils.isLikelyFloat(Integer.MAX_VALUE));
        Assert.assertFalse(NumberUtils.isLikelyFloat(Integer.MIN_VALUE));
        Assert.assertFalse(NumberUtils.isLikelyFloat(Short.MIN_VALUE));
        Assert.assertFalse(NumberUtils.isLikelyFloat(Short.MAX_VALUE));
    }

    @Test
    public void isLikelyDoubleTest() {
        Assert.assertTrue(NumberUtils.isLikelyDouble(Double.doubleToRawLongBits(1.23f)));
        Assert.assertTrue(NumberUtils.isLikelyDouble(Double.doubleToRawLongBits(1.0f)));
        Assert.assertTrue(NumberUtils.isLikelyDouble(Double.doubleToRawLongBits(Double.NaN)));
        Assert.assertTrue(NumberUtils.isLikelyDouble(Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY)));
        Assert.assertTrue(NumberUtils.isLikelyDouble(Double.doubleToRawLongBits(Double.POSITIVE_INFINITY)));
        Assert.assertTrue(NumberUtils.isLikelyDouble(Double.doubleToRawLongBits(1e-30f)));
        Assert.assertTrue(NumberUtils.isLikelyDouble(Double.doubleToRawLongBits(1000f)));
        Assert.assertTrue(NumberUtils.isLikelyDouble(Double.doubleToRawLongBits(1f)));
        Assert.assertTrue(NumberUtils.isLikelyDouble(Double.doubleToRawLongBits(-1f)));
        Assert.assertTrue(NumberUtils.isLikelyDouble(Double.doubleToRawLongBits(-5f)));
        Assert.assertTrue(NumberUtils.isLikelyDouble(Double.doubleToRawLongBits(1.3333f)));
        Assert.assertTrue(NumberUtils.isLikelyDouble(Double.doubleToRawLongBits(1.33333f)));
        Assert.assertTrue(NumberUtils.isLikelyDouble(Double.doubleToRawLongBits(4.5f)));
        Assert.assertTrue(NumberUtils.isLikelyDouble(Double.doubleToRawLongBits(.1f)));
        Assert.assertTrue(NumberUtils.isLikelyDouble(Double.doubleToRawLongBits(50000f)));
        Assert.assertTrue(NumberUtils.isLikelyDouble(Double.doubleToRawLongBits(Double.MAX_VALUE)));
        Assert.assertTrue(NumberUtils.isLikelyDouble(Double.doubleToRawLongBits(Math.PI)));
        Assert.assertTrue(NumberUtils.isLikelyDouble(Double.doubleToRawLongBits(Math.E)));

        // Double.MIN_VALUE is equivalent to integer value 1 - this should be detected as an integer
        //Assert.assertTrue(NumberUtils.isLikelyDouble(Double.doubleToRawLongBits(Double.MIN_VALUE)));

        Assert.assertFalse(NumberUtils.isLikelyDouble(0));
        Assert.assertFalse(NumberUtils.isLikelyDouble(1));
        Assert.assertFalse(NumberUtils.isLikelyDouble(10));
        Assert.assertFalse(NumberUtils.isLikelyDouble(100));
        Assert.assertFalse(NumberUtils.isLikelyDouble(1000));
        Assert.assertFalse(NumberUtils.isLikelyDouble(1024));
        Assert.assertFalse(NumberUtils.isLikelyDouble(1234));
        Assert.assertFalse(NumberUtils.isLikelyDouble(-5));
        Assert.assertFalse(NumberUtils.isLikelyDouble(-13));
        Assert.assertFalse(NumberUtils.isLikelyDouble(-123));
        Assert.assertFalse(NumberUtils.isLikelyDouble(20000000));
        Assert.assertFalse(NumberUtils.isLikelyDouble(2000000000));
        Assert.assertFalse(NumberUtils.isLikelyDouble(-2000000000));
        Assert.assertFalse(NumberUtils.isLikelyDouble(Integer.MAX_VALUE));
        Assert.assertFalse(NumberUtils.isLikelyDouble(Integer.MIN_VALUE));
        Assert.assertFalse(NumberUtils.isLikelyDouble(Short.MIN_VALUE));
        Assert.assertFalse(NumberUtils.isLikelyDouble(Short.MAX_VALUE));
    }
}
