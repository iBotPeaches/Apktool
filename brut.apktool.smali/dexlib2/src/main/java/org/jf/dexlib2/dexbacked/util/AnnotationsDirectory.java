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

package org.jf.dexlib2.dexbacked.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.jf.dexlib2.dexbacked.DexBackedAnnotation;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Set;

public abstract class AnnotationsDirectory {
    public static final AnnotationsDirectory EMPTY = new AnnotationsDirectory() {
        @Override public int getFieldAnnotationCount() { return 0; }
        @Nonnull @Override public Set<? extends DexBackedAnnotation> getClassAnnotations() { return ImmutableSet.of(); }
        @Nonnull @Override public AnnotationIterator getFieldAnnotationIterator() { return AnnotationIterator.EMPTY; }
        @Nonnull @Override public AnnotationIterator getMethodAnnotationIterator() { return AnnotationIterator.EMPTY; }
        @Nonnull @Override public AnnotationIterator getParameterAnnotationIterator() {return AnnotationIterator.EMPTY;}
    };

    public abstract int getFieldAnnotationCount();
    @Nonnull public abstract Set<? extends DexBackedAnnotation> getClassAnnotations();
    @Nonnull public abstract AnnotationIterator getFieldAnnotationIterator();
    @Nonnull public abstract AnnotationIterator getMethodAnnotationIterator();
    @Nonnull public abstract AnnotationIterator getParameterAnnotationIterator();

    @Nonnull
    public static AnnotationsDirectory newOrEmpty(@Nonnull DexBackedDexFile dexFile,
                                                  int directoryAnnotationsOffset) {
        if (directoryAnnotationsOffset == 0) {
            return EMPTY;
        }
        return new AnnotationsDirectoryImpl(dexFile, directoryAnnotationsOffset);
    }

    /**
     * This provides a forward-only, skipable iteration over the field_annotation, method_annotation or
     * parameter_annotation lists in an annotations_directory_item.
     *
     * These lists associate a key, either a field or method index, with an offset to where the annotation data for
     * that field/method/parameter is stored.
     */
    public interface AnnotationIterator {
        public static final AnnotationIterator EMPTY = new AnnotationIterator() {
            @Override public int seekTo(int key) { return 0; }
            @Override public void reset() {}
        };

        /**
         * Seeks the iterator forward, to the first item whose key is >= the requested key. If the requested key value
         * is less than that of the item that the iterator currently points to, it will not be moved forward.
         *
         * If an item with the requested key is found, the associated annotation offset is returned. Otherwise, 0 is
         * returned.
         *
         * @param key The method/field index to search for
         * @return The annotation offset associated with the requested key, or 0 if not found.
         */
        public int seekTo(int key);

        /**
         * Resets the iterator to the beginning of its list.
         */
        public void reset();
    }

    @Nonnull
    public static Set<? extends DexBackedAnnotation> getAnnotations(@Nonnull final DexBackedDexFile dexFile,
                                                                     final int annotationSetOffset) {
        if (annotationSetOffset != 0) {
            final int size = dexFile.readSmallUint(annotationSetOffset);
            return new FixedSizeSet<DexBackedAnnotation>() {
                @Nonnull
                @Override
                public DexBackedAnnotation readItem(int index) {
                    int annotationOffset = dexFile.readSmallUint(annotationSetOffset + 4 + (4*index));
                    return new DexBackedAnnotation(dexFile, annotationOffset);
                }

                @Override public int size() { return size; }
            };
        }

        return ImmutableSet.of();
    }

    @Nonnull
    public static List<Set<? extends DexBackedAnnotation>> getParameterAnnotations(
            @Nonnull final DexBackedDexFile dexFile, final int annotationSetListOffset) {
        if (annotationSetListOffset > 0) {
            final int size = dexFile.readSmallUint(annotationSetListOffset);

            return new FixedSizeList<Set<? extends DexBackedAnnotation>>() {
                @Nonnull
                @Override
                public Set<? extends DexBackedAnnotation> readItem(int index) {
                    int annotationSetOffset = dexFile.readSmallUint(annotationSetListOffset + 4 + index * 4);
                    return getAnnotations(dexFile, annotationSetOffset);
                }

                @Override public int size() { return size; }
            };
        }
        return ImmutableList.of();
    }

    private static class AnnotationsDirectoryImpl extends AnnotationsDirectory {
        @Nonnull public final DexBackedDexFile dexFile;
        private final int directoryOffset;

        private static final int FIELD_COUNT_OFFSET = 4;
        private static final int METHOD_COUNT_OFFSET = 8;
        private static final int PARAMETER_COUNT_OFFSET = 12;
        private static final int ANNOTATIONS_START_OFFSET = 16;

        /** The size of a field_annotation structure */
        private static final int FIELD_ANNOTATION_SIZE = 8;
        /** The size of a method_annotation structure */
        private static final int METHOD_ANNOTATION_SIZE = 8;

        public AnnotationsDirectoryImpl(@Nonnull DexBackedDexFile dexFile,
                                        int directoryOffset) {
            this.dexFile = dexFile;
            this.directoryOffset = directoryOffset;
        }

        public int getFieldAnnotationCount() {
            return dexFile.readSmallUint(directoryOffset + FIELD_COUNT_OFFSET);
        }

        public int getMethodAnnotationCount() {
            return dexFile.readSmallUint(directoryOffset + METHOD_COUNT_OFFSET);
        }

        public int getParameterAnnotationCount() {
            return dexFile.readSmallUint(directoryOffset + PARAMETER_COUNT_OFFSET);
        }

        @Nonnull
        public Set<? extends DexBackedAnnotation> getClassAnnotations() {
            return getAnnotations(dexFile, dexFile.readSmallUint(directoryOffset));
        }

        @Nonnull
        public AnnotationIterator getFieldAnnotationIterator() {
            int fieldAnnotationCount = getFieldAnnotationCount();
            if (fieldAnnotationCount == 0) {
                return AnnotationIterator.EMPTY;
            }
            return new AnnotationIteratorImpl(directoryOffset + ANNOTATIONS_START_OFFSET, fieldAnnotationCount);
        }

        @Nonnull
        public AnnotationIterator getMethodAnnotationIterator() {
            int methodCount = getMethodAnnotationCount();
            if (methodCount == 0) {
                return AnnotationIterator.EMPTY;
            }
            int fieldCount = getFieldAnnotationCount();
            int methodAnnotationsOffset = directoryOffset + ANNOTATIONS_START_OFFSET +
                    fieldCount * FIELD_ANNOTATION_SIZE;
            return new AnnotationIteratorImpl(methodAnnotationsOffset, methodCount);
        }

        @Nonnull
        public AnnotationIterator getParameterAnnotationIterator() {
            int parameterAnnotationCount = getParameterAnnotationCount();
            if (parameterAnnotationCount == 0) {
                return AnnotationIterator.EMPTY;
            }
            int fieldCount = getFieldAnnotationCount();
            int methodCount = getMethodAnnotationCount();
            int parameterAnnotationsOffset = directoryOffset + ANNOTATIONS_START_OFFSET +
                    fieldCount * FIELD_ANNOTATION_SIZE +
                    methodCount * METHOD_ANNOTATION_SIZE;
            return new AnnotationIteratorImpl(parameterAnnotationsOffset, parameterAnnotationCount);
        }

        private class AnnotationIteratorImpl implements AnnotationIterator {
            private final int startOffset;
            private final int size;
            private int currentIndex;
            private int currentItemIndex;

            public AnnotationIteratorImpl(int startOffset, int size) {
                this.startOffset = startOffset;
                this.size = size;
                this.currentItemIndex = dexFile.readSmallUint(startOffset);
                this.currentIndex = 0;
            }

            public int seekTo(int itemIndex) {
                while (currentItemIndex < itemIndex && (currentIndex+1) < size) {
                    currentIndex++;
                    currentItemIndex = dexFile.readSmallUint(startOffset + (currentIndex*8));
                }

                if (currentItemIndex == itemIndex) {
                    return dexFile.readSmallUint(startOffset + (currentIndex*8)+4);
                }
                return 0;
            }

            public void reset() {
                this.currentItemIndex = dexFile.readSmallUint(startOffset);
                this.currentIndex = 0;
            }
        }
    }
}
