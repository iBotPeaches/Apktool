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

import ds.tree.RadixTree;
import ds.tree.RadixTreeImpl;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.CharBuffer;
import java.util.regex.Pattern;

/**
 * This class checks for case-insensitive file systems, and generates file names based on a given class name, that are
 * guaranteed to be unique. When "colliding" class names are found, it appends a numeric identifier to the end of the
 * class name to distinguish it from another class with a name that differes only by case. i.e. a.smali and a_2.smali
 */
public class ClassFileNameHandler {
    // we leave an extra 10 characters to allow for a numeric suffix to be added, if it's needed
    private static final int MAX_FILENAME_LENGTH = 245;

    private PackageNameEntry top;
    private String fileExtension;
    private boolean modifyWindowsReservedFilenames;

    public ClassFileNameHandler(File path, String fileExtension) {
        this.top = new PackageNameEntry(path);
        this.fileExtension = fileExtension;
        this.modifyWindowsReservedFilenames = testForWindowsReservedFileNames(path);
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

        String packageElement;
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

                packageElement = className.substring(elementStart, i);

                if (modifyWindowsReservedFilenames && isReservedFileName(packageElement)) {
                    packageElement += "#";
                }

                if (packageElement.length() > MAX_FILENAME_LENGTH) {
                    packageElement = shortenPathComponent(packageElement, MAX_FILENAME_LENGTH);
                }

                packageElements[elementIndex++] = packageElement;
                elementStart = ++i;
            }
        }

        //at this point, we have added all the package elements to packageElements, but still need to add
        //the final class name. elementStart should point to the beginning of the class name

        //this will be true if the class ends in a '/', i.e. Lsome/package/className/;
        if (elementStart >= className.length()-1) {
            throw new RuntimeException("Not a valid dalvik class name");
        }

        packageElement = className.substring(elementStart, className.length()-1);
        if (modifyWindowsReservedFilenames && isReservedFileName(packageElement)) {
            packageElement += "#";
        }

        if ((packageElement.length() + fileExtension.length()) > MAX_FILENAME_LENGTH) {
            packageElement = shortenPathComponent(packageElement, MAX_FILENAME_LENGTH - fileExtension.length());
        }

        packageElements[elementIndex] = packageElement;

        return top.addUniqueChild(packageElements, 0);
    }

    @Nonnull
    static String shortenPathComponent(@Nonnull String pathComponent, int maxLength) {
        int toRemove = pathComponent.length() - maxLength + 1;

        int firstIndex = (pathComponent.length()/2) - (toRemove/2);
        return pathComponent.substring(0, firstIndex) + "#" + pathComponent.substring(firstIndex+toRemove);
    }

    private static boolean testForWindowsReservedFileNames(File path) {
        String[] reservedNames = new String[]{"aux", "con", "com1", "com9", "lpt1", "com9"};

        for (String reservedName: reservedNames) {
            File f = new File(path, reservedName + ".smali");
            if (f.exists()) {
                continue;
            }

            try {
                FileWriter writer = new FileWriter(f);
                writer.write("test");
                writer.flush();
                writer.close();
                f.delete(); //doesn't throw IOException
            } catch (IOException ex) {
                //if an exception occured, it's likely that we're on a windows system.
                return true;
            }
        }
        return false;
    }

    private static Pattern reservedFileNameRegex = Pattern.compile("^CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9]$",
            Pattern.CASE_INSENSITIVE);
    private static boolean isReservedFileName(String className) {
        return reservedFileNameRegex.matcher(className).matches();
    }

    private abstract class FileSystemEntry {
        public final File file;

        public FileSystemEntry(File file) {
            this.file = file;
        }

        public abstract File addUniqueChild(String[] pathElements, int pathElementsIndex);

        public FileSystemEntry makeVirtual(File parent) {
            return new VirtualGroupEntry(this, parent);
        }
    }

    private class PackageNameEntry extends FileSystemEntry {
        //this contains the FileSystemEntries for all of this package's children
        //the associated keys are all lowercase
        private RadixTree<FileSystemEntry> children = new RadixTreeImpl<FileSystemEntry>();

        public PackageNameEntry(File parent, String name) {
            super(new File(parent, name));
        }

        public PackageNameEntry(File path) {
            super(path);
        }

        @Override
        public synchronized File addUniqueChild(String[] pathElements, int pathElementsIndex) {
            String elementName;
            String elementNameLower;

            if (pathElementsIndex == pathElements.length - 1) {
                elementName = pathElements[pathElementsIndex];
                elementName += fileExtension;
            } else {
                elementName = pathElements[pathElementsIndex];
            }
            elementNameLower = elementName.toLowerCase();

            FileSystemEntry existingEntry = children.find(elementNameLower);
            if (existingEntry != null) {
                FileSystemEntry virtualEntry = existingEntry;
                //if there is already another entry with the same name but different case, we need to
                //add a virtual group, and then add the existing entry and the new entry to that group
                if (!(existingEntry instanceof VirtualGroupEntry)) {
                    if (existingEntry.file.getName().equals(elementName)) {
                        if (pathElementsIndex == pathElements.length - 1) {
                            return existingEntry.file;
                        } else {
                            return existingEntry.addUniqueChild(pathElements, pathElementsIndex + 1);
                        }
                    } else {
                        virtualEntry = existingEntry.makeVirtual(file);
                        children.replace(elementNameLower, virtualEntry);
                    }
                }

                return virtualEntry.addUniqueChild(pathElements, pathElementsIndex);
            }

            if (pathElementsIndex == pathElements.length - 1) {
                ClassNameEntry classNameEntry = new ClassNameEntry(file, elementName);
                children.insert(elementNameLower, classNameEntry);
                return classNameEntry.file;
            } else {
                PackageNameEntry packageNameEntry = new PackageNameEntry(file, elementName);
                children.insert(elementNameLower, packageNameEntry);
                return packageNameEntry.addUniqueChild(pathElements, pathElementsIndex + 1);
            }
        }
    }

    /**
     * A virtual group that groups together file system entries with the same name, differing only in case
     */
    private class VirtualGroupEntry extends FileSystemEntry {
        //this contains the FileSystemEntries for all of the files/directories in this group
        //the key is the unmodified name of the entry, before it is modified to be made unique (if needed).
        private RadixTree<FileSystemEntry> groupEntries = new RadixTreeImpl<FileSystemEntry>();

        //whether the containing directory is case sensitive or not.
        //-1 = unset
        //0 = false;
        //1 = true;
        private int isCaseSensitive = -1;

        public VirtualGroupEntry(FileSystemEntry firstChild, File parent) {
            super(parent);

            //use the name of the first child in the group as-is
            groupEntries.insert(firstChild.file.getName(), firstChild);
        }

        @Override
        public File addUniqueChild(String[] pathElements, int pathElementsIndex) {
            String elementName = pathElements[pathElementsIndex];

            if (pathElementsIndex == pathElements.length - 1) {
                elementName = elementName + fileExtension;
            }

            FileSystemEntry existingEntry = groupEntries.find(elementName);
            if (existingEntry != null) {
                if (pathElementsIndex == pathElements.length - 1) {
                    return existingEntry.file;
                } else {
                    return existingEntry.addUniqueChild(pathElements, pathElementsIndex+1);
                }
            }

            if (pathElementsIndex == pathElements.length - 1) {
                String fileName;
                if (!isCaseSensitive()) {
                    fileName = pathElements[pathElementsIndex] + "." + (groupEntries.getSize()+1) + fileExtension;
                } else {
                    fileName = elementName;
                }

                ClassNameEntry classNameEntry = new ClassNameEntry(file, fileName);
                groupEntries.insert(elementName, classNameEntry);
                return classNameEntry.file;
            } else {
                String fileName;
                if (!isCaseSensitive()) {
                    fileName = pathElements[pathElementsIndex] + "." + (groupEntries.getSize()+1);
                } else {
                    fileName = elementName;
                }

                PackageNameEntry packageNameEntry = new PackageNameEntry(file, fileName);
                groupEntries.insert(elementName, packageNameEntry);
                return packageNameEntry.addUniqueChild(pathElements, pathElementsIndex + 1);
            }
        }

        private boolean isCaseSensitive() {
            if (isCaseSensitive != -1) {
                return isCaseSensitive == 1;
            }

            File path = file;

            if (path.exists() && path.isFile()) {
                path = path.getParentFile();
            }

            if ((!file.exists() && !file.mkdirs())) {
                return false;
            }

            try {
                boolean result = testCaseSensitivity(path);
                isCaseSensitive = result?1:0;
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

        @Override
        public FileSystemEntry makeVirtual(File parent) {
            return this;
        }
    }

    private class ClassNameEntry extends FileSystemEntry {
        public ClassNameEntry(File parent, String name) {
            super(new File(parent, name));
        }

        @Override
        public File addUniqueChild(String[] pathElements, int pathElementsIndex) {
            assert false;
            return file;
        }
    }
}
