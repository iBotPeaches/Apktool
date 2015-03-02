/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver
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

package org.jf.util;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.IntBuffer;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 * This class handles the complexities of translating a class name into a file name. i.e. dealing with case insensitive
 * file systems, windows reserved filenames, class names with extremely long package/class elements, etc.
 *
 * The types of transformations this class does include:
 * - append a '#123' style numeric suffix if 2 physical representations collide
 * - replace some number of characters in the middle with a '#' character name if an individual path element is too long
 * - append a '#' if an individual path element would otherwise be considered a reserved filename
 */
public class ClassFileNameHandler {
    private static final int MAX_FILENAME_LENGTH = 255;
    // How many characters to reserve in the physical filename for numeric suffixes
    // Dex files can currently only have 64k classes, so 5 digits plus 1 for an '#' should
    // be sufficient to handle the case when every class has a conflicting name
    private static final int NUMERIC_SUFFIX_RESERVE = 6;

    private final int NO_VALUE = -1;
    private final int CASE_INSENSITIVE = 0;
    private final int CASE_SENSITIVE = 1;
    private int forcedCaseSensitivity = NO_VALUE;

    private DirectoryEntry top;
    private String fileExtension;
    private boolean modifyWindowsReservedFilenames;

    public ClassFileNameHandler(File path, String fileExtension) {
        this.top = new DirectoryEntry(path);
        this.fileExtension = fileExtension;
        this.modifyWindowsReservedFilenames = isWindows();
    }

    // for testing
    public ClassFileNameHandler(File path, String fileExtension, boolean caseSensitive,
                                boolean modifyWindowsReservedFilenames) {
        this.top = new DirectoryEntry(path);
        this.fileExtension = fileExtension;
        this.forcedCaseSensitivity = caseSensitive?CASE_SENSITIVE:CASE_INSENSITIVE;
        this.modifyWindowsReservedFilenames = modifyWindowsReservedFilenames;
    }

    private int getMaxFilenameLength() {
        return MAX_FILENAME_LENGTH - NUMERIC_SUFFIX_RESERVE;
    }

    public File getUniqueFilenameForClass(String className) {
        //class names should be passed in the normal dalvik style, with a leading L, a trailing ;, and using
        //'/' as a separator.
        if (className.charAt(0) != 'L' || className.charAt(className.length()-1) != ';') {
            throw new RuntimeException("Not a valid dalvik class name");
        }

        int packageElementCount = 1;
        for (int i=1; i<className.length()-1; i++) {
            if (className.charAt(i) == '/') {
                packageElementCount++;
            }
        }

        String[] packageElements = new String[packageElementCount];
        int elementIndex = 0;
        int elementStart = 1;
        for (int i=1; i<className.length()-1; i++) {
            if (className.charAt(i) == '/') {
                //if the first char after the initial L is a '/', or if there are
                //two consecutive '/'
                if (i-elementStart==0) {
                    throw new RuntimeException("Not a valid dalvik class name");
                }

                packageElements[elementIndex++] = className.substring(elementStart, i);
                elementStart = ++i;
            }
        }

        //at this point, we have added all the package elements to packageElements, but still need to add
        //the final class name. elementStart should point to the beginning of the class name

        //this will be true if the class ends in a '/', i.e. Lsome/package/className/;
        if (elementStart >= className.length()-1) {
            throw new RuntimeException("Not a valid dalvik class name");
        }

        packageElements[elementIndex] = className.substring(elementStart, className.length()-1);

        return addUniqueChild(top, packageElements, 0);
    }

    @Nonnull
    private File addUniqueChild(@Nonnull DirectoryEntry parent, @Nonnull String[] packageElements,
                                int packageElementIndex) {
        if (packageElementIndex == packageElements.length - 1) {
            FileEntry fileEntry = new FileEntry(parent, packageElements[packageElementIndex] + fileExtension);
            parent.addChild(fileEntry);

            String physicalName = fileEntry.getPhysicalName();

            // the physical name should be set when adding it as a child to the parent
            assert  physicalName != null;

            return new File(parent.file, physicalName);
        } else {
            DirectoryEntry directoryEntry = new DirectoryEntry(parent, packageElements[packageElementIndex]);
            directoryEntry = (DirectoryEntry)parent.addChild(directoryEntry);
            return addUniqueChild(directoryEntry, packageElements, packageElementIndex+1);
        }
    }

    private static int utf8Length(String str) {
        int utf8Length = 0;
        int i=0;
        while (i<str.length()) {
            int c = str.codePointAt(i);
            utf8Length += utf8Length(c);
            i += Character.charCount(c);
        }
        return utf8Length;
    }

    private static int utf8Length(int codePoint) {
        if (codePoint < 0x80) {
            return 1;
        } else if (codePoint < 0x800) {
            return 2;
        } else if (codePoint < 0x10000) {
            return 3;
        } else {
            return 4;
        }
    }

    /**
     * Shortens an individual file/directory name, removing the necessary number of code points
     * from the middle of the string such that the utf-8 encoding of the string is at least
     * bytesToRemove bytes shorter than the original.
     *
     * The removed codePoints in the middle of the string will be replaced with a # character.
     */
    @Nonnull
    static String shortenPathComponent(@Nonnull String pathComponent, int bytesToRemove) {
        // We replace the removed part with a #, so we need to remove 1 extra char
        bytesToRemove++;

        int[] codePoints;
        try {
            IntBuffer intBuffer = ByteBuffer.wrap(pathComponent.getBytes("UTF-32BE")).asIntBuffer();
            codePoints = new int[intBuffer.limit()];
            intBuffer.get(codePoints);
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }

        int midPoint = codePoints.length/2;

        int firstEnd = midPoint; // exclusive
        int secondStart = midPoint+1; // inclusive
        int bytesRemoved = utf8Length(codePoints[midPoint]);

        // if we have an even number of codepoints, start by removing both middle characters,
        // unless just removing the first already removes enough bytes
        if (((codePoints.length % 2) == 0) && bytesRemoved < bytesToRemove) {
            bytesRemoved += utf8Length(codePoints[secondStart]);
            secondStart++;
        }

        while ((bytesRemoved < bytesToRemove) &&
                (firstEnd > 0 || secondStart < codePoints.length)) {
            if (firstEnd > 0) {
                firstEnd--;
                bytesRemoved += utf8Length(codePoints[firstEnd]);
            }

            if (bytesRemoved < bytesToRemove && secondStart < codePoints.length) {
                bytesRemoved += utf8Length(codePoints[secondStart]);
                secondStart++;
            }
        }

        StringBuilder sb = new StringBuilder();
        for (int i=0; i<firstEnd; i++) {
            sb.appendCodePoint(codePoints[i]);
        }
        sb.append('#');
        for (int i=secondStart; i<codePoints.length; i++) {
            sb.appendCodePoint(codePoints[i]);
        }

        return sb.toString();
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").startsWith("Windows");
    }

    private static Pattern reservedFileNameRegex = Pattern.compile("^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(\\..*)?$",
            Pattern.CASE_INSENSITIVE);
    private static boolean isReservedFileName(String className) {
        return reservedFileNameRegex.matcher(className).matches();
    }

    private abstract class FileSystemEntry {
        @Nullable public final DirectoryEntry parent;
        @Nonnull public final String logicalName;
        @Nullable protected String physicalName = null;

        private FileSystemEntry(@Nullable DirectoryEntry parent, @Nonnull String logicalName) {
            this.parent = parent;
            this.logicalName = logicalName;
        }

        @Nonnull public String getNormalizedName(boolean preserveCase) {
            String elementName = logicalName;
            if (!preserveCase && parent != null && !parent.isCaseSensitive()) {
                elementName = elementName.toLowerCase();
            }

            if (modifyWindowsReservedFilenames && isReservedFileName(elementName)) {
                elementName = addSuffixBeforeExtension(elementName, "#");
            }

            int utf8Length = utf8Length(elementName);
            if (utf8Length > getMaxFilenameLength()) {
                elementName = shortenPathComponent(elementName, utf8Length - getMaxFilenameLength());
            }
            return elementName;
        }

        @Nullable
        public String getPhysicalName() {
            return physicalName;
        }

        public void setSuffix(int suffix) {
            if (suffix < 0 || suffix > 99999) {
                throw new IllegalArgumentException("suffix must be in [0, 100000)");
            }

            if (this.physicalName != null) {
                throw new IllegalStateException("The suffix can only be set once");
            }
            this.physicalName = makePhysicalName(suffix);
        }

        protected abstract String makePhysicalName(int suffix);
    }

    private class DirectoryEntry extends FileSystemEntry {
        @Nullable private File file = null;
        private int caseSensitivity = forcedCaseSensitivity;

        // maps a normalized (but not suffixed) entry name to 1 or more FileSystemEntries.
        // Each FileSystemEntry asociated with a normalized entry name must have a distinct
        // physical name
        private final Multimap<String, FileSystemEntry> children = ArrayListMultimap.create();

        public DirectoryEntry(@Nonnull File path) {
            super(null, path.getName());
            file = path;
            physicalName = file.getName();
        }

        public DirectoryEntry(@Nullable DirectoryEntry parent, @Nonnull String logicalName) {
            super(parent, logicalName);
        }

        public synchronized FileSystemEntry addChild(FileSystemEntry entry) {
            String normalizedChildName = entry.getNormalizedName(false);
            Collection<FileSystemEntry> entries = children.get(normalizedChildName);
            if (entry instanceof DirectoryEntry) {
                for (FileSystemEntry childEntry: entries) {
                    if (childEntry.logicalName.equals(entry.logicalName)) {
                        return childEntry;
                    }
                }
            }
            entry.setSuffix(entries.size());
            entries.add(entry);
            return entry;
        }

        @Override
        protected String makePhysicalName(int suffix) {
            if (suffix > 0) {
                return getNormalizedName(true) + "." + Integer.toString(suffix);
            }
            return getNormalizedName(true);
        }

        @Override
        public void setSuffix(int suffix) {
            super.setSuffix(suffix);
            String physicalName = getPhysicalName();
            if (parent != null && physicalName != null) {
                file = new File(parent.file, physicalName);
            }
        }

        protected boolean isCaseSensitive() {
            if (getPhysicalName() == null || file == null) {
                throw new IllegalStateException("Must call setSuffix() first");
            }

            if (caseSensitivity != NO_VALUE) {
                return caseSensitivity == CASE_SENSITIVE;
            }

            File path = file;
            if (path.exists() && path.isFile()) {
                if (!path.delete()) {
                    throw new ExceptionWithContext("Can't delete %s to make it into a directory",
                            path.getAbsolutePath());
                }
            }

            if (!path.exists() && !path.mkdirs()) {
                throw new ExceptionWithContext("Couldn't create directory %s", path.getAbsolutePath());
            }

            try {
                boolean result = testCaseSensitivity(path);
                caseSensitivity = result?CASE_SENSITIVE:CASE_INSENSITIVE;
                return result;
            } catch (IOException ex) {
                return false;
            }
        }

        private boolean testCaseSensitivity(File path) throws IOException {
            int num = 1;
            File f, f2;
            do {
                f = new File(path, "test." + num);
                f2 = new File(path, "TEST." + num++);
            } while(f.exists() || f2.exists());

            try {
                try {
                    FileWriter writer = new FileWriter(f);
                    writer.write("test");
                    writer.flush();
                    writer.close();
                } catch (IOException ex) {
                    try {f.delete();} catch (Exception ex2) {}
                    throw ex;
                }

                if (f2.exists()) {
                    return false;
                }

                if (f2.createNewFile()) {
                    return true;
                }

                //the above 2 tests should catch almost all cases. But maybe there was a failure while creating f2
                //that isn't related to case sensitivity. Let's see if we can open the file we just created using
                //f2
                try {
                    CharBuffer buf = CharBuffer.allocate(32);
                    FileReader reader = new FileReader(f2);

                    while (reader.read(buf) != -1 && buf.length() < 4);
                    if (buf.length() == 4 && buf.toString().equals("test")) {
                        return false;
                    } else {
                        //we probably shouldn't get here. If the filesystem was case-sensetive, creating a new
                        //FileReader should have thrown a FileNotFoundException. Otherwise, we should have opened
                        //the file and read in the string "test". It's remotely possible that someone else modified
                        //the file after we created it. Let's be safe and return false here as well
                        assert(false);
                        return false;
                    }
                } catch (FileNotFoundException ex) {
                    return true;
                }
            } finally {
                try { f.delete(); } catch (Exception ex) {}
                try { f2.delete(); } catch (Exception ex) {}
            }
        }
    }

    private class FileEntry extends FileSystemEntry {
        private FileEntry(@Nullable DirectoryEntry parent, @Nonnull String logicalName) {
            super(parent, logicalName);
        }

        @Override
        protected String makePhysicalName(int suffix) {
            if (suffix > 0) {
                return addSuffixBeforeExtension(getNormalizedName(true), '.' + Integer.toString(suffix));
            }
            return getNormalizedName(true);
        }
    }

    private static String addSuffixBeforeExtension(String pathElement, String suffix) {
        int extensionStart = pathElement.lastIndexOf('.');

        StringBuilder newName = new StringBuilder(pathElement.length() + suffix.length() + 1);
        if (extensionStart < 0) {
            newName.append(pathElement);
            newName.append(suffix);
        } else {
            newName.append(pathElement.subSequence(0, extensionStart));
            newName.append(suffix);
            newName.append(pathElement.subSequence(extensionStart, pathElement.length()));
        }
        return newName.toString();
    }
}
