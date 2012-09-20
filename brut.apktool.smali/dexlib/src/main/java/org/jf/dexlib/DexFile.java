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

import org.jf.dexlib.Util.*;

import java.io.*;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.zip.Adler32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * <h3>Main use cases</h3>
 *
 * <p>These are the main use cases that drove the design of this library</p>
 *
 * <ol>
 * <li><p><b>Annotate an existing dex file</b> - In this case, the intent is to document the structure of
 *    an existing dex file. We want to be able to read in the dex file, and then write out a dex file
 *    that is exactly the same (while adding annotation information to an AnnotatedOutput object)</p></li>
 *
 * <li><p><b>Canonicalize an existing dex file</b> - In this case, the intent is to rewrite an existing dex file
 *    so that it is in a canonical form. There is a certain amount of leeway in how various types of
 *    tems in a dex file are ordered or represented. It is sometimes useful to be able to easily
 *    compare a disassebled and reassembled dex file with the original dex file. If both dex-files are
 *    written canonically, they "should" match exactly, barring any explicit changes to the reassembled
 *    file.</p>
 *
 *    <p>Currently, there are a couple of pieces of information that probably won't match exactly
 *    <ul>
 *    <li>the order of exception handlers in the <code>EncodedCatchHandlerList</code> for a method</li>
 *    <li>the ordering of some of the debug info in the <code>{@link org.jf.dexlib.DebugInfoItem}</code> for a method</li>
 *    </ul></p>
 *
 *
 *    <p>Note that the above discrepancies should typically only be "intra-item" differences. They
 *    shouldn't change the size of the item, or affect how anything else is placed or laid out</p></li>
 *
 * <li><p><b>Creating a dex file from scratch</b> - In this case, a blank dex file is created and then classes
 *    are added to it incrementally by calling the {@link org.jf.dexlib.Section#intern intern} method of
 *    {@link DexFile#ClassDefsSection}, which will add all the information necessary to represent the given
 *    class. For example, when assembling a dex file from a set of assembly text files.</p>
 *
 *    <p>In this case, we can choose to write  the dex file in a canonical form or not. It is somewhat
 *    slower to write it in a canonical format, due to the extra sorting and calculations that are
 *    required.</p></li>
 *
 *
 * <li><p><b>Reading in the dex file</b> - In this case, the intent is to read in a dex file and expose all the
 *    data to the calling application. For example, when disassembling a dex file into a text based
 *    assembly format, or doing other misc processing of the dex file.</p></li>
 *
 *
 * <h3>Other use cases</h3>
 *
 * <p>These are other use cases that are possible, but did not drive the design of the library.
 * No effort was made to test these use cases or ensure that they work. Some of these could
 * probably be better achieved with a disassemble - modify - reassemble type process, using
 * smali/baksmali or another assembler/disassembler pair that are compatible with each other</p>
 *
 * <ul>
 * <li>deleting classes/methods/etc. from a dex file</li>
 * <li>merging 2 dex files</li>
 * <li>splitting a dex file</li>
 * <li>moving classes from 1 dex file to another</li>
 * <li>removing the debug information from a dex file</li>
 * <li>obfustication of a dex file</li>
 * </ul>
 */
public class DexFile
{
    /**
     * A mapping from ItemType to the section that contains items of the given type
     */
    private final Section[] sectionsByType;

    /**
     * Ordered lists of the indexed and offsetted sections. The order of these lists specifies the order
     * that the sections will be written in
     */
    private final IndexedSection[] indexedSections;
    private final OffsettedSection[] offsettedSections;

    /**
     * dalvik had a bug where it wrote the registers for certain types of debug info in a signed leb
     * format, instead of an unsigned leb format. There are no negative registers of course, but
     * certain positive values have a different encoding depending on whether they are encoded as
     * an unsigned leb128 or a signed leb128. Specifically, the signed leb128 is 1 byte longer in some cases.
     *
     * This determine whether we should keep any signed registers as signed, or force all register to
     * unsigned. By default we don't keep track of whether they were signed or not, and write them back
     * out as unsigned. This option only has an effect when reading an existing dex file. It has no
     * effect when a dex file is created from scratch
     *
     * The 2 main use-cases in play are
     * 1. Annotate an existing dex file - In this case, preserveSignedRegisters should be false, so that we keep
     * track of any signed registers and write them back out as signed Leb128 values.
     *
     * 2. Canonicalize an existing dex file - In this case, fixRegisters should be true, so that all
     * registers in the debug info are written as unsigned Leb128 values regardless of how they were
     * originally encoded
     */
    private final boolean preserveSignedRegisters;

    /**
     * When true, any instructions in a code item are skipped over instead of being read in. This is useful when
     * you only need the information about the classes and their methods, for example, when loading the BOOTCLASSPATH
     * jars in order to analyze a dex file
     */
    private final boolean skipInstructions;

    /**
     * When true, this prevents any sorting of the items during placement of the dex file. This
     * should *only* be set to true when this dex file was read in from an existing (valid) dex file,
     * and no modifications were made (i.e. no items added or deleted). Otherwise it is likely that
     * an invalid dex file will be generated.
     *
     * This is useful for the first use case (annotating an existing dex file). This ensures the items
     * retain the same order as in the original dex file.
     */
    private boolean inplace = false;

    /**
     * When true, this imposes an full ordering on all the items, to force them into a (possibly
     * arbitrary) canonical order. When false, only the items that the dex format specifies
     * an order for are sorted. The rest of the items are not ordered.
     *
     * This is useful for the second use case (canonicalizing an existing dex file) or possibly for
     * the third use case (creating a dex file from scratch), if there is a need to write the new
     * dex file in a canonical form.
     */
    private boolean sortAllItems = false;

    /**
     * Is this file an odex file? This is only set when reading in an odex file
     */
    private boolean isOdex = false;

    private OdexHeader odexHeader;
    private OdexDependencies odexDependencies;

    private int dataOffset;
    private int dataSize;
    private int fileSize;

    /**
     * A private constructor containing common code to initialize the section maps and lists
     * @param preserveSignedRegisters If true, keep track of any registers in the debug information
     * @param skipInstructions If true, skip the instructions in any code item.
     * that are signed, so they will be written in the same format. See
     * <code>getPreserveSignedRegisters()</code>
     */
    private DexFile(boolean preserveSignedRegisters, boolean skipInstructions) {
        this.preserveSignedRegisters = preserveSignedRegisters;
        this.skipInstructions = skipInstructions;

        sectionsByType = new Section[] {
                StringIdsSection,
                TypeIdsSection,
                ProtoIdsSection,
                FieldIdsSection,
                MethodIdsSection,
                ClassDefsSection,
                TypeListsSection,
                AnnotationSetRefListsSection,
                AnnotationSetsSection,
                ClassDataSection,
                CodeItemsSection,
                AnnotationDirectoriesSection,
                StringDataSection,
                DebugInfoItemsSection,
                AnnotationsSection,
                EncodedArraysSection,
                null,
                null
        };

        indexedSections = new IndexedSection[] {
                StringIdsSection,
                TypeIdsSection,
                ProtoIdsSection,
                FieldIdsSection,
                MethodIdsSection,
                ClassDefsSection
        };

        offsettedSections = new OffsettedSection[] {
                AnnotationSetRefListsSection,
                AnnotationSetsSection,
                CodeItemsSection,
                AnnotationDirectoriesSection,
                TypeListsSection,
                StringDataSection,
                AnnotationsSection,
                EncodedArraysSection,
                ClassDataSection,
                DebugInfoItemsSection
        };
    }


    /**
     * Construct a new DexFile instance by reading in the given dex file.
     * @param file The dex file to read in
     * @throws IOException if an IOException occurs
     */
    public DexFile(String file)
            throws IOException {
        this(new File(file), true, false);
    }

    /**
     * Construct a new DexFile instance by reading in the given dex file,
     * and optionally keep track of any registers in the debug information that are signed,
     * so they will be written in the same format.
     * @param file The dex file to read in
     * @param preserveSignedRegisters If true, keep track of any registers in the debug information
     * that are signed, so they will be written in the same format. See
     * @param skipInstructions If true, skip the instructions in any code item.
     * <code>getPreserveSignedRegisters()</code>
     * @throws IOException if an IOException occurs
     */
    public DexFile(String file, boolean preserveSignedRegisters, boolean skipInstructions)
            throws IOException {
        this(new File(file), preserveSignedRegisters, skipInstructions);
    }

    /**
     * Construct a new DexFile instance by reading in the given dex file.
     * @param file The dex file to read in
     * @throws IOException if an IOException occurs
     */
    public DexFile(File file)
            throws IOException {
        this(file, true, false);
    }

    /**
     * Construct a new DexFile instance by reading in the given dex file,
     * and optionally keep track of any registers in the debug information that are signed,
     * so they will be written in the same format.
     * @param file The dex file to read in
     * @param preserveSignedRegisters If true, keep track of any registers in the debug information
     * that are signed, so they will be written in the same format.
     * @param skipInstructions If true, skip the instructions in any code item.
     * @see #getPreserveSignedRegisters
     * @throws IOException if an IOException occurs
     */
    public DexFile(File file, boolean preserveSignedRegisters, boolean skipInstructions)
            throws IOException {
        this(preserveSignedRegisters, skipInstructions);

        long fileLength;
        byte[] magic = FileUtils.readFile(file, 0, 8);

        InputStream inputStream = null;
        Input in = null;
        ZipFile zipFile = null;

        try {
            //do we have a zip file?
            if (magic[0] == 0x50 && magic[1] == 0x4B) {
                zipFile = new ZipFile(file);
                ZipEntry zipEntry = zipFile.getEntry("classes.dex");
                if (zipEntry == null) {
                    throw new NoClassesDexException("zip file " + file.getName() + " does not contain a classes.dex " +
                            "file");
                }
                fileLength = zipEntry.getSize();
                if (fileLength < 40) {
                    throw new RuntimeException("The classes.dex file in " + file.getName() + " is too small to be a" +
                            " valid dex file");
                } else if (fileLength > Integer.MAX_VALUE) {
                    throw new RuntimeException("The classes.dex file in " + file.getName() + " is too large to read in");
                }
                inputStream = new BufferedInputStream(zipFile.getInputStream(zipEntry));

                inputStream.mark(8);
                for (int i=0; i<8; i++) {
                    magic[i] = (byte)inputStream.read();
                }
                inputStream.reset();
            } else {
                fileLength = file.length();
                if (fileLength < 40) {
                    throw new RuntimeException(file.getName() + " is too small to be a valid dex file");
                }
                if (fileLength < 40) {
                    throw new RuntimeException(file.getName() + " is too small to be a valid dex file");
                } else if (fileLength > Integer.MAX_VALUE) {
                    throw new RuntimeException(file.getName() + " is too large to read in");
                }
                inputStream = new FileInputStream(file);
            }

            byte[] dexMagic, odexMagic;
            boolean isDex = false;
            this.isOdex = false;

            for (int i=0; i<HeaderItem.MAGIC_VALUES.length; i++) {
                byte[] magic_value = HeaderItem.MAGIC_VALUES[i];
                if (Arrays.equals(magic, magic_value)) {
                    isDex = true;
                    break;
                }
            }
            if (!isDex) {
                if (Arrays.equals(magic, OdexHeader.MAGIC_35)) {
                    isOdex = true;
                } else if (Arrays.equals(magic, OdexHeader.MAGIC_36)) {
                    isOdex = true;
                }
            }

            if (isOdex) {
                byte[] odexHeaderBytes = FileUtils.readStream(inputStream, 40);
                Input odexHeaderIn = new ByteArrayInput(odexHeaderBytes);
                odexHeader = new OdexHeader(odexHeaderIn);

                int dependencySkip = odexHeader.depsOffset - odexHeader.dexOffset - odexHeader.dexLength;
                if (dependencySkip < 0) {
                    throw new ExceptionWithContext("Unexpected placement of the odex dependency data");
                }

                if (odexHeader.dexOffset > 40) {
                    FileUtils.readStream(inputStream, odexHeader.dexOffset - 40);
                }

                in = new ByteArrayInput(FileUtils.readStream(inputStream, odexHeader.dexLength));

                if (dependencySkip > 0) {
                    FileUtils.readStream(inputStream, dependencySkip);
                }

                odexDependencies = new OdexDependencies(
                        new ByteArrayInput(FileUtils.readStream(inputStream, odexHeader.depsLength)));
            } else if (isDex) {
                in = new ByteArrayInput(FileUtils.readStream(inputStream, (int)fileLength));
            } else {
                StringBuffer sb = new StringBuffer("bad magic value:");
                for (int i=0; i<8; i++) {
                    sb.append(" ");
                    sb.append(Hex.u1(magic[i]));
                }
                throw new RuntimeException(sb.toString());
            }
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            if (zipFile != null) {
                zipFile.close();
            }
        }

        ReadContext readContext = new ReadContext();

        HeaderItem.readFrom(in, 0, readContext);

        //the map offset was set while reading in the header item
        int mapOffset = readContext.getSectionOffset(ItemType.TYPE_MAP_LIST);

        in.setCursor(mapOffset);
        MapItem.readFrom(in, 0, readContext);

        //the sections are ordered in such a way that the item types
        Section sections[] = new Section[] {
                StringDataSection,
                StringIdsSection,
                TypeIdsSection,
                TypeListsSection,
                ProtoIdsSection,
                FieldIdsSection,
                MethodIdsSection,
                AnnotationsSection,
                AnnotationSetsSection,
                AnnotationSetRefListsSection,
                AnnotationDirectoriesSection,
                DebugInfoItemsSection,
                CodeItemsSection,
                ClassDataSection,
                EncodedArraysSection,
                ClassDefsSection
        };

        for (Section section: sections) {
            if (section == null) {
                continue;
            }

            if (skipInstructions && (section == CodeItemsSection || section == DebugInfoItemsSection)) {
                continue;
            }

            int sectionOffset = readContext.getSectionOffset(section.ItemType);
            if (sectionOffset > 0) {
                int sectionSize = readContext.getSectionSize(section.ItemType);
                in.setCursor(sectionOffset);
                section.readFrom(sectionSize, in, readContext);
            }
        }
    }

    /**
     * Constructs a new, blank dex file. Classes can be added to this dex file by calling
     * the <code>Section.intern()</code> method of <code>ClassDefsSection</code>
     */
    public DexFile() {
        this(true, false);
    }

    /**
     * Get the <code>Section</code> containing items of the same type as the given item
     * @param item Get the <code>Section</code> that contains items of this type
     * @param <T> The specific item subclass - inferred from the passed item
     * @return the <code>Section</code> containing items of the same type as the given item
     */
    public <T extends Item> Section<T> getSectionForItem(T item) {
        return (Section<T>)sectionsByType[item.getItemType().SectionIndex];
    }

    /**
     * Get the <code>Section</code> containing items of the given type
     * @param itemType the type of item
     * @return the <code>Section</code> containing items of the given type
     */
    public Section getSectionForType(ItemType itemType) {
        return sectionsByType[itemType.SectionIndex];
    }

    /**
     * Get a boolean value indicating whether this dex file preserved any signed
     * registers in the debug info as it read the dex file in. By default, the dex file
     * doesn't check whether the registers are encoded as unsigned or signed values.
     *
     * This does *not* affect the actual register value that is read in. The value is
     * read correctly regardless
     *
     * This does affect whether any signed registers will retain the same encoding or be
     * forced to the (correct) unsigned encoding when the dex file is written back out.
     *
     * See the discussion about signed register values in the documentation for
     * <code>DexFile</code>
     * @return a boolean indicating whether this dex file preserved any signed registers
     * as it was read in
     */
    public boolean getPreserveSignedRegisters() {
        return preserveSignedRegisters;
    }

    /**
     * Get a boolean value indicating whether to skip any instructions in a code item while reading in the dex file.
     * This is useful when  you only need the information about the classes and their methods, for example, when
     * loading the BOOTCLASSPATH jars in order to analyze a dex file
     * @return a boolean value indicating whether to skip any instructions in a code item
     */
    public boolean skipInstructions() {
        return skipInstructions;
    }

    /**
     * Get a boolean value indicating whether all items should be placed into a
     * (possibly arbitrary) "canonical" ordering. If false, then only the items
     * that must be ordered per the dex specification are sorted.
     *
     * When true, writing the dex file involves somewhat more overhead
     *
     * If both SortAllItems and Inplace are true, Inplace takes precedence
     * @return a boolean value indicating whether all items should be sorted
     */
    public boolean getSortAllItems() {
        return this.sortAllItems;
    }

    /**
     * Set a boolean value indicating whether all items should be placed into a
     * (possibly arbitrary) "canonical" ordering. If false, then only the items
     * that must be ordered per the dex specification are sorted.
     *
     * When true, writing the dex file involves somewhat more overhead
     *
     * If both SortAllItems and Inplace are true, Inplace takes precedence
     * @param value a boolean value indicating whether all items should be sorted
     */
    public void setSortAllItems(boolean value) {
        this.sortAllItems = value;
    }

    /**
     * @return a boolean value indicating whether this dex file was created by reading in an odex file
     */
    public boolean isOdex() {
        return this.isOdex;
    }

    /**
     * @return an OdexDependencies object that contains the dependencies for this odex, or null if this
     * DexFile represents a dex file instead of an odex file
     */
    public OdexDependencies getOdexDependencies() {
        return odexDependencies;
    }

    /**
     * @return An OdexHeader object containing the information from the odex header in this dex file, or null if there
     * is no odex header
     */
    public OdexHeader getOdexHeader() {
        return odexHeader;
    }

    /**
     * Get a boolean value indicating whether items in this dex file should be
     * written back out "in-place", or whether the normal layout logic should be
     * applied.
     *
     * This should only be used for a dex file that has been read from an existing
     * dex file, and no modifications have been made to the dex file. Otherwise,
     * there is a good chance that the resulting dex file will be invalid due to
     * items that aren't placed correctly
     *
     * If both SortAllItems and Inplace are true, Inplace takes precedence
     * @return a boolean value indicating whether items in this dex file should be
     * written back out in-place.
     */
    public boolean getInplace() {
        return this.inplace;
    }

    /**
     * @return the size of the file, in bytes
     */
    public int getFileSize() {
        return fileSize;
    }

    /**
     * @return the size of the data section, in bytes
     */
    public int getDataSize() {
        return dataSize;
    }

    /**
     * @return the offset where the data section begins
     */
    public int getDataOffset() {
        return dataOffset;
    }

    /**
     * Set a boolean value indicating whether items in this dex file should be
     * written back out "in-place", or whether the normal layout logic should be
     * applied.
     *
     * This should only be used for a dex file that has been read from an existing
     * dex file, and no modifications have been made to the dex file. Otherwise,
     * there is a good chance that the resulting dex file will be invalid due to
     * items that aren't placed correctly
     *
     * If both SortAllItems and Inplace are true, Inplace takes precedence
     * @param value a boolean value indicating whether items in this dex file should be
     * written back out in-place.
     */
    public void setInplace(boolean value) {
        this.inplace = value;
    }

    /**
     * Get an array of Section objects that are sorted by offset.
     * @return an array of Section objects that are sorted by offset.
     */
    protected Section[] getOrderedSections() {
        int sectionCount = 0;

        for (Section section: sectionsByType) {
            if (section != null && section.getItems().size() > 0) {
                sectionCount++;
            }
        }

        Section[] sections = new Section[sectionCount];
        sectionCount = 0;
        for (Section section: sectionsByType) {
            if (section != null && section.getItems().size() > 0) {
                sections[sectionCount++] = section;
            }
        }

        Arrays.sort(sections, new Comparator<Section>() {
            public int compare(Section a, Section b) {
                return a.getOffset() - b.getOffset();
            }
        });

        return sections;
    }

    /**
     * This method should be called before writing a dex file. It sorts the sections
     * as needed or as indicated by <code>getSortAllItems()</code> and <code>getInplace()</code>,
     * and then performs a pass through all of the items, finalizing the position (i.e.
     * index and/or offset) of each item in the dex file.
     *
     * This step is needed primarily so that the indexes and offsets of all indexed and
     * offsetted items are available when writing references to those items elsewhere.
     */
    public void place() {
        int offset = HeaderItem.placeAt(0, 0);

        int sectionsPosition = 0;
        Section[] sections;
        if (this.inplace) {
            sections = this.getOrderedSections();
        } else {
            sections = new Section[indexedSections.length + offsettedSections.length];
            System.arraycopy(indexedSections, 0, sections, 0, indexedSections.length);
            System.arraycopy(offsettedSections, 0, sections, indexedSections.length,  offsettedSections.length);
        }

        while (sectionsPosition < sections.length && sections[sectionsPosition].ItemType.isIndexedItem()) {
            Section section = sections[sectionsPosition];
            if (!this.inplace) {
                section.sortSection();
            }

            offset = section.placeAt(offset);

            sectionsPosition++;
        }

        dataOffset = offset;

        while (sectionsPosition < sections.length) {
            Section section = sections[sectionsPosition];
            if (this.sortAllItems && !this.inplace) {
                section.sortSection();
            }
            offset = section.placeAt(offset);

            sectionsPosition++;
        }

        offset = AlignmentUtils.alignOffset(offset, ItemType.TYPE_MAP_LIST.ItemAlignment);
        offset = MapItem.placeAt(offset, 0);

        fileSize = offset;
        dataSize = offset - dataOffset;
    }

    /**
     * Writes the dex file to the give <code>AnnotatedOutput</code> object. If
     * <code>out.Annotates()</code> is true, then annotations that document the format
     * of the dex file are written.
     *
     * You must call <code>place()</code> on this dex file, before calling this method
     * @param out the AnnotatedOutput object to write the dex file and annotations to
     *
     * After calling this method, you should call <code>calcSignature()</code> and
     * then <code>calcChecksum()</code> on the resulting byte array, to calculate the
     * signature and checksum in the header
     */
    public void writeTo(AnnotatedOutput out) {

        out.annotate(0, "-----------------------------");
        out.annotate(0, "header item");
        out.annotate(0, "-----------------------------");
        out.annotate(0, " ");
        HeaderItem.writeTo(out);

        out.annotate(0, " ");

        int sectionsPosition = 0;
        Section[] sections;
        if (this.inplace) {
            sections = this.getOrderedSections();
        } else {
            sections = new Section[indexedSections.length + offsettedSections.length];
            System.arraycopy(indexedSections, 0, sections, 0, indexedSections.length);
            System.arraycopy(offsettedSections, 0, sections, indexedSections.length,  offsettedSections.length);
        }

        while (sectionsPosition < sections.length) {
            sections[sectionsPosition].writeTo(out);
            sectionsPosition++;
        }

        out.alignTo(MapItem.getItemType().ItemAlignment);

        out.annotate(0, " ");
        out.annotate(0, "-----------------------------");
        out.annotate(0, "map item");
        out.annotate(0, "-----------------------------");
        out.annotate(0, " ");
        MapItem.writeTo(out);
    }

    public final HeaderItem HeaderItem = new HeaderItem(this);
    public final MapItem MapItem = new MapItem(this);

    /**
     * The <code>IndexedSection</code> containing <code>StringIdItem</code> items
     */
    public final IndexedSection<StringIdItem> StringIdsSection =
            new IndexedSection<StringIdItem>(this, ItemType.TYPE_STRING_ID_ITEM);

    /**
     * The <code>IndexedSection</code> containing <code>TypeIdItem</code> items
     */
    public final IndexedSection<TypeIdItem> TypeIdsSection =
            new IndexedSection<TypeIdItem>(this, ItemType.TYPE_TYPE_ID_ITEM);

    /**
     * The <code>IndexedSection</code> containing <code>ProtoIdItem</code> items
     */
    public final IndexedSection<ProtoIdItem> ProtoIdsSection =
            new IndexedSection<ProtoIdItem>(this, ItemType.TYPE_PROTO_ID_ITEM);

    /**
     * The <code>IndexedSection</code> containing <code>FieldIdItem</code> items
     */
    public final IndexedSection<FieldIdItem> FieldIdsSection =
            new IndexedSection<FieldIdItem>(this, ItemType.TYPE_FIELD_ID_ITEM);

    /**
     * The <code>IndexedSection</code> containing <code>MethodIdItem</code> items
     */
    public final IndexedSection<MethodIdItem> MethodIdsSection =
            new IndexedSection<MethodIdItem>(this, ItemType.TYPE_METHOD_ID_ITEM);

    /**
     * The <code>IndexedSection</code> containing <code>ClassDefItem</code> items
     */
    public final IndexedSection<ClassDefItem> ClassDefsSection =
            new IndexedSection<ClassDefItem>(this, ItemType.TYPE_CLASS_DEF_ITEM) {

         public int placeAt(int offset) {
            if (DexFile.this.getInplace()) {
                return super.placeAt(offset);
            }

            int ret = ClassDefItem.placeClassDefItems(this, offset);

            Collections.sort(this.items);

            this.offset = items.get(0).getOffset();
            return ret;
        }

        protected void sortSection() {
            // Do nothing. Sorting is handled by ClassDefItem.ClassDefPlacer, during placement
        }
    };

    /**
     * The <code>OffsettedSection</code> containing <code>TypeListItem</code> items
     */
    public final OffsettedSection<TypeListItem> TypeListsSection =
            new OffsettedSection<TypeListItem>(this, ItemType.TYPE_TYPE_LIST);

    /**
     * The <code>OffsettedSection</code> containing <code>AnnotationSetRefList</code> items
     */
    public final OffsettedSection<AnnotationSetRefList> AnnotationSetRefListsSection =
            new OffsettedSection<AnnotationSetRefList>(this, ItemType.TYPE_ANNOTATION_SET_REF_LIST);

    /**
     * The <code>OffsettedSection</code> containing <code>AnnotationSetItem</code> items
     */
    public final OffsettedSection<AnnotationSetItem> AnnotationSetsSection =
            new OffsettedSection<AnnotationSetItem>(this, ItemType.TYPE_ANNOTATION_SET_ITEM);

    /**
     * The <code>OffsettedSection</code> containing <code>ClassDataItem</code> items
     */
    public final OffsettedSection<ClassDataItem> ClassDataSection =
            new OffsettedSection<ClassDataItem>(this, ItemType.TYPE_CLASS_DATA_ITEM);

    /**
     * The <code>OffsettedSection</code> containing <code>CodeItem</code> items
     */
    public final OffsettedSection<CodeItem> CodeItemsSection =
            new OffsettedSection<CodeItem>(this, ItemType.TYPE_CODE_ITEM);

    /**
     * The <code>OffsettedSection</code> containing <code>StringDataItem</code> items
     */
    public final OffsettedSection<StringDataItem> StringDataSection =
            new OffsettedSection<StringDataItem>(this, ItemType.TYPE_STRING_DATA_ITEM);

    /**
     * The <code>OffsettedSection</code> containing <code>DebugInfoItem</code> items
     */
    public final OffsettedSection<DebugInfoItem> DebugInfoItemsSection =
            new OffsettedSection<DebugInfoItem>(this, ItemType.TYPE_DEBUG_INFO_ITEM);

    /**
     * The <code>OffsettedSection</code> containing <code>AnnotationItem</code> items
     */
    public final OffsettedSection<AnnotationItem> AnnotationsSection =
            new OffsettedSection<AnnotationItem>(this, ItemType.TYPE_ANNOTATION_ITEM);

    /**
     * The <code>OffsettedSection</code> containing <code>EncodedArrayItem</code> items
     */
    public final OffsettedSection<EncodedArrayItem> EncodedArraysSection =
            new OffsettedSection<EncodedArrayItem>(this, ItemType.TYPE_ENCODED_ARRAY_ITEM);

    /**
     * The <code>OffsettedSection</code> containing <code>AnnotationDirectoryItem</code> items
     */
    public final OffsettedSection<AnnotationDirectoryItem> AnnotationDirectoriesSection =
            new OffsettedSection<AnnotationDirectoryItem>(this, ItemType.TYPE_ANNOTATIONS_DIRECTORY_ITEM);


    /**
     * Calculates the signature for the dex file in the given byte array,
     * and then writes the signature to the appropriate location in the header
     * containing in the array
     *
     * @param bytes non-null; the bytes of the file
     */
    public static void calcSignature(byte[] bytes) {
        MessageDigest md;

        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }

        md.update(bytes, 32, bytes.length - 32);

        try {
            int amt = md.digest(bytes, 12, 20);
            if (amt != 20) {
                throw new RuntimeException("unexpected digest write: " + amt +
                                           " bytes");
            }
        } catch (DigestException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Calculates the checksum for the <code>.dex</code> file in the
     * given array, and modify the array to contain it.
     *
     * @param bytes non-null; the bytes of the file
     */
    public static void calcChecksum(byte[] bytes) {
        Adler32 a32 = new Adler32();

        a32.update(bytes, 12, bytes.length - 12);

        int sum = (int) a32.getValue();

        bytes[8]  = (byte) sum;
        bytes[9]  = (byte) (sum >> 8);
        bytes[10] = (byte) (sum >> 16);
        bytes[11] = (byte) (sum >> 24);
    }

    public static class NoClassesDexException extends ExceptionWithContext {
        public NoClassesDexException(String message) {
            super(message);
        }
    }
}