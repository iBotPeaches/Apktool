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

import java.util.Comparator;
import java.util.List;

public class LinearSearch {
    /**
     * Performs a linear search in a sorted list for key, starting at initialGuess
     *
     * @param list The sorted list to search
     * @param comparator The comparator to use
     * @param key The key to search for
     * @param initialGuess An initial guess of the location.
     * @return If found, the index of the item. If not found, -return + 1 is the index at which the item would be
     *         inserted
     */
    public static <T> int linearSearch(List<? extends T> list, Comparator<T> comparator, T key, int initialGuess) {
        int guess = initialGuess;
        if (guess >= list.size()) {
            guess = list.size()-1;
        }
        int comparison = comparator.compare(list.get(guess), key);
        if (comparison == 0) {
            return guess;
        }
        if (comparison < 0) {
            guess++;
            while (guess < list.size()) {
                comparison = comparator.compare(list.get(guess), key);
                if (comparison == 0) {
                    return guess;
                }
                if (comparison > 0) {
                    return -(guess+1);
                }
                guess++;
            }
            return -(list.size()+1);
        } else {
            guess--;
            while (guess >= 0) {
                comparison = comparator.compare(list.get(guess), key);
                if (comparison == 0) {
                    return guess;
                }
                if (comparison < 0) {
                    return -(guess+2);
                }
                guess--;
            }
            return -1;
        }
    }
}
