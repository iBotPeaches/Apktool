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

package org.jf.dexlib2.util;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jf.util.Hex;
import org.jf.util.TwoColumnOutput;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Collects/presents a set of textual annotations, each associated with a range of bytes or a specific point
 * between bytes.
 *
 * Point annotations cannot occur within the middle of a range annotation, only at the endpoints, or some other area
 * with no range annotation.
 *
 * Multiple point annotations can be defined for a given point. They will be printed in insertion order.
 *
 * Only a single range annotation may exist for any given range of bytes. Range annotations may not overlap.
 */
public class AnnotatedBytes {
    /**
     * This defines the bytes ranges and their associated range and point annotations.
     *
     * A range is defined by 2 consecutive keys in the map. The first key is the inclusive start point, the second key
     * is the exclusive end point. The range annotation for a range is associated with the first key for that range.
     * The point annotations for a point are associated with the key at that point.
     */
    @Nonnull private TreeMap<Integer, AnnotationEndpoint> annotatations = Maps.newTreeMap();

    private int cursor;
    private int indentLevel;

    /** &gt;= 40 (if used); the desired maximum output width */
    private int outputWidth;

    /**
     * &gt;= 8 (if used); the number of bytes of hex output to use
     * in annotations
     */
    private int hexCols = 8;

    public AnnotatedBytes(int width) {
        this.outputWidth = width;
    }

    /**
     * Moves the cursor to a new location
     *
     * @param offset The offset to move to
     */
    public void moveTo(int offset) {
        cursor = offset;
    }

    /**
     * Moves the cursor forward or backward by some amount
     *
     * @param offset The amount to move the cursor
     */
    public void moveBy(int offset) {
        cursor += offset;
    }

    public void annotateTo(int offset, @Nonnull String msg, Object... formatArgs) {
        annotate(offset - cursor, msg, formatArgs);
    }

    /**
     * Add an annotation of the given length at the current location.
     *
     * The location
     *
     *
     * @param length the length of data being annotated
     * @param msg the annotation message
     * @param formatArgs format arguments to pass to String.format
     */
    public void annotate(int length, @Nonnull String msg, Object... formatArgs) {
        String formattedMsg = String.format(msg, formatArgs);
        int exclusiveEndOffset = cursor + length;

        AnnotationEndpoint endPoint = null;

        // Do we have an endpoint at the beginning of this annotation already?
        AnnotationEndpoint startPoint = annotatations.get(cursor);
        if (startPoint == null) {
            // Nope. We need to check that we're not in the middle of an existing range annotation.
            Map.Entry<Integer, AnnotationEndpoint> previousEntry = annotatations.lowerEntry(cursor);
            if (previousEntry != null) {
                AnnotationEndpoint previousAnnotations = previousEntry.getValue();
                AnnotationItem previousRangeAnnotation = previousAnnotations.rangeAnnotation;
                if (previousRangeAnnotation != null) {
                    throw new IllegalStateException(
                            String.format("Cannot add annotation %s, due to existing annotation %s",
                            formatAnnotation(cursor, cursor + length, formattedMsg),
                            formatAnnotation(previousEntry.getKey(), previousRangeAnnotation.annotation)));
                }
            }
        } else if (length > 0) {
            AnnotationItem existingRangeAnnotation = startPoint.rangeAnnotation;
            if (existingRangeAnnotation != null) {
                throw new IllegalStateException(
                        String.format("Cannot add annotation %s, due to existing annotation %s",
                                formatAnnotation(cursor, cursor + length, formattedMsg),
                                formatAnnotation(cursor, existingRangeAnnotation.annotation)));
            }
        }

        if (length > 0) {
            // Ensure that there is no later annotation that would intersect with this one
            Map.Entry<Integer, AnnotationEndpoint> nextEntry = annotatations.higherEntry(cursor);
            if (nextEntry != null) {
                int nextKey = nextEntry.getKey();
                if (nextKey < exclusiveEndOffset) {
                    // there is an endpoint that would intersect with this annotation. Find one of the annotations
                    // associated with the endpoint, to print in the error message
                    AnnotationEndpoint nextEndpoint = nextEntry.getValue();
                    AnnotationItem nextRangeAnnotation = nextEndpoint.rangeAnnotation;
                    if (nextRangeAnnotation != null) {
                        throw new IllegalStateException(
                                String.format("Cannot add annotation %s, due to existing annotation %s",
                                        formatAnnotation(cursor, cursor + length, formattedMsg),
                                        formatAnnotation(nextKey, nextRangeAnnotation.annotation)));
                    }
                    if (nextEndpoint.pointAnnotations.size() > 0) {
                        throw new IllegalStateException(
                                String.format("Cannot add annotation %s, due to existing annotation %s",
                                        formatAnnotation(cursor, cursor + length, formattedMsg),
                                        formatAnnotation(nextKey, nextKey,
                                                nextEndpoint.pointAnnotations.get(0).annotation)));
                    }
                    // There are no annotations on this endpoint. This "shouldn't" happen. We can still throw an exception.
                    throw new IllegalStateException(
                            String.format("Cannot add annotation %s, due to existing annotation endpoint at %d",
                                    formatAnnotation(cursor, cursor + length, formattedMsg),
                                    nextKey));
                }

                if (nextKey == exclusiveEndOffset) {
                    // the next endpoint matches the end of the annotation we are adding
                    endPoint = nextEntry.getValue();
                }
            }
        }

        // Now, actually add the annotation
        // If startPoint is null, we need to create a new one and add it to annotations. Otherwise, we just need to add
        // the annotation to the existing AnnotationEndpoint
        // the range annotation
        if (startPoint == null) {
            startPoint = new AnnotationEndpoint();
            annotatations.put(cursor, startPoint);
        }
        if (length == 0) {
            startPoint.pointAnnotations.add(new AnnotationItem(indentLevel, formattedMsg));
        } else {
            startPoint.rangeAnnotation = new AnnotationItem(indentLevel, formattedMsg);

            // If endPoint is null, we need to create a new, empty one and add it to annotations
            if (endPoint == null) {
                endPoint = new AnnotationEndpoint();
                annotatations.put(exclusiveEndOffset, endPoint);
            }
        }

        cursor += length;
    }

    private String formatAnnotation(int offset, String annotationMsg) {
        Integer endOffset = annotatations.higherKey(offset);
        return formatAnnotation(offset, endOffset, annotationMsg);
    }

    private String formatAnnotation(int offset, Integer endOffset, String annotationMsg) {
        if (endOffset != null) {
            return String.format("[0x%x, 0x%x) \"%s\"", offset, endOffset, annotationMsg);
        } else {
            return String.format("[0x%x, ) \"%s\"", offset, annotationMsg);
        }
    }

    public void indent() {
        indentLevel++;
    }

    public void deindent() {
        indentLevel--;
        if (indentLevel < 0) {
            indentLevel = 0;
        }
    }

    public int getCursor() {
        return cursor;
    }

    private static class AnnotationEndpoint {
        /** Annotations that are associated with a specific point between bytes */
        @Nonnull
        public final List<AnnotationItem> pointAnnotations = Lists.newArrayList();
        /** Annotations that are associated with a range of bytes */
        @Nullable
        public AnnotationItem rangeAnnotation = null;
    }

    private static class AnnotationItem {
        public final int indentLevel;
        public final String annotation;

        public AnnotationItem(int  indentLevel, String annotation) {
            this.indentLevel = indentLevel;
            this.annotation = annotation;
        }
    }

    /**
     * Gets the width of the right side containing the annotations
     * @return
     */
    public int getAnnotationWidth() {
        int leftWidth = 8 + (hexCols * 2) + (hexCols / 2);

        return outputWidth - leftWidth;
    }

    /**
     * Writes the annotated content of this instance to the given writer.
     *
     * @param out non-null; where to write to
     */
    public void writeAnnotations(Writer out, byte[] data) throws IOException {
        int rightWidth = getAnnotationWidth();
        int leftWidth = outputWidth - rightWidth - 1;

        String padding = Strings.repeat(" ", 1000);

        TwoColumnOutput twoc = new TwoColumnOutput(out, leftWidth, rightWidth, "|");

        Integer[] keys = new Integer[annotatations.size()];
        keys = annotatations.keySet().toArray(keys);

        AnnotationEndpoint[] values = new AnnotationEndpoint[annotatations.size()];
        values = annotatations.values().toArray(values);

        for (int i=0; i<keys.length-1; i++) {
            int rangeStart = keys[i];
            int rangeEnd = keys[i+1];

            AnnotationEndpoint annotations = values[i];

            for (AnnotationItem pointAnnotation: annotations.pointAnnotations) {
                String paddingSub = padding.substring(0, pointAnnotation.indentLevel*2);
                twoc.write("", paddingSub + pointAnnotation.annotation);
            }

            String right;
            AnnotationItem rangeAnnotation = annotations.rangeAnnotation;
            if (rangeAnnotation != null) {
                right = padding.substring(0, rangeAnnotation.indentLevel*2);
                right += rangeAnnotation.annotation;
            } else {
                right = "";
            }

            String left = Hex.dump(data, rangeStart, rangeEnd - rangeStart, rangeStart, hexCols, 6);

            twoc.write(left, right);
        }

        int lastKey = keys[keys.length-1];
        if (lastKey < data.length) {
            String left = Hex.dump(data, lastKey, data.length - lastKey, lastKey, hexCols, 6);
            twoc.write(left, "");
        }
    }
}