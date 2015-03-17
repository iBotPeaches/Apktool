/*
 * Copyright 2014, Google Inc.
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

package org.jf.dexlib2.writer;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import junit.framework.Assert;
import org.jf.dexlib2.AnnotationVisibility;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.AnnotationElement;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.value.AnnotationEncodedValue;
import org.jf.dexlib2.immutable.ImmutableAnnotation;
import org.jf.dexlib2.immutable.ImmutableAnnotationElement;
import org.jf.dexlib2.immutable.ImmutableClassDef;
import org.jf.dexlib2.immutable.ImmutableDexFile;
import org.jf.dexlib2.immutable.value.ImmutableAnnotationEncodedValue;
import org.jf.dexlib2.immutable.value.ImmutableNullEncodedValue;
import org.jf.dexlib2.writer.io.MemoryDataStore;
import org.jf.dexlib2.writer.pool.DexPool;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class DexWriterTest {
    @Test
    public void testAnnotationElementOrder() {
        // Elements are out of order wrt to the element name
        ImmutableSet<ImmutableAnnotationElement> elements =
                ImmutableSet.of(new ImmutableAnnotationElement("zabaglione", ImmutableNullEncodedValue.INSTANCE),
                        new ImmutableAnnotationElement("blah", ImmutableNullEncodedValue.INSTANCE));

        ImmutableAnnotation annotation = new ImmutableAnnotation(AnnotationVisibility.RUNTIME,
                "Lorg/test/anno;", elements);

        ImmutableClassDef classDef = new ImmutableClassDef("Lorg/test/blah;",
                0, "Ljava/lang/Object;", null, null, ImmutableSet.of(annotation), null, null);

        MemoryDataStore dataStore = new MemoryDataStore();

        try {
            DexPool.writeTo(dataStore, new ImmutableDexFile(ImmutableSet.of(classDef)));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        DexBackedDexFile dexFile = new DexBackedDexFile(new Opcodes(15, false), dataStore.getData());
        ClassDef dbClassDef = Iterables.getFirst(dexFile.getClasses(), null);
        Assert.assertNotNull(dbClassDef);
        Annotation dbAnnotation = Iterables.getFirst(dbClassDef.getAnnotations(), null);
        Assert.assertNotNull(dbAnnotation);
        List<AnnotationElement> dbElements = Lists.newArrayList(dbAnnotation.getElements());

        // Ensure that the elements were written out in sorted order
        Assert.assertEquals(2, dbElements.size());
        Assert.assertEquals("blah", dbElements.get(0).getName());
        Assert.assertEquals("zabaglione", dbElements.get(1).getName());
    }

    @Test
    public void testEncodedAnnotationElementOrder() {
        // Elements are out of order wrt to the element name
        ImmutableSet<ImmutableAnnotationElement> encodedElements =
                ImmutableSet.of(new ImmutableAnnotationElement("zabaglione", ImmutableNullEncodedValue.INSTANCE),
                        new ImmutableAnnotationElement("blah", ImmutableNullEncodedValue.INSTANCE));

        ImmutableAnnotationEncodedValue encodedAnnotations =
                new ImmutableAnnotationEncodedValue("Lan/encoded/annotation", encodedElements);

        ImmutableSet<ImmutableAnnotationElement> elements =
                ImmutableSet.of(new ImmutableAnnotationElement("encoded_annotation", encodedAnnotations));

        ImmutableAnnotation annotation = new ImmutableAnnotation(AnnotationVisibility.RUNTIME,
                "Lorg/test/anno;", elements);

        ImmutableClassDef classDef = new ImmutableClassDef("Lorg/test/blah;",
                0, "Ljava/lang/Object;", null, null, ImmutableSet.of(annotation), null, null);

        MemoryDataStore dataStore = new MemoryDataStore();

        try {
            DexPool.writeTo(dataStore, new ImmutableDexFile(ImmutableSet.of(classDef)));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        DexBackedDexFile dexFile = new DexBackedDexFile(new Opcodes(15, false), dataStore.getData());
        ClassDef dbClassDef = Iterables.getFirst(dexFile.getClasses(), null);
        Assert.assertNotNull(dbClassDef);
        Annotation dbAnnotation = Iterables.getFirst(dbClassDef.getAnnotations(), null);
        Assert.assertNotNull(dbAnnotation);

        AnnotationElement element = Iterables.getFirst(dbAnnotation.getElements(), null);
        AnnotationEncodedValue dbAnnotationEncodedValue = (AnnotationEncodedValue)element.getValue();

        List<AnnotationElement> dbElements = Lists.newArrayList(dbAnnotationEncodedValue.getElements());

        // Ensure that the elements were written out in sorted order
        Assert.assertEquals(2, dbElements.size());
        Assert.assertEquals("blah", dbElements.get(0).getName());
        Assert.assertEquals("zabaglione", dbElements.get(1).getName());
    }
}
