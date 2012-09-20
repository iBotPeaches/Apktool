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

class ItemFactory {
    protected static Item makeItem(ItemType itemType, DexFile dexFile) {
        switch (itemType) {
            case TYPE_STRING_ID_ITEM:
                return new StringIdItem(dexFile);
            case TYPE_TYPE_ID_ITEM:
                return new TypeIdItem(dexFile);
            case TYPE_PROTO_ID_ITEM:
                return new ProtoIdItem(dexFile);
            case TYPE_FIELD_ID_ITEM:
                return new FieldIdItem(dexFile);
            case TYPE_METHOD_ID_ITEM:
                return new MethodIdItem(dexFile);
            case TYPE_CLASS_DEF_ITEM:
                return new ClassDefItem(dexFile);
            case TYPE_TYPE_LIST:
                return new TypeListItem(dexFile);
            case TYPE_ANNOTATION_SET_REF_LIST:
                return new AnnotationSetRefList(dexFile);
            case TYPE_ANNOTATION_SET_ITEM:
                return new AnnotationSetItem(dexFile);
            case TYPE_CLASS_DATA_ITEM:
                return new ClassDataItem(dexFile);
            case TYPE_CODE_ITEM:
                return new CodeItem(dexFile);
            case TYPE_STRING_DATA_ITEM:
                return new StringDataItem(dexFile);
            case TYPE_DEBUG_INFO_ITEM:
                return new DebugInfoItem(dexFile);
            case TYPE_ANNOTATION_ITEM:
                return new AnnotationItem(dexFile);
            case TYPE_ENCODED_ARRAY_ITEM:
                return new EncodedArrayItem(dexFile);
            case TYPE_ANNOTATIONS_DIRECTORY_ITEM:
                return new AnnotationDirectoryItem(dexFile);
            default:
                assert false;
        }
        return null;
    }
}
