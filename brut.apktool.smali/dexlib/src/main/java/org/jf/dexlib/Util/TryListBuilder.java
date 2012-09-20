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

import org.jf.dexlib.CodeItem;
import org.jf.dexlib.TypeIdItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class TryListBuilder
{
    /*TODO: add logic to merge adjacent, identical try blocks, and remove superflous handlers
      Also provide a "strict" mode, where the above isn't performed, which will be useful to be able to
      exactly reproduce the original .dex file (for testing/verification purposes)*/


    private TryRange firstTryRange = new TryRange(0,0);
    private TryRange lastTryRange = new TryRange(0,0);

    public TryListBuilder() {
        firstTryRange.next = lastTryRange;
        lastTryRange.previous = firstTryRange;
    }

    private class TryRange
    {
        public TryRange previous = null;
        public TryRange next = null;

        public int startAddress;
        public int endAddress;
        public LinkedList<Handler> handlers;

        public int catchAllHandlerAddress;

        public TryRange(int startAddress, int endAddress) {
            this.startAddress = startAddress;
            this.endAddress = endAddress;
            this.handlers = new LinkedList<Handler>();
            this.previous = null;
            this.next = null;
            catchAllHandlerAddress = -1;
        }

        public void append(TryRange tryRange) {
            /*we use a dummy last item, so this.next will always
            have a value*/
            this.next.previous = tryRange;
            tryRange.next = this.next;

            this.next = tryRange;
            tryRange.previous = this;
        }

        public void prepend(TryRange tryRange){
            /*we use a dummy first item, so this.previous will always
            have a value*/
            this.previous.next = tryRange;
            tryRange.previous = this.previous;

            this.previous = tryRange;
            tryRange.next = this;
        }

        /**
         * This splits the current range into two ranges at the given
         * address. The existing range will be shortened to the first
         * half, and a new range will be created and returned for the
         * 2nd half.
         * @param address The address to split at
         * @return The 2nd half of the
         */
        public TryRange split(int address) {
            //this is a private class, so address is assumed
            //to be valid

            TryRange tryRange = new TryRange(address, endAddress);
            tryRange.catchAllHandlerAddress = this.catchAllHandlerAddress;
            tryRange.handlers.addAll(this.handlers);
            append(tryRange);

            this.endAddress = address;

            return tryRange;
        }

        public void appendHandler(Handler handler) {
            handlers.addLast(handler);
        }

        public void prependHandler(Handler handler) {
            handlers.addFirst(handler);
        }
    }

    private class Handler
    {
        public final TypeIdItem type;
        public final int handlerAddress;

        public Handler(TypeIdItem type, int handlerAddress) {
            this.type = type;
            this.handlerAddress = handlerAddress;
        }
    }

    public Pair<List<CodeItem.TryItem>, List<CodeItem.EncodedCatchHandler>> encodeTries() {
        if (firstTryRange.next == lastTryRange) {
            return new Pair<List<CodeItem.TryItem>, List<CodeItem.EncodedCatchHandler>>(null, null);
        }

        ArrayList<CodeItem.TryItem> tries = new ArrayList<CodeItem.TryItem>();
        ArrayList<CodeItem.EncodedCatchHandler> handlers = new ArrayList<CodeItem.EncodedCatchHandler>();

        HashMap<CodeItem.EncodedCatchHandler, CodeItem.EncodedCatchHandler> handlerDict =
	                    new HashMap<CodeItem.EncodedCatchHandler, CodeItem.EncodedCatchHandler>();

        TryRange tryRange = firstTryRange.next;

        while (tryRange != lastTryRange) {
            CodeItem.EncodedTypeAddrPair[] encodedTypeAddrPairs =
                    new CodeItem.EncodedTypeAddrPair[tryRange.handlers.size()];

            int index = 0;
            for (Handler handler: tryRange.handlers) {
                CodeItem.EncodedTypeAddrPair encodedTypeAddrPair = new CodeItem.EncodedTypeAddrPair(
                        handler.type,
                        handler.handlerAddress);
                encodedTypeAddrPairs[index++] = encodedTypeAddrPair;
            }

            CodeItem.EncodedCatchHandler encodedCatchHandler = new CodeItem.EncodedCatchHandler(
                                                                    encodedTypeAddrPairs,
                                                                    tryRange.catchAllHandlerAddress);
            CodeItem.EncodedCatchHandler internedEncodedCatchHandler = handlerDict.get(encodedCatchHandler);
            if (internedEncodedCatchHandler == null) {
                handlerDict.put(encodedCatchHandler, encodedCatchHandler);
                handlers.add(encodedCatchHandler);
            } else {
                encodedCatchHandler = internedEncodedCatchHandler;
            }

            CodeItem.TryItem tryItem = new CodeItem.TryItem(
                    tryRange.startAddress,
                    tryRange.endAddress - tryRange.startAddress,
                    encodedCatchHandler);
            tries.add(tryItem);

            tryRange = tryRange.next;
        }

        return new Pair<List<CodeItem.TryItem>, List<CodeItem.EncodedCatchHandler>>(tries, handlers);
    }

    public void addCatchAllHandler(int startAddress, int endAddress, int handlerAddress) {
        TryRange startRange;
        TryRange endRange;

        Pair<TryRange, TryRange> ranges = getBoundingRanges(startAddress, endAddress);
        startRange = ranges.first;
        endRange = ranges.second;

        int previousEnd = startAddress;
        TryRange tryRange = startRange;

        /*Now we have the start and end ranges that exactly match the start and end
        of the range being added. We need to iterate over all the ranges from the start
        to end range inclusively, and append the handler to the end of each range's handler
        list. We also need to create a new range for any "holes" in the existing ranges*/
        do
        {
            //is there a hole? If so, add a new range to fill the hole
            if (tryRange.startAddress > previousEnd) {
                TryRange newRange = new TryRange(previousEnd, tryRange.startAddress);
                tryRange.prepend(newRange);
                tryRange = newRange;
            }

            if (tryRange.catchAllHandlerAddress == -1) {
                tryRange.catchAllHandlerAddress = handlerAddress;
            }

            previousEnd = tryRange.endAddress;
            tryRange = tryRange.next;
        } while (tryRange.previous != endRange);
    }

    public Pair<TryRange, TryRange> getBoundingRanges(int startAddress, int endAddress) {
        TryRange startRange = null;
        TryRange endRange = null;

        TryRange tryRange = firstTryRange.next;
        while (tryRange != lastTryRange) {
            if (startAddress == tryRange.startAddress) {
                //|-----|
                //^------
                /*Bam. We hit the start of the range right on the head*/
                startRange = tryRange;
                break;
            } else if (startAddress > tryRange.startAddress && startAddress < tryRange.endAddress) {
                //|-----|
                //  ^----
                /*Almost. The start of the range being added is in the middle
                of an existing try range. We need to split the existing range
                at the start address of the range being added*/
                startRange = tryRange.split(startAddress);
                break;
            }else if (startAddress < tryRange.startAddress) {
                if (endAddress <= tryRange.startAddress) {
                    //      |-----|
                    //^--^
                    /*Oops, totally too far! The new range doesn't overlap any existing
                    ones, so we just add it and return*/
                    startRange = new TryRange(startAddress, endAddress);
                    tryRange.prepend(startRange);
                    return new Pair<TryRange, TryRange>(startRange, startRange);
                } else {
                    //   |-----|
                    //^---------
                    /*Oops, too far! We've passed the start of the range being added, but
                     the new range does overlap this one. We need to add a new range just
                     before this one*/
                    startRange = new TryRange(startAddress, tryRange.startAddress);
                    tryRange.prepend(startRange);
                    break;
                }
            }

            tryRange = tryRange.next;
        }

        //|-----|
        //        ^-----
        /*Either the list of tries is blank, or all the tries in the list
        end before the range being added starts. In either case, we just need
        to add a new range at the end of the list*/
        if (startRange == null) {
            startRange = new TryRange(startAddress, endAddress);
            lastTryRange.prepend(startRange);
            return new Pair<TryRange, TryRange>(startRange, startRange);
        }

        tryRange = startRange;
        while (tryRange != lastTryRange) {
            if (tryRange.endAddress == endAddress) {
                //|-----|
                //------^
                /*Bam! We hit the end right on the head.*/
                endRange = tryRange;
                break;
            } else if (tryRange.startAddress < endAddress && tryRange.endAddress > endAddress) {
                //|-----|
                //--^
                /*Almost. The range being added ends in the middle of an
                existing range. We need to split the existing range
                at the end of the range being added.*/
                tryRange.split(endAddress);
                endRange = tryRange;
                break;
            } else if (tryRange.startAddress >= endAddress) {
                //|-----|       |-----|
                //-----------^
                /*Oops, too far! The current range starts after the range being added
                ends. We need to create a new range that starts at the end of the
                previous range, and ends at the end of the range being added*/
                endRange = new TryRange(tryRange.previous.endAddress, endAddress);
                tryRange.prepend(endRange);
                break;
            }
            tryRange = tryRange.next;
        }

        //|-----|
        //--------^
        /*The last range in the list ended before the end of the range being added.
        We need to add a new range that starts at the end of the last range in the
        list, and ends at the end of the range being added.*/
        if (endRange == null) {
            endRange = new TryRange(lastTryRange.previous.endAddress, endAddress);
            lastTryRange.prepend(endRange);
        }

        return new Pair<TryRange, TryRange>(startRange, endRange);
    }

    public void addHandler(TypeIdItem type, int startAddress, int endAddress, int handlerAddress) {
        TryRange startRange;
        TryRange endRange;

        //TODO: need to check for pre-existing exception types in the handler list?

        Pair<TryRange, TryRange> ranges = getBoundingRanges(startAddress, endAddress);
        startRange = ranges.first;
        endRange = ranges.second;
        Handler handler = new Handler(type, handlerAddress);

        int previousEnd = startAddress;
        TryRange tryRange = startRange;

        /*Now we have the start and end ranges that exactly match the start and end
        of the range being added. We need to iterate over all the ranges from the start
        to end range inclusively, and append the handler to the end of each range's handler
        list. We also need to create a new range for any "holes" in the existing ranges*/
        do
        {
            //is there a hole? If so, add a new range to fill the hole
            if (tryRange.startAddress > previousEnd) {
                TryRange newRange = new TryRange(previousEnd, tryRange.startAddress);
                tryRange.prepend(newRange);
                tryRange = newRange;
            }

            tryRange.appendHandler(handler);
            previousEnd = tryRange.endAddress;
            tryRange = tryRange.next;
        } while (tryRange.previous != endRange);
    }
}
