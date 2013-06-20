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

package org.jf.dexlib2.writer.util;

import com.google.common.collect.ImmutableList;
import junit.framework.Assert;
import org.jf.dexlib2.iface.ExceptionHandler;
import org.jf.dexlib2.iface.TryBlock;
import org.jf.dexlib2.immutable.ImmutableExceptionHandler;
import org.jf.dexlib2.immutable.ImmutableTryBlock;
import org.junit.Test;

import java.util.List;

public class TryListBuilderTest {
    private static class TryListBuilder extends org.jf.dexlib2.writer.util.TryListBuilder<ExceptionHandler> {
    }

    @Test
    public void testSingleCatchAll_Beginning() {
        TryListBuilder tlb = new TryListBuilder();

        tlb.addHandler(0, 10, new ImmutableExceptionHandler(null, 5));

        List<? extends TryBlock<? extends ExceptionHandler>> tryBlocks = tlb.getTryBlocks();

        List<? extends TryBlock> expected = ImmutableList.of(new ImmutableTryBlock(0, 10,
                ImmutableList.of(new ImmutableExceptionHandler(null, 5))));

        Assert.assertEquals(expected, tryBlocks);
    }

    @Test
    public void testSingleCatchAll_Middle() {
        TryListBuilder tlb = new TryListBuilder();

        tlb.addHandler(5, 10, new ImmutableExceptionHandler(null, 15));

        List<? extends TryBlock<? extends ExceptionHandler>> tryBlocks = tlb.getTryBlocks();

        List<? extends TryBlock> expected = ImmutableList.of(new ImmutableTryBlock(5, 5,
                ImmutableList.of(new ImmutableExceptionHandler(null, 15))));

        Assert.assertEquals(expected, tryBlocks);
    }

    @Test
    public void testSingleCatch_Beginning() {
        TryListBuilder tlb = new TryListBuilder();

        tlb.addHandler(0, 10, new ImmutableExceptionHandler("Ljava/lang/Exception;", 5));

        List<? extends TryBlock<? extends ExceptionHandler>> tryBlocks = tlb.getTryBlocks();

        List<? extends TryBlock> expected = ImmutableList.of(new ImmutableTryBlock(0, 10,
                ImmutableList.of(new ImmutableExceptionHandler("Ljava/lang/Exception;", 5))));

        Assert.assertEquals(expected, tryBlocks);
    }

    @Test
    public void testSingleCatch_Middle() {
        TryListBuilder tlb = new TryListBuilder();

        tlb.addHandler(5, 10, new ImmutableExceptionHandler("Ljava/lang/Exception;", 15));

        List<? extends TryBlock<? extends ExceptionHandler>> tryBlocks = tlb.getTryBlocks();

        List<? extends TryBlock> expected = ImmutableList.of(new ImmutableTryBlock(5, 5,
                ImmutableList.of(new ImmutableExceptionHandler("Ljava/lang/Exception;", 15))));

        Assert.assertEquals(expected, tryBlocks);
    }

    @Test
    public void testOverlap_End_After() {
        TryListBuilder tlb = new TryListBuilder();

        tlb.addHandler(0, 10, new ImmutableExceptionHandler("LException1;", 5));
        tlb.addHandler(10, 20, new ImmutableExceptionHandler("LException2;", 6));

        List<? extends TryBlock<? extends ExceptionHandler>> tryBlocks = tlb.getTryBlocks();

        List<? extends TryBlock> expected = ImmutableList.of(
                new ImmutableTryBlock(0, 10,
                        ImmutableList.of(new ImmutableExceptionHandler("LException1;", 5))),
                new ImmutableTryBlock(10, 10,
                        ImmutableList.of(new ImmutableExceptionHandler("LException2;", 6))));

        Assert.assertEquals(expected, tryBlocks);
    }

    @Test
    public void testOverlap_After_After() {
        TryListBuilder tlb = new TryListBuilder();

        tlb.addHandler(0, 10, new ImmutableExceptionHandler("LException1;", 5));
        tlb.addHandler(15, 20, new ImmutableExceptionHandler("LException2;", 6));

        List<? extends TryBlock<? extends ExceptionHandler>> tryBlocks = tlb.getTryBlocks();

        List<? extends TryBlock> expected = ImmutableList.of(
                new ImmutableTryBlock(0, 10,
                        ImmutableList.of(new ImmutableExceptionHandler("LException1;", 5))),
                new ImmutableTryBlock(15, 5,
                        ImmutableList.of(new ImmutableExceptionHandler("LException2;", 6))));

        Assert.assertEquals(expected, tryBlocks);
    }

    @Test
    public void testOverlap_Before_Start() {
        TryListBuilder tlb = new TryListBuilder();

        tlb.addHandler(5, 10, new ImmutableExceptionHandler("LException1;", 5));
        tlb.addHandler(0, 5, new ImmutableExceptionHandler("LException2;", 6));

        List<? extends TryBlock<? extends ExceptionHandler>> tryBlocks = tlb.getTryBlocks();

        List<? extends TryBlock> expected = ImmutableList.of(
                new ImmutableTryBlock(0, 5,
                        ImmutableList.of(new ImmutableExceptionHandler("LException2;", 6))),
                new ImmutableTryBlock(5, 5,
                        ImmutableList.of(new ImmutableExceptionHandler("LException1;", 5))));

        Assert.assertEquals(expected, tryBlocks);
    }

    @Test
    public void testOverlap_Before_Before() {
        TryListBuilder tlb = new TryListBuilder();

        tlb.addHandler(5, 10, new ImmutableExceptionHandler("LException1;", 5));
        tlb.addHandler(0, 3, new ImmutableExceptionHandler("LException2;", 6));

        List<? extends TryBlock<? extends ExceptionHandler>> tryBlocks = tlb.getTryBlocks();

        List<? extends TryBlock> expected = ImmutableList.of(
                new ImmutableTryBlock(0, 3,
                        ImmutableList.of(new ImmutableExceptionHandler("LException2;", 6))),
                new ImmutableTryBlock(5, 5,
                        ImmutableList.of(new ImmutableExceptionHandler("LException1;", 5))));

        Assert.assertEquals(expected, tryBlocks);
    }

    @Test
    public void testOverlap_Start_End() {
        TryListBuilder tlb = new TryListBuilder();

        tlb.addHandler(0, 10, new ImmutableExceptionHandler("LException1;", 5));
        tlb.addHandler(0, 10, new ImmutableExceptionHandler("LException2;", 6));

        List<? extends TryBlock<? extends ExceptionHandler>> tryBlocks = tlb.getTryBlocks();

        List<? extends TryBlock> expected = ImmutableList.of(
                new ImmutableTryBlock(0, 10,
                        ImmutableList.of(
                                new ImmutableExceptionHandler("LException1;", 5),
                                new ImmutableExceptionHandler("LException2;", 6))));

        Assert.assertEquals(expected, tryBlocks);
    }

    @Test
    public void testOverlap_Start_Middle() {
        TryListBuilder tlb = new TryListBuilder();

        tlb.addHandler(0, 10, new ImmutableExceptionHandler("LException1;", 5));
        tlb.addHandler(0, 5, new ImmutableExceptionHandler("LException2;", 6));

        List<? extends TryBlock<? extends ExceptionHandler>> tryBlocks = tlb.getTryBlocks();

        List<? extends TryBlock> expected = ImmutableList.of(
                new ImmutableTryBlock(0, 5,
                        ImmutableList.of(
                                new ImmutableExceptionHandler("LException1;", 5),
                                new ImmutableExceptionHandler("LException2;", 6))),
                new ImmutableTryBlock(5, 5,
                        ImmutableList.of(
                                new ImmutableExceptionHandler("LException1;", 5))));

        Assert.assertEquals(expected, tryBlocks);
    }

    @Test
    public void testOverlap_Middle_Middle() {
        TryListBuilder tlb = new TryListBuilder();

        tlb.addHandler(0, 10, new ImmutableExceptionHandler("LException1;", 5));
        tlb.addHandler(2, 7, new ImmutableExceptionHandler("LException2;", 6));

        List<? extends TryBlock<? extends ExceptionHandler>> tryBlocks = tlb.getTryBlocks();

        List<? extends TryBlock> expected = ImmutableList.of(
                new ImmutableTryBlock(0, 2,
                        ImmutableList.of(
                                new ImmutableExceptionHandler("LException1;", 5))),
                new ImmutableTryBlock(2, 5,
                        ImmutableList.of(
                                new ImmutableExceptionHandler("LException1;", 5),
                                new ImmutableExceptionHandler("LException2;", 6))),
                new ImmutableTryBlock(7, 3,
                        ImmutableList.of(
                                new ImmutableExceptionHandler("LException1;", 5))));

        Assert.assertEquals(expected, tryBlocks);
    }

    @Test
    public void testOverlap_Middle_End() {
        TryListBuilder tlb = new TryListBuilder();

        tlb.addHandler(0, 10, new ImmutableExceptionHandler("LException1;", 5));
        tlb.addHandler(5, 10, new ImmutableExceptionHandler("LException2;", 6));

        List<? extends TryBlock<? extends ExceptionHandler>> tryBlocks = tlb.getTryBlocks();

        List<? extends TryBlock> expected = ImmutableList.of(
                new ImmutableTryBlock(0, 5,
                        ImmutableList.of(
                                new ImmutableExceptionHandler("LException1;", 5))),
                new ImmutableTryBlock(5, 5,
                        ImmutableList.of(
                                new ImmutableExceptionHandler("LException1;", 5),
                                new ImmutableExceptionHandler("LException2;", 6))));

        Assert.assertEquals(expected, tryBlocks);
    }

    @Test
    public void testOverlap_Beginning_After() {
        TryListBuilder tlb = new TryListBuilder();

        tlb.addHandler(0, 10, new ImmutableExceptionHandler("LException1;", 5));
        tlb.addHandler(0, 15, new ImmutableExceptionHandler("LException2;", 6));

        List<? extends TryBlock<? extends ExceptionHandler>> tryBlocks = tlb.getTryBlocks();

        List<? extends TryBlock> expected = ImmutableList.of(
                new ImmutableTryBlock(0, 10,
                        ImmutableList.of(
                                new ImmutableExceptionHandler("LException1;", 5),
                                new ImmutableExceptionHandler("LException2;", 6))),
                new ImmutableTryBlock(10, 5,
                        ImmutableList.of(
                                new ImmutableExceptionHandler("LException2;", 6))));

        Assert.assertEquals(expected, tryBlocks);
    }

    @Test
    public void testOverlap_Middle_After() {
        TryListBuilder tlb = new TryListBuilder();

        tlb.addHandler(0, 10, new ImmutableExceptionHandler("LException1;", 5));
        tlb.addHandler(5, 15, new ImmutableExceptionHandler("LException2;", 6));

        List<? extends TryBlock<? extends ExceptionHandler>> tryBlocks = tlb.getTryBlocks();

        List<? extends TryBlock> expected = ImmutableList.of(
                new ImmutableTryBlock(0, 5,
                        ImmutableList.of(
                                new ImmutableExceptionHandler("LException1;", 5))),
                new ImmutableTryBlock(5, 5,
                        ImmutableList.of(
                                new ImmutableExceptionHandler("LException1;", 5),
                                new ImmutableExceptionHandler("LException2;", 6))),
                new ImmutableTryBlock(10, 5,
                        ImmutableList.of(
                                new ImmutableExceptionHandler("LException2;", 6))));

        Assert.assertEquals(expected, tryBlocks);
    }

    @Test
    public void testOverlap_Before_End() {
        TryListBuilder tlb = new TryListBuilder();

        tlb.addHandler(5, 10, new ImmutableExceptionHandler("LException1;", 5));
        tlb.addHandler(0, 10, new ImmutableExceptionHandler("LException2;", 6));

        List<? extends TryBlock<? extends ExceptionHandler>> tryBlocks = tlb.getTryBlocks();

        List<? extends TryBlock> expected = ImmutableList.of(
                new ImmutableTryBlock(0, 5,
                        ImmutableList.of(
                                new ImmutableExceptionHandler("LException2;", 6))),
                new ImmutableTryBlock(5, 5,
                        ImmutableList.of(
                                new ImmutableExceptionHandler("LException1;", 5),
                                new ImmutableExceptionHandler("LException2;", 6))));

        Assert.assertEquals(expected, tryBlocks);
    }

    @Test
    public void testOverlap_Before_Middle() {
        TryListBuilder tlb = new TryListBuilder();

        tlb.addHandler(5, 10, new ImmutableExceptionHandler("LException1;", 5));
        tlb.addHandler(0, 7, new ImmutableExceptionHandler("LException2;", 6));

        List<? extends TryBlock<? extends ExceptionHandler>> tryBlocks = tlb.getTryBlocks();

        List<? extends TryBlock> expected = ImmutableList.of(
                new ImmutableTryBlock(0, 5,
                        ImmutableList.of(
                                new ImmutableExceptionHandler("LException2;", 6))),
                new ImmutableTryBlock(5, 2,
                        ImmutableList.of(
                                new ImmutableExceptionHandler("LException1;", 5),
                                new ImmutableExceptionHandler("LException2;", 6))),
                new ImmutableTryBlock(7, 3,
                        ImmutableList.of(
                                new ImmutableExceptionHandler("LException1;", 5))));

        Assert.assertEquals(expected, tryBlocks);
    }

    @Test
    public void testOverlap_Before_After() {
        TryListBuilder tlb = new TryListBuilder();

        tlb.addHandler(5, 10, new ImmutableExceptionHandler("LException1;", 5));
        tlb.addHandler(0, 15, new ImmutableExceptionHandler("LException2;", 6));

        List<? extends TryBlock<? extends ExceptionHandler>> tryBlocks = tlb.getTryBlocks();

        List<? extends TryBlock> expected = ImmutableList.of(
                new ImmutableTryBlock(0, 5,
                        ImmutableList.of(
                                new ImmutableExceptionHandler("LException2;", 6))),
                new ImmutableTryBlock(5, 5,
                        ImmutableList.of(
                                new ImmutableExceptionHandler("LException1;", 5),
                                new ImmutableExceptionHandler("LException2;", 6))),
                new ImmutableTryBlock(10, 5,
                        ImmutableList.of(
                                new ImmutableExceptionHandler("LException2;", 6))));

        Assert.assertEquals(expected, tryBlocks);
    }

    @Test
    public void testOverlap_Hole() {
        TryListBuilder tlb = new TryListBuilder();

        tlb.addHandler(1, 5, new ImmutableExceptionHandler("LException1;", 5));
        tlb.addHandler(10, 14, new ImmutableExceptionHandler("LException1;", 5));
        tlb.addHandler(0, 15, new ImmutableExceptionHandler("LException2;", 6));

        List<? extends TryBlock<? extends ExceptionHandler>> tryBlocks = tlb.getTryBlocks();

        List<? extends TryBlock> expected = ImmutableList.of(
                new ImmutableTryBlock(0, 1,
                        ImmutableList.of(
                                new ImmutableExceptionHandler("LException2;", 6))),
                new ImmutableTryBlock(1, 4,
                        ImmutableList.of(
                                new ImmutableExceptionHandler("LException1;", 5),
                                new ImmutableExceptionHandler("LException2;", 6))),
                new ImmutableTryBlock(5, 5,
                        ImmutableList.of(
                                new ImmutableExceptionHandler("LException2;", 6))),
                new ImmutableTryBlock(10, 4,
                        ImmutableList.of(
                                new ImmutableExceptionHandler("LException1;", 5),
                                new ImmutableExceptionHandler("LException2;", 6))),
                new ImmutableTryBlock(14, 1,
                        ImmutableList.of(
                                new ImmutableExceptionHandler("LException2;", 6))));

        Assert.assertEquals(expected, tryBlocks);
    }

    @Test
    public void testHandlerMerge_Same() {
        TryListBuilder tlb = new TryListBuilder();

        tlb.addHandler(5, 10, new ImmutableExceptionHandler("LException1;", 5));
        tlb.addHandler(0, 15, new ImmutableExceptionHandler("LException1;", 5));

        List<? extends TryBlock<? extends ExceptionHandler>> tryBlocks = tlb.getTryBlocks();

        List<? extends TryBlock> expected = ImmutableList.of(
                new ImmutableTryBlock(0, 15,
                        ImmutableList.of(
                                new ImmutableExceptionHandler("LException1;", 5))));

        Assert.assertEquals(expected, tryBlocks);
    }

    @Test
    public void testHandlerMerge_DifferentType() {
        TryListBuilder tlb = new TryListBuilder();

        tlb.addHandler(5, 10, new ImmutableExceptionHandler("LException1;", 5));
        tlb.addHandler(0, 15, new ImmutableExceptionHandler("LException2;", 6));

        List<? extends TryBlock<? extends ExceptionHandler>> tryBlocks = tlb.getTryBlocks();

        List<? extends TryBlock> expected = ImmutableList.of(
                new ImmutableTryBlock(0, 5,
                        ImmutableList.of(
                                new ImmutableExceptionHandler("LException2;", 6))),
                new ImmutableTryBlock(5, 5,
                        ImmutableList.of(
                                new ImmutableExceptionHandler("LException1;", 5),
                                new ImmutableExceptionHandler("LException2;", 6))),
                new ImmutableTryBlock(10, 5,
                        ImmutableList.of(
                                new ImmutableExceptionHandler("LException2;", 6))));

        Assert.assertEquals(expected, tryBlocks);
    }

    @Test
    public void testHandlerMerge_DifferentAddress() {
        TryListBuilder tlb = new TryListBuilder();

        tlb.addHandler(5, 10, new ImmutableExceptionHandler("LException1;", 5));
        try {
            tlb.addHandler(0, 15, new ImmutableExceptionHandler("LException1;", 6));
        } catch (TryListBuilder.InvalidTryException ex) {
            return;
        }
        Assert.fail();
    }

    @Test
    public void testHandlerMerge_Exception_Catchall() {
        TryListBuilder tlb = new TryListBuilder();

        tlb.addHandler(5, 10, new ImmutableExceptionHandler("LException1;", 5));
        tlb.addHandler(0, 15, new ImmutableExceptionHandler(null, 6));

        List<? extends TryBlock<? extends ExceptionHandler>> tryBlocks = tlb.getTryBlocks();

        List<? extends TryBlock> expected = ImmutableList.of(
                new ImmutableTryBlock(0, 5,
                        ImmutableList.of(
                                new ImmutableExceptionHandler(null, 6))),
                new ImmutableTryBlock(5, 5,
                        ImmutableList.of(
                                new ImmutableExceptionHandler("LException1;", 5),
                                new ImmutableExceptionHandler(null, 6))),
                new ImmutableTryBlock(10, 5,
                        ImmutableList.of(
                                new ImmutableExceptionHandler(null, 6))));

        Assert.assertEquals(expected, tryBlocks);
    }

    @Test
    public void testHandlerMerge_Catchall_Exception() {
        TryListBuilder tlb = new TryListBuilder();

        tlb.addHandler(5, 10, new ImmutableExceptionHandler(null, 5));
        tlb.addHandler(0, 15, new ImmutableExceptionHandler("LException1;", 6));

        List<? extends TryBlock<? extends ExceptionHandler>> tryBlocks = tlb.getTryBlocks();

        List<? extends TryBlock> expected = ImmutableList.of(
                new ImmutableTryBlock(0, 5,
                        ImmutableList.of(
                                new ImmutableExceptionHandler("LException1;", 6))),
                new ImmutableTryBlock(5, 5,
                        ImmutableList.of(
                                new ImmutableExceptionHandler(null, 5),
                                new ImmutableExceptionHandler("LException1;", 6))),
                new ImmutableTryBlock(10, 5,
                        ImmutableList.of(
                                new ImmutableExceptionHandler("LException1;", 6))));

        Assert.assertEquals(expected, tryBlocks);
    }

    @Test
    public void testHandlerMerge_Catchall_Catchall() {
        TryListBuilder tlb = new TryListBuilder();

        tlb.addHandler(5, 10, new ImmutableExceptionHandler(null, 5));
        tlb.addHandler(0, 15, new ImmutableExceptionHandler(null, 5));

        List<? extends TryBlock<? extends ExceptionHandler>> tryBlocks = tlb.getTryBlocks();

        List<? extends TryBlock> expected = ImmutableList.of(
                new ImmutableTryBlock(0, 15,
                        ImmutableList.of(
                                new ImmutableExceptionHandler(null, 5))));

        Assert.assertEquals(expected, tryBlocks);
    }

    @Test
    public void testHandlerMerge_Catchall_Catchall_DifferentAddress() {
        TryListBuilder tlb = new TryListBuilder();

        tlb.addHandler(5, 10, new ImmutableExceptionHandler(null, 5));
        try {
            tlb.addHandler(0, 15, new ImmutableExceptionHandler(null, 6));
        } catch (TryListBuilder.InvalidTryException ex) {
            return;
        }
        Assert.fail();
    }

    @Test
    public void testHandlerMerge_MergeSame() {
        TryListBuilder tlb = new TryListBuilder();

        tlb.addHandler(0, 15, new ImmutableExceptionHandler(null, 6));
        tlb.addHandler(10, 20, new ImmutableExceptionHandler("LException1;", 5));
        tlb.addHandler(20, 30, new ImmutableExceptionHandler("LException1;", 5));
        tlb.addHandler(25, 40, new ImmutableExceptionHandler(null, 6));

        List<? extends TryBlock<? extends ExceptionHandler>> tryBlocks = tlb.getTryBlocks();

        List<? extends TryBlock> expected = ImmutableList.of(
                new ImmutableTryBlock(0, 10,
                        ImmutableList.of(
                                new ImmutableExceptionHandler(null, 6))),
                new ImmutableTryBlock(10, 5,
                        ImmutableList.of(
                                new ImmutableExceptionHandler(null, 6),
                                new ImmutableExceptionHandler("LException1;", 5))),
                new ImmutableTryBlock(15, 10,
                        ImmutableList.of(
                                new ImmutableExceptionHandler("LException1;", 5))),
                new ImmutableTryBlock(25, 5,
                        ImmutableList.of(
                                new ImmutableExceptionHandler("LException1;", 5),
                                new ImmutableExceptionHandler(null, 6))),
                new ImmutableTryBlock(30, 10,
                        ImmutableList.of(
                                new ImmutableExceptionHandler(null, 6))));

        Assert.assertEquals(expected, tryBlocks);
    }
}
