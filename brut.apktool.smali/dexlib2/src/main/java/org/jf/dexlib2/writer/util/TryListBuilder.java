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

import com.google.common.collect.Lists;
import org.jf.dexlib2.base.BaseTryBlock;
import org.jf.dexlib2.iface.ExceptionHandler;
import org.jf.dexlib2.iface.TryBlock;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class TryListBuilder<EH extends ExceptionHandler>
{
    // Linked list sentinels that don't represent an actual try block
    // Their values are never modified, only their links
    private final MutableTryBlock<EH> listStart;
    private final MutableTryBlock<EH> listEnd;

    public TryListBuilder() {
        listStart = new MutableTryBlock<EH>(0, 0);
        listEnd = new MutableTryBlock<EH>(0, 0);
        listStart.next = listEnd;
        listEnd.prev = listStart;
    }

    public static <EH extends ExceptionHandler> List<TryBlock<EH>> massageTryBlocks(
            List<? extends TryBlock<? extends EH>> tryBlocks) {
        TryListBuilder<EH> tlb = new TryListBuilder<EH>();

        for (TryBlock<? extends EH> tryBlock: tryBlocks) {
            int startAddress = tryBlock.getStartCodeAddress();
            int endAddress = startAddress + tryBlock.getCodeUnitCount();

            for (EH exceptionHandler: tryBlock.getExceptionHandlers()) {
                tlb.addHandler(startAddress, endAddress, exceptionHandler);
            }
        }
        return tlb.getTryBlocks();
    }

    private static class TryBounds<EH extends ExceptionHandler> {
        @Nonnull public final MutableTryBlock<EH> start;
        @Nonnull public final MutableTryBlock<EH> end;

        public TryBounds(@Nonnull MutableTryBlock<EH> start, @Nonnull MutableTryBlock<EH> end) {
            this.start = start;
            this.end = end;
        }
    }

    public static class InvalidTryException extends ExceptionWithContext {
        public InvalidTryException(Throwable cause) {
            super(cause);
        }

        public InvalidTryException(Throwable cause, String message, Object... formatArgs) {
            super(cause, message, formatArgs);
        }

        public InvalidTryException(String message, Object... formatArgs) {
            super(message, formatArgs);
        }
    }

    private static class MutableTryBlock<EH extends ExceptionHandler> extends BaseTryBlock<EH> {
        public MutableTryBlock<EH> prev = null;
        public MutableTryBlock<EH> next = null;

        public int startCodeAddress;
        public int endCodeAddress;
        @Nonnull public List<EH> exceptionHandlers = Lists.newArrayList();

        public MutableTryBlock(int startCodeAddress, int endCodeAddress) {
            this.startCodeAddress = startCodeAddress;
            this.endCodeAddress = endCodeAddress;
        }

        public MutableTryBlock(int startCodeAddress, int endCodeAddress,
                               @Nonnull List<EH> exceptionHandlers) {
            this.startCodeAddress = startCodeAddress;
            this.endCodeAddress = endCodeAddress;
            this.exceptionHandlers = Lists.newArrayList(exceptionHandlers);
        }

        @Override public int getStartCodeAddress() {
            return startCodeAddress;
        }

        @Override public int getCodeUnitCount() {
            return endCodeAddress - startCodeAddress;
        }

        @Nonnull @Override public List<EH> getExceptionHandlers() {
            return exceptionHandlers;
        }

        @Nonnull
        public MutableTryBlock<EH> split(int splitAddress) {
            MutableTryBlock<EH> newTryBlock = new MutableTryBlock<EH>(splitAddress, endCodeAddress, exceptionHandlers);
            endCodeAddress = splitAddress;
            append(newTryBlock);
            return newTryBlock;
        }

        public void delete() {
            next.prev = prev;
            prev.next = next;
        }

        public void mergeNext() {
            //assert next.startCodeAddress == this.endCodeAddress;
            this.endCodeAddress = next.endCodeAddress;
            next.delete();
        }

        public void append(@Nonnull MutableTryBlock<EH> tryBlock) {
            next.prev = tryBlock;
            tryBlock.next = next;
            tryBlock.prev = this;
            next = tryBlock;
        }

        public void prepend(@Nonnull MutableTryBlock<EH> tryBlock) {
            prev.next = tryBlock;
            tryBlock.prev = prev;
            tryBlock.next = this;
            prev = tryBlock;
        }

        public void addHandler(@Nonnull EH handler) {
            for (ExceptionHandler existingHandler: exceptionHandlers) {
                String existingType = existingHandler.getExceptionType();
                String newType = handler.getExceptionType();

                // Don't add it if we already have a handler of the same type
                if (existingType == null) {
                    if (newType == null) {
                        if (existingHandler.getHandlerCodeAddress() != handler.getHandlerCodeAddress()) {
                            throw new InvalidTryException(
                                    "Multiple overlapping catch all handlers with different handlers");
                        }
                        return;
                    }
                } else if (existingType.equals(newType)) {
                    if (existingHandler.getHandlerCodeAddress() != handler.getHandlerCodeAddress()) {
                        throw new InvalidTryException(
                                "Multiple overlapping catches for %s with different handlers", existingType);
                    }
                    return;
                }
            }

            exceptionHandlers.add(handler);
        }
    }

    private TryBounds<EH> getBoundingRanges(int startAddress, int endAddress) {
        MutableTryBlock<EH> startBlock = null;

        MutableTryBlock<EH> tryBlock = listStart.next;
        while (tryBlock != listEnd) {
            int currentStartAddress = tryBlock.startCodeAddress;
            int currentEndAddress = tryBlock.endCodeAddress;

            if (startAddress == currentStartAddress) {
                //|-----|
                //^------
                /*Bam. We hit the start of the range right on the head*/
                startBlock = tryBlock;
                break;
            } else if (startAddress > currentStartAddress && startAddress < currentEndAddress) {
                //|-----|
                //  ^----
                /*Almost. The start of the range being added is in the middle
                of an existing try range. We need to split the existing range
                at the start address of the range being added*/
                startBlock = tryBlock.split(startAddress);
                break;
            }else if (startAddress < currentStartAddress) {
                if (endAddress <= currentStartAddress) {
                    //      |-----|
                    //^--^
                    /*Oops, totally too far! The new range doesn't overlap any existing
                    ones, so we just add it and return*/
                    startBlock = new MutableTryBlock<EH>(startAddress, endAddress);
                    tryBlock.prepend(startBlock);
                    return new TryBounds<EH>(startBlock, startBlock);
                } else {
                    //   |-----|
                    //^---------
                    /*Oops, too far! We've passed the start of the range being added, but
                     the new range does overlap this one. We need to add a new range just
                     before this one*/
                    startBlock = new MutableTryBlock<EH>(startAddress, currentStartAddress);
                    tryBlock.prepend(startBlock);
                    break;
                }
            }

            tryBlock = tryBlock.next;
        }

        //|-----|
        //        ^-----
        /*Either the list of tries is blank, or all the tries in the list
        end before the range being added starts. In either case, we just need
        to add a new range at the end of the list*/
        if (startBlock == null) {
            startBlock = new MutableTryBlock<EH>(startAddress, endAddress);
            listEnd.prepend(startBlock);
            return new TryBounds<EH>(startBlock, startBlock);
        }

        tryBlock = startBlock;
        while (tryBlock != listEnd) {
            int currentStartAddress = tryBlock.startCodeAddress;
            int currentEndAddress = tryBlock.endCodeAddress;

            if (endAddress == currentEndAddress) {
                //|-----|
                //------^
                /*Bam! We hit the end right on the head... err, tail.*/
                return new TryBounds<EH>(startBlock, tryBlock);
            } else if (endAddress > currentStartAddress && endAddress < currentEndAddress) {
                //|-----|
                //--^
                /*Almost. The range being added ends in the middle of an
                existing range. We need to split the existing range
                at the end of the range being added.*/
                tryBlock.split(endAddress);
                return new TryBounds<EH>(startBlock, tryBlock);
            } else if (endAddress <= currentStartAddress) {
                //|-----|       |-----|
                //-----------^
                /*Oops, too far! The current range starts after the range being added
                ends. We need to create a new range that starts at the end of the
                previous range, and ends at the end of the range being added*/
                MutableTryBlock<EH> endBlock = new MutableTryBlock<EH>(tryBlock.prev.endCodeAddress, endAddress);
                tryBlock.prepend(endBlock);
                return new TryBounds<EH>(startBlock, endBlock);
            }
            tryBlock = tryBlock.next;
        }

        //|-----|
        //--------^
        /*The last range in the list ended before the end of the range being added.
        We need to add a new range that starts at the end of the last range in the
        list, and ends at the end of the range being added.*/
        MutableTryBlock<EH> endBlock = new MutableTryBlock<EH>(listEnd.prev.endCodeAddress, endAddress);
        listEnd.prepend(endBlock);
        return new TryBounds<EH>(startBlock, endBlock);
    }

    public void addHandler(int startAddress, int endAddress, EH handler) {
        TryBounds<EH> bounds = getBoundingRanges(startAddress, endAddress);

        MutableTryBlock<EH> startBlock = bounds.start;
        MutableTryBlock<EH> endBlock = bounds.end;

        int previousEnd = startAddress;
        MutableTryBlock<EH> tryBlock = startBlock;

        /*Now we have the start and end ranges that exactly match the start and end
        of the range being added. We need to iterate over all the ranges from the start
        to end range inclusively, and append the handler to the end of each range's handler
        list. We also need to create a new range for any "holes" in the existing ranges*/
        do
        {
            //is there a hole? If so, add a new range to fill the hole
            if (tryBlock.startCodeAddress > previousEnd) {
                MutableTryBlock<EH> newBlock = new MutableTryBlock<EH>(previousEnd, tryBlock.startCodeAddress);
                tryBlock.prepend(newBlock);
                tryBlock = newBlock;
            }

            tryBlock.addHandler(handler);
            previousEnd = tryBlock.endCodeAddress;
            tryBlock = tryBlock.next;
        } while (tryBlock.prev != endBlock);
    }

    public List<TryBlock<EH>> getTryBlocks() {
        return Lists.newArrayList(new Iterator<TryBlock<EH>>() {
            // The next TryBlock to return. This has already been merged, if needed.
            @Nullable private MutableTryBlock<EH> next;

            {
                next = listStart;
                next = readNextItem();
            }

            /**
             * Read the item that comes after the current value of the next field.
             * @return The next item, or null if there is no next item
             */
            @Nullable protected MutableTryBlock<EH> readNextItem() {
                // We can assume that next is not null, due to the way iteration happens
                MutableTryBlock<EH> ret = next.next;

                if (ret == listEnd) {
                    return null;
                }

                while (ret.next != listEnd) {
                    if (ret.endCodeAddress == ret.next.startCodeAddress &&
                            ret.getExceptionHandlers().equals(ret.next.getExceptionHandlers())) {
                        ret.mergeNext();
                    } else {
                        break;
                    }
                }
                return ret;
            }

            @Override public boolean hasNext() {
                return next != null;
            }

            @Override @Nonnull public TryBlock<EH> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                TryBlock<EH> ret = next;
                next = readNextItem();
                // ret can't be null (ret=next and hasNext returned true)
                return ret;
            }

            @Override public void remove() {
                throw new UnsupportedOperationException();
            }
        });
    }
}
