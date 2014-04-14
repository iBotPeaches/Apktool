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

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import junit.framework.Assert;
import org.junit.Test;

import java.util.List;

public class LinearSearchTest {
    @Test
    public void testLinearSearch() {
        List<Integer> list = Lists.newArrayList(0, 1, 3, 4);

        doTest(list, 5, 10);
        doTest(list, 5, 4);
        doTest(list, 5, 3);
        doTest(list, 5, 2);
        doTest(list, 5, 1);
        doTest(list, 5, 0);

        doTest(list, 4, 10);
        doTest(list, 4, 4);
        doTest(list, 4, 3);
        doTest(list, 4, 2);
        doTest(list, 4, 1);
        doTest(list, 4, 0);

        doTest(list, 3, 10);
        doTest(list, 3, 4);
        doTest(list, 3, 3);
        doTest(list, 3, 2);
        doTest(list, 3, 1);
        doTest(list, 3, 0);

        doTest(list, 2, 10);
        doTest(list, 2, 4);
        doTest(list, 2, 3);
        doTest(list, 2, 2);
        doTest(list, 2, 1);
        doTest(list, 2, 0);

        doTest(list, 1, 10);
        doTest(list, 1, 4);
        doTest(list, 1, 3);
        doTest(list, 1, 2);
        doTest(list, 1, 1);
        doTest(list, 1, 0);

        doTest(list, 0, 10);
        doTest(list, 0, 4);
        doTest(list, 0, 3);
        doTest(list, 0, 2);
        doTest(list, 0, 1);
        doTest(list, 0, 0);

        doTest(list, -1, 10);
        doTest(list, -1, 4);
        doTest(list, -1, 3);
        doTest(list, -1, 2);
        doTest(list, -1, 1);
        doTest(list, -1, 0);
    }

    private void doTest(List<Integer> list, int key, int guess) {
        int expectedIndex =  Ordering.natural().binarySearch(list, key);

        Assert.assertEquals(expectedIndex, LinearSearch.linearSearch(list, Ordering.<Integer>natural(), key, guess));
    }
}
