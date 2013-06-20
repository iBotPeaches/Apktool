/*
 * Copyright 2012, Google Inc.
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
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

public class AbstractForwardSequentialListTest {
    private List<Integer> list;

    @Before
    public void setup() {
        list = new AbstractForwardSequentialList<Integer>() {
            @Nonnull @Override public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    private int index = 0;

                    @Override public boolean hasNext() {
                        return index < 100;
                    }

                    @Override public Integer next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException();
                        }
                        return index++;
                    }

                    @Override public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }

            @Override public int size() {
                return 100;
            }
        };
    }

    private void testForwardIterationImpl(ListIterator<Integer> iter) {
        Assert.assertFalse(iter.hasPrevious());

        for (int i=0; i<100; i++) {
            Assert.assertEquals(i, iter.nextIndex());
            Assert.assertEquals(i-1, iter.previousIndex());

            Assert.assertTrue(iter.hasNext());

            Assert.assertEquals(i, iter.next().intValue());
            Assert.assertTrue(iter.hasPrevious());
        }

        Assert.assertFalse(iter.hasNext());
        Assert.assertEquals(iter.nextIndex(), 100);
        Assert.assertEquals(iter.previousIndex(), 99);
    }

    @Test
    public void testForwardIteration() {
        testForwardIterationImpl(list.listIterator());
    }

    private void testReverseIterationImpl(ListIterator<Integer> iter) {
        Assert.assertFalse(iter.hasNext());

        for (int i=99; i>=0; i--) {
            Assert.assertEquals(i+1, iter.nextIndex());
            Assert.assertEquals(i, iter.previousIndex());

            Assert.assertTrue(iter.hasPrevious());

            Assert.assertEquals(i, iter.previous().intValue());
            Assert.assertTrue(iter.hasNext());
        }

        Assert.assertFalse(iter.hasPrevious());
        Assert.assertEquals(0, iter.nextIndex());
        Assert.assertEquals(-1, iter.previousIndex());
    }

    @Test
    public void testReverseIteration() {
        testReverseIterationImpl(list.listIterator(100));
    }

    @Test
    public void testAlternatingIteration() {
        ListIterator<Integer> iter = list.listIterator(50);

        for (int i=0; i<10; i++) {
            Assert.assertTrue(iter.hasNext());
            Assert.assertTrue(iter.hasPrevious());
            Assert.assertEquals(50, iter.nextIndex());
            Assert.assertEquals(49, iter.previousIndex());

            Assert.assertEquals(50, iter.next().intValue());

            Assert.assertTrue(iter.hasNext());
            Assert.assertTrue(iter.hasPrevious());
            Assert.assertEquals(51, iter.nextIndex());
            Assert.assertEquals(50, iter.previousIndex());

            Assert.assertEquals(50, iter.previous().intValue());
        }
    }

    @Test
    public void testAlternatingIteration2() {
        ListIterator<Integer> iter = list.listIterator(0);

        for (int i=0; i<10; i++) {
            testForwardIterationImpl(iter);
            testReverseIterationImpl(iter);
        }
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testNegativeIndex() {
        list.listIterator(-1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testLargeIndex() {
        list.listIterator(101);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testLargeIndex2() {
        list.listIterator(1000000);
    }

    @Test
    public void testForwardIterationException() {
        // note: no "expected = NoSuchElementException", because we want to make sure the exception occurs only during
        // the last call to next()

        ListIterator<Integer> iter = list.listIterator(0);
        for (int i=0; i<100; i++) {
            iter.next();
        }
        try {
            iter.next();
        } catch (NoSuchElementException ex) {
            return;
        }
        Assert.fail();
    }

    @Test(expected = NoSuchElementException.class)
    public void testForwardIterationException2() {
        ListIterator<Integer> iter = list.listIterator(100);
        iter.next();
    }

    @Test
    public void testReverseIterationException() {
        // note: no "expected = NoSuchElementException", because we want to make sure the exception occurs only during
        // the last call to previous()

        ListIterator<Integer> iter = list.listIterator(100);
        for (int i=0; i<100; i++) {
            iter.previous();
        }
        try {
            iter.previous();
        } catch (NoSuchElementException ex) {
            return;
        }
        Assert.fail();
    }

    @Test(expected = NoSuchElementException.class)
    public void testReverseIterationException2() {
        ListIterator<Integer> iter = list.listIterator(0);
        iter.previous();
    }
}
