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

package org.jf.dexlib;

import org.jf.dexlib.Util.AnnotatedOutput;
import org.jf.dexlib.Util.Input;

public class ProtoIdItem extends Item<ProtoIdItem> {
    private int hashCode = 0;

    private StringIdItem shortyDescriptor;
    private TypeIdItem returnType;
    private TypeListItem parameters;

    /**
     * Creates a new uninitialized <code>ProtoIdItem</code>
     * @param dexFile The <code>DexFile</code> that this item belongs to
     */
    protected ProtoIdItem(DexFile dexFile) {
        super(dexFile);
    }

    /**
     * Creates a new <code>ProtoIdItem</code> with the given values
     * @param dexFile The <code>DexFile</code> that this item belongs to
     * @param returnType the return type
     * @param parameters a <code>TypeListItem</code> containing a list of the parameter types
     */
    private ProtoIdItem(DexFile dexFile, TypeIdItem returnType, TypeListItem parameters) {
        this(dexFile);

        String shortyString = returnType.toShorty();
        if (parameters != null) {
            shortyString += parameters.getShortyString();
        }
        this.shortyDescriptor = StringIdItem.internStringIdItem(dexFile, shortyString);
        this.returnType = returnType;
        this.parameters = parameters;
    }

    /**
     * Returns a <code>ProtoIdItem</code> for the given values, and that has been interned into
     * the given <code>DexFile</code>
     * @param dexFile The <code>DexFile</code> that this item belongs to
     * @param returnType the return type
     * @param parameters a <code>TypeListItem</code> containing a list of the parameter types
     * @return a <code>ProtoIdItem</code> for the given values, and that has been interned into
     * the given <code>DexFile</code>
     */
    public static ProtoIdItem internProtoIdItem(DexFile dexFile, TypeIdItem returnType, TypeListItem parameters) {
        ProtoIdItem protoIdItem = new ProtoIdItem(dexFile, returnType, parameters);
        return dexFile.ProtoIdsSection.intern(protoIdItem);
    }

    /**
     * Looks up the <code>ProtoIdItem</code> from the given <code>DexFile</code> for the given
     * values
     * @param dexFile the <code>Dexfile</code> to find the type in
     * @param returnType the return type
     * @param parameters a <code>TypeListItem</code> containing a list of the parameter types
     * @return a <code>ProtoIdItem</code> from the given <code>DexFile</code> for the given
     * values, or null if it doesn't exist
     */
    public static ProtoIdItem lookupProtoIdItem(DexFile dexFile, TypeIdItem returnType, TypeListItem parameters) {
        ProtoIdItem protoIdItem = new ProtoIdItem(dexFile, returnType, parameters);
        return dexFile.ProtoIdsSection.getInternedItem(protoIdItem);
    }

    /** {@inheritDoc} */
    protected void readItem(Input in, ReadContext readContext) {
        shortyDescriptor = dexFile.StringIdsSection.getItemByIndex(in.readInt());
        returnType = dexFile.TypeIdsSection.getItemByIndex(in.readInt());
        parameters = (TypeListItem)readContext.getOptionalOffsettedItemByOffset(ItemType.TYPE_TYPE_LIST, in.readInt());
    }

    /** {@inheritDoc} */
    protected int placeItem(int offset) {
        return offset + 12;
    }

    /** {@inheritDoc} */
    protected void writeItem(AnnotatedOutput out) {
        if (out.annotates()) {
            out.annotate(4, "shorty_descriptor: " + shortyDescriptor.getStringValue());
            out.annotate(4, "return_type: " + returnType.getTypeDescriptor());

            if (parameters == null) {
                out.annotate(4, "parameters:");
            } else {
                out.annotate(4, "parameters: " + parameters.getTypeListString(""));
            }
        }

        out.writeInt(shortyDescriptor.getIndex());
        out.writeInt(returnType.getIndex());
        out.writeInt(parameters == null?0:parameters.getOffset());
    }

    /** {@inheritDoc} */
    public ItemType getItemType() {
        return ItemType.TYPE_PROTO_ID_ITEM;
    }

    /** {@inheritDoc} */
    public int compareTo(ProtoIdItem o) {
        int result = returnType.compareTo(o.returnType);
        if (result != 0) {
            return result;
        }

        if (parameters == null) {
            if (o.parameters == null) {
                return 0;
            }
            return -1;
        } else if (o.parameters == null) {
            return 1;
        }

        return parameters.compareTo(o.parameters);
    }

    /** {@inheritDoc} */
    public String getConciseIdentity() {
        return "proto_id_item: " + getPrototypeString();
    }

    private String cachedPrototypeString = null;
    /**
     * @return a string in the format (TTTT..)R where TTTT.. are the parameter types and R is the return type
     */
    public String getPrototypeString() {
        if (cachedPrototypeString == null) {
            StringBuilder sb = new StringBuilder("(");
            if (parameters != null) {
                sb.append(parameters.getTypeListString(""));
            }
            sb.append(")");
            sb.append(returnType.getTypeDescriptor());

            cachedPrototypeString = sb.toString();
        }
        return cachedPrototypeString;
    }

    /**
     * @return the return type of the method
     */
    public TypeIdItem getReturnType() {
        return returnType;
    }

    /**
     * @return a <code>TypeListItem</code> containing the method parameter types
     */
    public TypeListItem getParameters() {
        return parameters;
    }

    /**
     * @return the number of registers required for the parameters of this <code>ProtoIdItem</code>
     */
    public int getParameterRegisterCount() {
        if (parameters == null) {
            return 0;
        } else {
            return parameters.getRegisterCount();
        }
    }

    /**
     * calculate and cache the hashcode
     */
    private void calcHashCode() {
        hashCode = returnType.hashCode();
        hashCode = 31 * hashCode + (parameters==null?0:parameters.hashCode());
    }

    @Override
    public int hashCode() {
        //there's a small possibility that the actual hash code will be 0. If so, we'll
        //just end up recalculating it each time
        if (hashCode == 0)
            calcHashCode();
        return hashCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this==o) {
            return true;
        }
        if (o==null || !this.getClass().equals(o.getClass())) {
            return false;
        }

        //This assumes that the referenced items have been interned in both objects.
        //This is a valid assumption because all outside code must use the static
        //"getInterned..." style methods to make new items, and any item created
        //internally is guaranteed to be interned
        ProtoIdItem other = (ProtoIdItem)o;
        return (returnType == other.returnType &&
                parameters == other.parameters);
    }
}
