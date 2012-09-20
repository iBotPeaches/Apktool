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
 * INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.dexlib.Util;

import java.util.Arrays;
import java.util.Comparator;

public class ArrayUtils {
    /**
     * Utility method to sort two related arrays - that is, two arrays where the elements are related to each other
     * by their position in the array. i.e. firstArray[0] is related to secondArray[0], firstArray[1] is related to
     * secondArray[1], and so on. The first array is sorted based on its implementation of Comparable, and the 2nd
     * array is sorted by it's related item in the first array, so that the same elements are still related to each
     * other after the sort
     * @param firstArray The first array, which contains the values to sort
     * @param secondArray The second array, which will be sorted based on the values in the first array
     * @param <A> The type of element in the first array
     * @param <B> the type of element in the second array
     */
    public static <A extends Comparable<A>, B> void sortTwoArrays(A[] firstArray, B[] secondArray)
    {
        if (firstArray.length != secondArray.length) {
            throw new RuntimeException("Both arrays must be of the same length");
        }

        class element
        {
            public A first;
            public B second;
        }

        element[] elements = new element[firstArray.length];

        Arrays.sort(elements, new Comparator<element>(){
            public int compare(element a, element b) {
                return a.first.compareTo(b.first);
            }
        });

        for (int i=0; i<elements.length; i++) {
            firstArray[i] = elements[i].first;
            secondArray[i] = elements[i].second;
        }
    }
}
