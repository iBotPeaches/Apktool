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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.AbstractSequentialList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;

public abstract class AbstractForwardSequentialList<T> extends AbstractSequentialList<T> {

    @Nonnull private Iterator<T> iterator(int index) {
        if (index < 0) {
            throw new NoSuchElementException();
        }

        Iterator<T> it = iterator();
        for (int i=0; i<index; i++) {
            it.next();
        }
        return it;
    }

    @Override @Nonnull public abstract Iterator<T> iterator();

    @Override @Nonnull public ListIterator<T> listIterator(final int initialIndex) {

        final Iterator<T> initialIterator;
        try {
            initialIterator = iterator(initialIndex);
        } catch (NoSuchElementException ex) {
            throw new IndexOutOfBoundsException();
        }

        return new AbstractListIterator<T>() {
            private int index = initialIndex - 1;
            @Nullable private Iterator<T> forwardIterator = initialIterator;

            @Nonnull
            private Iterator<T> getForwardIterator() {
                if (forwardIterator == null) {
                    try {
                        forwardIterator = iterator(index+1);
                    } catch (IndexOutOfBoundsException ex) {
                        throw new NoSuchElementException();
                    }
                }
                return forwardIterator;
            }

            @Override public boolean hasNext() {
                return getForwardIterator().hasNext();
            }

            @Override public boolean hasPrevious() {
                return index >= 0;
            }

            @Override public T next() {
                T ret = getForwardIterator().next();
                index++;
                return ret;
            }

            @Override public int nextIndex() {
                return index+1;
            }

            @Override public T previous() {
                forwardIterator = null;
                try {
                    return iterator(index--).next();
                } catch (IndexOutOfBoundsException ex) {
                    throw new NoSuchElementException();
                }
            }

            @Override public int previousIndex() {
                return index;
            }
        };
    }

    @Override @Nonnull public ListIterator<T> listIterator() {
        return listIterator(0);
    }
}
