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

package org.jf.dexlib;

import com.google.common.base.Preconditions;
import org.jf.util.AlignmentUtils;
import org.jf.dexlib.Util.AnnotatedOutput;
import org.jf.util.ExceptionWithContext;
import org.jf.dexlib.Util.Input;

public abstract class Item<T extends Item> implements Comparable<T> {
    /**
     * The offset of this item in the dex file, or -1 if not known
     */
    protected int offset = -1;

    /**
     * The index of this item in the containing section, or -1 if not known
     */
    protected int index = -1;

    /**
     * The DexFile that this item is associatedr with
     */
    protected final DexFile dexFile;

    /**
     * The constructor that is used when reading in a <code>DexFile</code>
     * @param dexFile the <code>DexFile</code> that this item is associated with
     */
    protected Item(DexFile dexFile) {
        assert dexFile != null;

        this.dexFile = dexFile;
    }

    /**
     * Read in the item from the given input stream, and initialize the index
     * @param in the <code>Input</code> object to read from
     * @param index the index within the containing section of the item being read in
     * @param readContext a <code>ReadContext</code> object to hold information that is
     * only needed while reading in a file
     */
    protected void readFrom(Input in, int index, ReadContext readContext) {
        try {
            assert AlignmentUtils.isAligned(in.getCursor(), getItemType().ItemAlignment);

            this.offset = in.getCursor();
            this.index = index;

            this.readItem(in, readContext);
        } catch (Exception ex) {
            throw addExceptionContext(ex);
        }
    }

    /**
     * Place the item at the given offset and index, and return the offset of the byte following this item
     * @param offset The offset to place the item at
     * @param index The index of the item within the containing section
     * @return The offset of the byte following this item
     */
    protected int placeAt(int offset, int index) {
        try {
            assert AlignmentUtils.isAligned(offset, getItemType().ItemAlignment);
            assert !dexFile.getInplace() || (offset == this.offset && this.index == index);

            this.offset = offset;
            this.index = index;
            return this.placeItem(offset);
        } catch (Exception ex) {
            throw addExceptionContext(ex);
        }
    }

    /**
     * Write and annotate this item to the output stream
     * @param out The output stream to write and annotate to
     */
    protected void writeTo(AnnotatedOutput out) {
        try {
            assert AlignmentUtils.isAligned(offset, getItemType().ItemAlignment);
            //ensure that it is being written to the same offset where it was previously placed
            assert out.getCursor() == offset;

            if (out.annotates()) {
                out.annotate(0, "[" + index + "] " + this.getItemType().TypeName);
            }

            out.indent();
            writeItem(out);
            out.deindent();
        } catch (Exception ex) {
            throw addExceptionContext(ex);
        }
    }

    /**
     * Returns a human readable form of this item
     * @return a human readable form of this item
     */
    public String toString() {
        return getConciseIdentity();
    }

    /**
     * The method in the concrete item subclass that actually reads in the data for the item
     *
     * The logic in this method can assume that the given Input object is valid and is
     * aligned as neccessary.
     *
     * This method is for internal use only
     * @param in the <code>Input</code> object to read from
     * @param readContext a <code>ReadContext</code> object to hold information that is
     * only needed while reading in a file
     */
    protected abstract void readItem(Input in, ReadContext readContext);

    /**
     * The method should finalize the layout of the item and return the offset of the byte
     * immediately following the item.
     *
     * The implementation of this method can assume that the offset argument has already been
     * aligned based on the item's alignment requirements
     *
     * This method is for internal use only
     * @param offset the (pre-aligned) offset to place the item at
     * @return the size of the item, in bytes
     */
    protected abstract int placeItem(int offset);

    /**
     * The method in the concrete item subclass that actually writes and annotates the data
     * for the item.
     *
     * The logic in this method can assume that the given Output object is valid and is
     * aligned as neccessary
     *
     * @param out The <code>AnnotatedOutput</code> object to write/annotate to
     */
    protected abstract void writeItem(AnnotatedOutput out);

    /**
     * This method is called to add item specific context information to an exception, to identify the "current item"
     * when the exception occured. It adds the value returned by <code>getConciseIdentity</code> as context for the
     * exception
     * @param ex The exception that occured
     * @return A RuntimeException with additional details about the item added
     */
    protected final RuntimeException addExceptionContext(Exception ex) {
        return ExceptionWithContext.withContext(ex, getConciseIdentity());
    }

    /**
     * @return An ItemType enum that represents the item type of this item
     */
    public abstract ItemType getItemType();

    /**
     * @return A concise (human-readable) string value that conveys the identity of this item
     */
    public abstract String getConciseIdentity();


    /**
     * Note that the item must have been placed before calling this method (See <code>DexFile.place()</code>)
     * @return the offset in the dex file where this item is located
     */
    public int getOffset() {
        Preconditions.checkState(offset != -1,
                "The offset is not set until the DexFile containing this item is placed.");
        return offset;
    }

    /**
     * Note that the item must have been placed before calling this method (See <code>DexFile.place()</code>)
     * @return the index of this item within the item's containing section.
     */
    public int getIndex() {
        Preconditions.checkState(index != -1,
                "The index is not set until the DexFile containing this item is placed.");
        return index;
    }

    /**
     * @return True if this item has been placed, otherwise False
     */
    public boolean isPlaced() {
        return offset != -1;
    }

    /**
     * @return the <code>DexFile</code> that contains this item
     */
    public DexFile getDexFile() {
        return dexFile;
    }
}
