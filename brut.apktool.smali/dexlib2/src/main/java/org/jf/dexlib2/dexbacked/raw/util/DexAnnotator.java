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

package org.jf.dexlib2.dexbacked.raw.util;

import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
import org.jf.dexlib2.dexbacked.raw.*;
import org.jf.dexlib2.util.AnnotatedBytes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Writer;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class DexAnnotator extends AnnotatedBytes {
    @Nonnull public final RawDexFile dexFile;

    private final Map<Integer, SectionAnnotator> annotators = Maps.newHashMap();
    private static final Map<Integer, Integer> sectionAnnotationOrder = Maps.newHashMap();

    static {
        int[] sectionOrder = new int[] {
                ItemType.MAP_LIST,

                ItemType.HEADER_ITEM,
                ItemType.STRING_ID_ITEM,
                ItemType.TYPE_ID_ITEM,
                ItemType.PROTO_ID_ITEM,
                ItemType.FIELD_ID_ITEM,
                ItemType.METHOD_ID_ITEM,

                // these need to be ordered like this, so the item identities can be propagated
                ItemType.CLASS_DEF_ITEM,
                ItemType.CLASS_DATA_ITEM,
                ItemType.CODE_ITEM,
                ItemType.DEBUG_INFO_ITEM,

                ItemType.TYPE_LIST,
                ItemType.ANNOTATION_SET_REF_LIST,
                ItemType.ANNOTATION_SET_ITEM,
                ItemType.STRING_DATA_ITEM,
                ItemType.ANNOTATION_ITEM,
                ItemType.ENCODED_ARRAY_ITEM,
                ItemType.ANNOTATION_DIRECTORY_ITEM
        };

        for (int i=0; i<sectionOrder.length; i++) {
            sectionAnnotationOrder.put(sectionOrder[i], i);
        }
    }

    public DexAnnotator(@Nonnull RawDexFile dexFile, int width) {
        super(width);

        this.dexFile = dexFile;

        for (MapItem mapItem: dexFile.getMapItems()) {
            switch (mapItem.getType()) {
                case ItemType.HEADER_ITEM:
                    annotators.put(mapItem.getType(), HeaderItem.makeAnnotator(this, mapItem));
                    break;
                case ItemType.STRING_ID_ITEM:
                    annotators.put(mapItem.getType(), StringIdItem.makeAnnotator(this, mapItem));
                    break;
                case ItemType.TYPE_ID_ITEM:
                    annotators.put(mapItem.getType(), TypeIdItem.makeAnnotator(this, mapItem));
                    break;
                case ItemType.PROTO_ID_ITEM:
                    annotators.put(mapItem.getType(), ProtoIdItem.makeAnnotator(this, mapItem));
                    break;
                case ItemType.FIELD_ID_ITEM:
                    annotators.put(mapItem.getType(), FieldIdItem.makeAnnotator(this, mapItem));
                    break;
                case ItemType.METHOD_ID_ITEM:
                    annotators.put(mapItem.getType(), MethodIdItem.makeAnnotator(this, mapItem));
                    break;
                case ItemType.CLASS_DEF_ITEM:
                    annotators.put(mapItem.getType(), ClassDefItem.makeAnnotator(this, mapItem));
                    break;
                case ItemType.MAP_LIST:
                    annotators.put(mapItem.getType(), MapItem.makeAnnotator(this, mapItem));
                    break;
                case ItemType.TYPE_LIST:
                    annotators.put(mapItem.getType(), TypeListItem.makeAnnotator(this, mapItem));
                    break;
                case ItemType.ANNOTATION_SET_REF_LIST:
                    annotators.put(mapItem.getType(), AnnotationSetRefList.makeAnnotator(this, mapItem));
                    break;
                case ItemType.ANNOTATION_SET_ITEM:
                    annotators.put(mapItem.getType(), AnnotationSetItem.makeAnnotator(this, mapItem));
                    break;
                case ItemType.CLASS_DATA_ITEM:
                    annotators.put(mapItem.getType(), ClassDataItem.makeAnnotator(this, mapItem));
                    break;
                case ItemType.CODE_ITEM:
                    annotators.put(mapItem.getType(), CodeItem.makeAnnotator(this, mapItem));
                    break;
                case ItemType.STRING_DATA_ITEM:
                    annotators.put(mapItem.getType(), StringDataItem.makeAnnotator(this, mapItem));
                    break;
                case ItemType.DEBUG_INFO_ITEM:
                    annotators.put(mapItem.getType(), DebugInfoItem.makeAnnotator(this, mapItem));
                    break;
                case ItemType.ANNOTATION_ITEM:
                    annotators.put(mapItem.getType(), AnnotationItem.makeAnnotator(this, mapItem));
                    break;
                case ItemType.ENCODED_ARRAY_ITEM:
                    annotators.put(mapItem.getType(), EncodedArrayItem.makeAnnotator(this, mapItem));
                    break;
                case ItemType.ANNOTATION_DIRECTORY_ITEM:
                    annotators.put(mapItem.getType(), AnnotationDirectoryItem.makeAnnotator(this, mapItem));
                    break;
                default:
                    throw new RuntimeException(String.format("Unrecognized item type: 0x%x", mapItem.getType()));
            }
        }
    }

    public void writeAnnotations(Writer out) throws IOException {
        List<MapItem> mapItems = dexFile.getMapItems();
        // sort the map items based on the order defined by sectionAnnotationOrder
        Ordering<MapItem> ordering = Ordering.from(new Comparator<MapItem>() {
            @Override public int compare(MapItem o1, MapItem o2) {
                return Ints.compare(sectionAnnotationOrder.get(o1.getType()), sectionAnnotationOrder.get(o2.getType()));
            }
        });

        mapItems = ordering.immutableSortedCopy(mapItems);

        try {
            for (MapItem mapItem: mapItems) {
                SectionAnnotator annotator = annotators.get(mapItem.getType());
                annotator.annotateSection(this);
            }
        } finally {
            dexFile.writeAnnotations(out, this);
        }
    }

    @Nullable
    public SectionAnnotator getAnnotator(int itemType) {
        return annotators.get(itemType);
    }
}
